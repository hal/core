package org.jboss.as.console.client.rbac;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.web.bindery.event.shared.EventBus;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.plugins.AccessControlRegistry;
import org.jboss.as.console.client.widgets.progress.ProgressElement;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.model.mapping.AddressMapping;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.rbac.SecurityContextAware;
import org.jboss.ballroom.client.rbac.SecurityContextChangedEvent;
import org.jboss.ballroom.client.rbac.SecurityContextChangedHandler;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.FilteringStatementContext;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * The security manager creates and provides a {@link SecurityContext} per place
 *
 * @see com.gwtplatform.mvp.client.proxy.PlaceManager
 *
 * @author Heiko Braun
 */
public class SecurityFrameworkImpl implements SecurityFramework, SecurityContextChangedHandler {


    private static final String MODEL_DESCRIPTION = "model-description";
    private static final String DEFAULT = "default";
    private static final String ATTRIBUTES = "attributes";
    private static final String READ = "read";
    private static final String WRITE = "write";
    private static final String ADDRESS = "address";
    private static final String EXECUTE = "execute";
    private static final String EXCEPTIONS = "exceptions";
    private static final String ACCESS_CONTROL = "access-control";
    private static final String TRIM_DESCRIPTIONS = "trim-descriptions";
    private static final String COMBINED_DESCRIPTIONS = "combined-descriptions";

    protected final AccessControlRegistry accessControlMetaData;
    protected final DispatchAsync dispatcher;
    protected final CoreGUIContext statementContext;
    protected final CoreGUIContext coreGUIContext;
    protected final ContextKeyResolver keyResolver;

    private final FilteringStatementContext filteringStatementContext;
    private final Map<String, SecurityContextAware> contextAwareWidgets;

    protected Map<String, SecurityContext> contextMapping = new HashMap<String, SecurityContext>();

    private final static SecurityContext READ_ONLY  = new ReadOnlyContext();

    @Inject
    public SecurityFrameworkImpl(AccessControlRegistry accessControlMetaData, DispatchAsync dispatcher,
                                 CoreGUIContext statementContext, final BootstrapContext bootstrap, EventBus eventBus,
                                 CoreGUIContext coreGUIContext) {

        this.accessControlMetaData = accessControlMetaData;
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
        this.coreGUIContext = coreGUIContext;
        this.keyResolver = new PlaceSecurityResolver();
        this.contextAwareWidgets = new HashMap<String, SecurityContextAware>();
        this.filteringStatementContext = new FilteringStatementContext(
                statementContext,
                new FilteringStatementContext.Filter() {
                    @Override
                    public String filter(String key) {
                        if (key.equals("selected.entity")) {
                            return "*";
                        } else if (key.equals("addressable.group")) {
                            return bootstrap.getAddressableGroups().isEmpty() ? "*" : bootstrap
                                    .getAddressableGroups().iterator().next();
                        } else if (key.equals("addressable.host")) {
                            return bootstrap.getAddressableHosts().isEmpty() ? "*" : bootstrap.getAddressableHosts()
                                    .iterator().next();
                        } else {
                            return null;
                        }
                    }

                    @Override
                    public String[] filterTuple(String key) {
                        switch (key) {
                            case "selected.entity":
                                return new String[] {"*"};

                            case "addressable.group":
                                return bootstrap.getAddressableGroups().isEmpty()
                                        ? new String[] {"*"}
                                        : (String[]) bootstrap.getAddressableGroups().toArray();

                            case "addressable.host":
                                return bootstrap.getAddressableGroups().isEmpty()
                                        ? new String[] {"*"}
                                        : (String[]) bootstrap.getAddressableHosts().toArray();

                            default:
                                return null;
                        }
                    }
                }
        );

        SecurityContextChangedEvent.register(eventBus, this);
    }

    @Override
    public SecurityContext getSecurityContext() {
        return getSecurityContext(keyResolver.resolveKey());
    }

    @Override
    public boolean hasContext(String id) {
        return contextMapping.containsKey(id);
    }

    @Override
    public SecurityContext getSecurityContext(String id) {
        return contextMapping.get(id);
    }

    @Override
    public void registerWidget(final String id, final SecurityContextAware widget) {
        contextAwareWidgets.put(id, widget);
    }

    @Override
    public void unregisterWidget(final String id) {
        contextAwareWidgets.remove(id);
    }

    @Override
    public void onSecurityContextChanged(final SecurityContextChangedEvent event) {
        SecurityContext context = event.getSecurityContext();
        String addressTemplate = event.getResourceAddress();
        // System.out.println("<SCC>");

        if (context == null) {
            // address resolution
            ModelNode addressNode = AddressMapping.fromString(addressTemplate).asResource(coreGUIContext,
                    event.getWildcards());
            String resourceAddress = normalize(addressNode.get(ADDRESS));
            // System.out.println(
            //         "\tReceiving security context change event for " + addressTemplate + " -> " + resourceAddress);

            // look for child context
            context = getSecurityContext();
            if (context.hasChildContext(resourceAddress)) {
                System.out.println("\tFound child context for " + resourceAddress);
                context = context.getChildContext(resourceAddress);
            }
        }/* else {
            System.out.println("\tReceiving security context change event for " + context);
        }*/

        // update widgets (if visible and filter applies)
        for (Map.Entry<String, SecurityContextAware> entry : contextAwareWidgets.entrySet()) {
            String id = entry.getKey();
            SecurityContextAware widget = entry.getValue();

            boolean update = true;
            if (widget.getFilter() != null) {
                update = widget.getFilter().equals(addressTemplate);
            }
            if (update && widget.isAttached()) {
                //System.out.println("\tUpdating widget " + id);
                widget.updateSecurityContext(context);
            }
        }
        //System.out.println("</SCC>\n");
    }

    public void createSecurityContext(final String id, final AsyncCallback<SecurityContext> callback) {
         createSecurityContext(id, accessControlMetaData.getResources(id),  accessControlMetaData.isRecursive(id), callback);
    }

    public void createSecurityContext(final String id, final Set<String> requiredResources, boolean recursive, final AsyncCallback<SecurityContext> callback) {

        // @NoGatekeeper (and thus no mapped resources ...)
        if(requiredResources.isEmpty())
        {
            NoGatekeeperContext noop = new NoGatekeeperContext();
            contextMapping.put(id, noop);
            callback.onSuccess(noop);
        }

        // @RBACGatekeeper & @AccessControl
        else
        {
            try {
                loadSecurityMetadata(id, requiredResources, recursive, callback);
            } catch (Throwable t) {
                callback.onFailure(t);
            }
        }

    }

    private void loadSecurityMetadata(final String id, final Set<String> requiredResources, boolean recursive, final AsyncCallback<SecurityContext> callback) {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(COMPOSITE);
        operation.get(ADDRESS).setEmptyList();

        final List<ModelNode> steps = new LinkedList<ModelNode>();


        final Map<String, ResourceRef> step2address = new HashMap<String,ResourceRef>();

        // normalisation

        final Set<ResourceRef> references = new HashSet<ResourceRef>(requiredResources.size());
        for(String address : requiredResources)
            references.add(new ResourceRef(address));


        for(ResourceRef ref : references)
        {
            ModelNode emptyAddress = new ModelNode().setEmptyList();
            ModelNode step = AddressMapping.fromString(ref.address).asResource(emptyAddress, filteringStatementContext);

            step2address.put("step-" + (steps.size() + 1), ref);   // we need this for later retrieval

            step.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
            //step.get(RECURSIVE).set(true);

            if(recursive)
                step.get("recursive-depth").set(2); // Workaround for Beta2 : some browsers choke on two big payload size

            step.get(ACCESS_CONTROL).set(TRIM_DESCRIPTIONS); // reduces the payload size
            step.get(INCLUDE_ALIASES).set("true"); // TODO Test if this is still necessary once WFLY-2738 is fixed
            step.get(OPERATIONS).set(true);
            steps.add(step);

        }

        operation.get(STEPS).set(steps);

        //System.out.println("Request:" + operation);

        final long s0 = System.currentTimeMillis();

        // TOD: provide proper API
        final ProgressElement progressElement = Footer.PROGRESS_ELEMENT;
        progressElement.reset();
        progressElement.tick();

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onFailure(Throwable caught) {

                //callback.onFailure(new RuntimeException("Failed to create security context for "+id, caught));
                progressElement.finish();
                Log.error("Failed to create security context for "+id+ ", fallback to temporary read-only context", caught.getMessage());
                contextMapping.put(id, READ_ONLY);
                callback.onSuccess(READ_ONLY);
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {


                Log.info("Context http (" + id + "): " + (System.currentTimeMillis() - s0) + "ms");

                long s1 = System.currentTimeMillis();
                ModelNode response = dmrResponse.get();
                Log.info("Context decode (" + id + "): " + (System.currentTimeMillis() - s1) + "ms");


                final long s2 = System.currentTimeMillis();

                if(response.isFailure())
                {
                    Log.error(
                            "Failed to retrieve access control meta data, " +
                                    "fallback to temporary read-only context: ",
                            response.getFailureDescription());

                    contextMapping.put(id, READ_ONLY);
                    callback.onSuccess(READ_ONLY);

                    return;
                }

                try {

                    ModelNode overalResult = response.get(RESULT);

                    SecurityContextImpl context = new SecurityContextImpl(id, references);

                    // The first part is identify the resource that has been requested.
                    // Depending on whether you've requested a wildcard address or a specific one
                    // we either get a ModelType.List or ModelType.Object response.
                    // The former requires parsing the response to access control meta data matching the inquiry.

                    for(int i=1; i<=steps.size();i++)
                    {
                        String step = "step-"+i;
                        if(overalResult.hasDefined(step))
                        {

                            // break down the address into something we can match against the response
                            final ResourceRef ref = step2address.get(step);
                            ModelNode emptyAddress = new ModelNode().setEmptyList();
                            final ModelNode addressNode = AddressMapping.fromString(ref.address).asResource(emptyAddress, filteringStatementContext);
                            final List<ModelNode> inquiryAddress = addressNode.get(ADDRESS).asList();

                            ModelNode stepResult = overalResult.get(step).get(RESULT);

                            ModelNode payload = null;

                            // it's a List response when asking for '<resourceType>=*"
                            if(stepResult.getType() == ModelType.LIST)
                            {
                                List<ModelNode> nodes = stepResult.asList();

                                for(ModelNode node : nodes)
                                {
                                    // matching the wildcard response
                                    List<ModelNode> responseAddress = node.get(ADDRESS).asList();

                                    // match the inquiry
                                    if(matchingAddress(responseAddress, inquiryAddress))
                                    {
                                        payload = node;
                                        break;
                                    }
                                }

                                if(payload == null)
                                {
                                    //System.out.println(ref.address+" -> "+stepResult);
                                    throw new RuntimeException("Unexpected response format");
                                }

                            }
                            else
                            {
                                payload = stepResult;
                            }

                            // break down into root resource and children
                            parseAccessControlChildren(ref, references, context, payload);
                        }
                    }

                    context.seal(); // makes it immutable

                    contextMapping.put(id, context);

                    Log.info("Context parse (" + id + "): " + (System.currentTimeMillis() - s2) + "ms");

                    progressElement.finish();
                    callback.onSuccess(context);

                } catch (Throwable e) {
                    progressElement.finish();
                    e.printStackTrace();
                    callback.onFailure(new RuntimeException("Failed to parse access control meta data: "+ e.getMessage(), e));
                }

            }
        });
    }

    private static boolean matchingAddress(List<ModelNode> responseAddress, List<ModelNode> inquiryAdress) {

        int numMatchingTokens = 0;
        int offset = inquiryAdress.size()-responseAddress.size();

        for(int i=responseAddress.size()-1; i>=0; i--)
        {
            ModelNode token = responseAddress.get(i);
            if(inquiryAdress.get(i+offset).toString().equals(token.toString())) {
                numMatchingTokens++;
            } else {
                String rspTokString = responseAddress.get(i).asProperty().getValue().asString();
                Property reqTok = inquiryAdress.get(i + offset).asProperty();

                // If the response specificies * for a value, but the request specified multiple values
                // Then, the response is valid even though it is not an exact match.
                if (rspTokString.equals("*") &&
                        (reqTok.getValue().asString().equals("*") || reqTok.getValue().asList().size() > 1)) {
                    numMatchingTokens++;
                }
            }
        }
        return numMatchingTokens==responseAddress.size();
    }

    private void parseAccessControlChildren(final ResourceRef ref, Set<ResourceRef> references, SecurityContextImpl context, ModelNode payload) {

        ModelNode actualPayload = payload.hasDefined(RESULT) ? payload.get(RESULT) : payload;

        // parse the root resource itself
        parseAccessControlMetaData(ref, context, actualPayload);

        // parse the child resources
        if(actualPayload.hasDefined(CHILDREN))
        {
            ModelNode childNodes = actualPayload.get(CHILDREN);
            Set<String> children = childNodes.keys();
            for(String child : children)
            {
                ResourceRef childAddress = new ResourceRef(ref.address+"/"+child+"=*");
                if(!references.contains(childAddress)) // might be parsed already
                {
                    ModelNode childModel = childNodes.get(child);
                    if(childModel.hasDefined(MODEL_DESCRIPTION)) // depends on 'recursive' true/false
                    {
                        ModelNode childPayload = childModel.get(MODEL_DESCRIPTION).asPropertyList().get(0).getValue();
                        references.add(childAddress); /// dynamically update the list of required resources
                        parseAccessControlChildren(childAddress, references, context, childPayload);
                    }
                }
            }
        }
    }

    private static void parseAccessControlMetaData(final ResourceRef ref, SecurityContextImpl context,
                                                   ModelNode payload) {

        ModelNode accessControl = payload.get(ACCESS_CONTROL);
        if (accessControl.isDefined() && accessControl.hasDefined(DEFAULT)) {

            // default policy for requested resource type
            Constraints defaultConstraints = parseConstraints(ref, accessControl.get(DEFAULT));
            if (ref.optional) {
                context.setOptionalConstraints(ref.address, defaultConstraints);
            } else {
                context.setConstraints(ref.address, defaultConstraints);
            }

            // exceptions (instances) of requested resource type
            if (accessControl.hasDefined(EXCEPTIONS)) {
                for (Property exception : accessControl.get(EXCEPTIONS).asPropertyList()) {
                    ModelNode addressNode = exception.getValue().get(ADDRESS);
                    String address = normalize(addressNode);
                    if (address != null) {
                        ResourceRef exceptionRef = new ResourceRef(address);
                        Constraints instanceConstraints = parseConstraints(exceptionRef, exception.getValue());
                        context.addChildContext(address, instanceConstraints);
                    } else {
                        Log.error("Skip exception " + exception.getName() + ": No address found in " + exception
                                .getValue());
                    }
                }
            }
        }
    }

    private static String normalize(final ModelNode address) {
        if (address.isDefined()) {
            StringBuilder normalized = new StringBuilder();
            List<Property> properties = address.asPropertyList();
            for (Property property : properties) {
                normalized.append("/").append(property.getName()).append("=").append(property.getValue().asString());
            }
            return normalized.toString();
        }
        return null;
    }

    private static Constraints parseConstraints(final ResourceRef ref, ModelNode policyModel) {

        Constraints constraints = new Constraints(ref.address);

        // resource constraints
        if (policyModel.hasDefined(ADDRESS) && !policyModel.get(ADDRESS).asBoolean()) {
            constraints.setAddress(false);
        } else {
            constraints.setReadResource(policyModel.get(READ).asBoolean());
            constraints.setWriteResource(policyModel.get(WRITE).asBoolean());
        }

        // operation constraints
        if (policyModel.hasDefined(OPERATIONS)) {
            List<Property> operations = policyModel.get(OPERATIONS).asPropertyList();
            for (Property op : operations) {
                ModelNode opConstraintModel = op.getValue();
                constraints.setOperationExec(ref.address, op.getName(), opConstraintModel.get(EXECUTE).asBoolean());
            }
        }

        // attribute constraints
        if (policyModel.hasDefined(ATTRIBUTES)) {
            List<Property> attributes = policyModel.get(ATTRIBUTES).asPropertyList();

            for (Property att : attributes) {
                ModelNode attConstraintModel = att.getValue();
                constraints.setAttributeRead(att.getName(), attConstraintModel.get(READ).asBoolean());
                constraints.setAttributeWrite(att.getName(), attConstraintModel.get(WRITE).asBoolean());
            }
        }
        return constraints;
    }

    @Override
    public void flushContext(String nameToken) {
        contextMapping.remove(nameToken);
    }

    @Override
    public Set<String> getReadOnlyJavaNames(Class<?> type, SecurityContext securityContext) {

        // Fallback
        if(type == Object.class || type == null)
            return Collections.emptySet();

        return new MetaDataAdapter(Console.MODULES.getApplicationMetaData())
                .getReadOnlyJavaNames(type, securityContext);
    }

    @Override
    public Set<String> getReadOnlyJavaNames(Class<?> type, String resourceAddress, SecurityContext securityContext) {

        // Fallback
        if(type == Object.class || type == null)
            return Collections.emptySet();

        return new MetaDataAdapter(Console.MODULES.getApplicationMetaData())
                .getReadOnlyJavaNames(type, resourceAddress, securityContext);
    }

    @Override
    public Set<String> getFilteredJavaNames(Class<?> type, String resourceAddress, SecurityContext securityContext) {
        // Fallback
        if(type == Object.class || type == null)
            return Collections.emptySet();

        return new MetaDataAdapter(Console.MODULES.getApplicationMetaData())
                .getFilteredJavaNames(type, resourceAddress, securityContext);
    }

    @Override
    public Set<String> getFilteredJavaNames(Class<?> type, SecurityContext securityContext) {
        // Fallback
        if(type == Object.class || type == null)
            return Collections.emptySet();

        return new MetaDataAdapter(Console.MODULES.getApplicationMetaData())
                .getFilteredJavaNames(type,  securityContext);
    }

    @Override
    public Set<String> getReadOnlyDMRNames(String resourceAddress, List<String> formItemNames, SecurityContext securityContext) {

        // TODO: at some point this should refer to the actual resource address
        Set<String> readOnly = new HashSet<String>();
        for(String item : formItemNames)
        {
            if(!securityContext.getAttributeWritePriviledge(item).isGranted())
                readOnly.add(item);
        }
        return readOnly;
    }

    @Override
    public Set<String> getFilteredDMRNames(String resourceAddress, List<String> formItemNames, SecurityContext securityContext) {
       // TODO: at some point this should refer to the actual resource address
        Set<String> readOnly = new HashSet<String>();
        for(String item : formItemNames)
        {
            boolean writepriv = securityContext.getAttributeWritePriviledge(item).isGranted();
            boolean readpriv = securityContext.getAttributeReadPriviledge(item).isGranted();

            if(!writepriv && !readpriv)
                readOnly.add(item);
        }
        return readOnly;
    }
}
