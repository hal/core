package org.jboss.as.console.client.rbac;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.plugins.AccessControlRegistry;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.model.mapping.AddressMapping;
import org.jboss.ballroom.client.rbac.SecurityContext;
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
 * @date 7/3/13
 */
public class SecurityFrameworkImpl implements SecurityFramework {


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

    protected final AccessControlRegistry accessControlMetaData;
    protected final DispatchAsync dispatcher;
    protected final CoreGUIContext statementContext;
    protected final ContextKeyResolver keyResolver;
    private final BootstrapContext bootstrap;

    protected Map<String, SecurityContext> contextMapping = new HashMap<String, SecurityContext>();

    private final static SecurityContext READ_ONLY  = new ReadOnlyContext();

    @Inject
    public SecurityFrameworkImpl(
            AccessControlRegistry accessControlMetaData,
            DispatchAsync dispatcher,
            CoreGUIContext statementContext, BootstrapContext bootstrap) {
        this.accessControlMetaData = accessControlMetaData;
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
        this.keyResolver = new PlaceSecurityResolver();
        this.bootstrap = bootstrap;
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

        SecurityContext securityContext = contextMapping.get(id);
        return securityContext;
    }



    public void createSecurityContext(final String id, final AsyncCallback<SecurityContext> callback) {
        createSecurityContext(id, accessControlMetaData.getResources(id), callback);
    }

    public void createSecurityContext(final String id, final Set<String> requiredResources, final AsyncCallback<SecurityContext> callback) {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(COMPOSITE);
        operation.get(ADDRESS).setEmptyList();

        final List<ModelNode> steps = new LinkedList<ModelNode>();


        final Map<String, String> step2address = new HashMap<String,String>();

        for(String resource : requiredResources)
        {

            ModelNode step = AddressMapping.fromString(resource).asResource(
                    new FilteringStatementContext(
                            statementContext,
                            new FilteringStatementContext.Filter() {
                                @Override
                                public String filter(String key) {

                                    if("selected.entity".equals(key))
                                    {
                                        return "*";
                                    }
                                    else if("addressable.group".equals(key))
                                    {
                                        return bootstrap.getAddressableGroups().isEmpty() ? "*" : bootstrap.getAddressableGroups().iterator().next();
                                    }
                                    else if("addressable.host".equals(key))
                                    {
                                        return bootstrap.getAddressableHosts().isEmpty() ? "*" : bootstrap.getAddressableHosts().iterator().next();
                                    }
                                    else
                                    {
                                        return null;
                                    }
                                }

                                @Override
                                public String[] filterTuple(String key) {
                                    return null;
                                }
                            }
                    ) {

                    }
            );

            step2address.put("step-" + (steps.size() + 1), resource);   // we need this for later retrieval

            step.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
            //step.get(RECURSIVE).set(true);

            if(accessControlMetaData.isRecursive(id))
                step.get("recursive-depth").set(2); // Workaround for Beta2 : some browsers choke on two big payload size

            step.get(ACCESS_CONTROL).set(TRIM_DESCRIPTIONS); // reduces the payload size
            step.get(OPERATIONS).set(true);
            steps.add(step);

        }

        operation.get(STEPS).set(steps);

        //System.out.println("Request:" + operation);

        final long s0 = System.currentTimeMillis();

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onFailure(Throwable caught) {

                //callback.onFailure(new RuntimeException("Failed to create security context for "+id, caught));

                Console.warning("Failed to create security context for "+id+ ", fallback to temporary read-only context", caught.getMessage());
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
                    Console.warning(
                            "Failed to retrieve access control meta data, " +
                            "fallback to temporary read-only context: ",
                            response.getFailureDescription());

                    contextMapping.put(id, READ_ONLY);
                    callback.onSuccess(READ_ONLY);

                    return;
                }

                //System.out.println("Response: "+ response);

                try {

                    ModelNode overalResult = response.get(RESULT);

                    SecurityContextImpl context = new SecurityContextImpl(id, requiredResources);

                    // retrieve access constraints for each required resource and update the security context
                    for(int i=1; i<=steps.size();i++)
                    {
                        String step = "step-"+i;
                        if(overalResult.hasDefined(step))
                        {
                            String resourceAddress = step2address.get(step);
                            ModelNode stepResult = overalResult.get(step).get(RESULT);

                            ModelNode payload = null;
                            if(stepResult.getType() == ModelType.LIST)
                            {
                                List<ModelNode> nodes = stepResult.asList(); // TODO: Should be optimized
                                boolean instanceReference = !nodes.isEmpty();
                                for(ModelNode node : nodes)
                                {
                                    // matching the wildcard response
                                    List<ModelNode> tokens = node.get(ADDRESS).asList();
                                    if(instanceReference)
                                    {
                                        // chose an instance declaration
                                        if(!tokens.get(tokens.size()-1).asString().contains("*"))
                                        {
                                            payload = node;
                                            break;
                                        }

                                    }
                                    else
                                    {
                                        // chose the wildcard declaration
                                        if(tokens.get(tokens.size()-1).asString().contains("*"))
                                        {
                                            payload = node;
                                            break;
                                        }
                                    }
                                }

                                // TODO: Fallback needed?
                                if(payload == null)
                                    payload = nodes.get(0);

                            }
                            else
                            {
                                payload = stepResult;
                            }

                            // break down into root resource and children
                            parseAccessControlChildren(resourceAddress, requiredResources, context, payload);
                        }
                    }

                    context.seal(); // makes it immutable

                    contextMapping.put(id, context);

                    Log.info("Context parse (" + id + "): " + (System.currentTimeMillis() - s2) + "ms");

                    callback.onSuccess(context);

                } catch (Throwable e) {
                    callback.onFailure(new RuntimeException("Failed to parse access control meta data", e));
                }

            }
        });


    }

    private static void parseAccessControlChildren(final String resourceAddress, Set<String> requiredResources, SecurityContextImpl context, ModelNode payload) {

        ModelNode actualPayload = payload.hasDefined(RESULT) ? payload.get(RESULT) : payload;

        // parse the root resource itself
        parseAccessControlMetaData(resourceAddress, context, actualPayload);

        // parse the child resources
        if(actualPayload.hasDefined(CHILDREN))
        {
            //List<Property> children = actualPayload.get(CHILDREN).asPropertyList();
            ModelNode childNodes = actualPayload.get(CHILDREN);
            Set<String> children = childNodes.keys();
            for(String child : children)
            {
                String childAddress = resourceAddress+"/"+child+"=*";
                if(!requiredResources.contains(childAddress)) // might be parsed already
                {
                    ModelNode childModel = childNodes.get(child);
                    if(childModel.hasDefined(MODEL_DESCRIPTION)) // depends on 'recursive' true/false
                    {
                        ModelNode childPayload = childModel.get(MODEL_DESCRIPTION).asPropertyList().get(0).getValue();
                        requiredResources.add(childAddress); /// dynamically update the list of required resources
                        parseAccessControlChildren(childAddress, requiredResources, context, childPayload);
                    }
                }
            }
        }
    }

    private static void parseAccessControlMetaData(final String resourceAddress, SecurityContextImpl context, ModelNode payload) {

        ModelNode accessControl = payload.get(ACCESS_CONTROL);

        if(accessControl.isDefined() && accessControl.hasDefined(DEFAULT))
        {

            // identify the target node, in some cases exceptions override the dfefault behaviour
            ModelNode model = null;
            ModelNode exceptionModel = accessControl.get(EXCEPTIONS);
            if(exceptionModel.keys().size()>0)  // TODO: fix the actual representation, should not be ModelType.Object
            {
                List<Property> exceptions = exceptionModel.asPropertyList();
                model = exceptions.get(0).getValue();
            }
            else
            {
                model = accessControl.get(DEFAULT);
            }

            Constraints c = new Constraints(resourceAddress);

            if(model.hasDefined(ADDRESS)
                    && model.get(ADDRESS).asBoolean()==false)
            {
                c.setAddress(false);
            }
            else
            {

                c.setReadResource(model.get(READ).asBoolean());
                c.setWriteResource(model.get(WRITE).asBoolean());
            }

            // operation constraints
            if(model.hasDefined(OPERATIONS))
            {
                List<Property> operations = model.get(OPERATIONS).asPropertyList();
                for(Property op : operations)
                {
                    ModelNode opConstraintModel = op.getValue();
                    c.setOperationExec(resourceAddress, op.getName(), opConstraintModel.get(EXECUTE).asBoolean());
                }

            }

            // attribute constraints

            if(model.hasDefined(ATTRIBUTES))
            {
                List<Property> attributes = model.get(ATTRIBUTES).asPropertyList();

                for(Property att : attributes)
                {
                    ModelNode attConstraintModel = att.getValue();
                    c.setAttributeRead(att.getName(), attConstraintModel.get(READ).asBoolean());
                    c.setAttributeWrite(att.getName(), attConstraintModel.get(WRITE).asBoolean());

                }
            }

            context.updateResourceConstraints(resourceAddress, c);
        }
        else
        {
            Console.warning("Access-control meta data missing for "+ resourceAddress);
        }
    }

    @Override
    public void flushContext(String nameToken) {
        contextMapping.remove(nameToken);
    }

    @Override
    public Set<String> getReadOnlyJavaNames(Class<?> type, SecurityContext securityContext) {

        // Fallback
        if(type == Object.class || type == null)
            return Collections.EMPTY_SET;

        return new MetaDataAdapter(Console.MODULES.getApplicationMetaData())
                .getReadOnlyJavaNames(type, securityContext);
    }

    @Override
    public Set<String> getReadOnlyJavaNames(Class<?> type, String resourceAddress, SecurityContext securityContext) {

        // Fallback
        if(type == Object.class || type == null)
            return Collections.EMPTY_SET;

        return new MetaDataAdapter(Console.MODULES.getApplicationMetaData())
                .getReadOnlyJavaNames(type, resourceAddress, securityContext);
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

}


