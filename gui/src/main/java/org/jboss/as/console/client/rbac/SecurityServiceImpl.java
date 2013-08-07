package org.jboss.as.console.client.rbac;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.plugins.AccessControlRegistry;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.model.mapping.AddressMapping;
import org.jboss.ballroom.client.rbac.Constraints;
import org.jboss.ballroom.client.rbac.Facet;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.rbac.SecurityService;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * The secuirty service creates and provides a {@link SecurityContext} per place
 *
 * @see com.gwtplatform.mvp.client.proxy.PlaceManager
 *
 * @author Heiko Braun
 * @date 7/3/13
 */
public class SecurityServiceImpl implements SecurityService {


    private final AccessControlRegistry accessControlReg;
    private final DispatchAsync dispatcher;
    private final CoreGUIContext statementContext;

    private Map<String, SecurityContext> contextMapping = new HashMap<String, SecurityContext>();

    @Inject
    public SecurityServiceImpl(AccessControlRegistry accessControlReg, DispatchAsync dispatcher, CoreGUIContext statementContext) {
        this.accessControlReg = accessControlReg;
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
    }

    @Override
    public boolean hasContext(String nameToken) {
        return contextMapping.containsKey(nameToken);
    }

    @Override
    public SecurityContext getSecurityContext(String nameToken) {

        SecurityContext securityContext = contextMapping.get(nameToken);
        if(null==securityContext)
            throw new IllegalStateException("Security context should have been created upfront");

        return securityContext;
    }

    public void createSecurityContext(final String nameToken, final AsyncCallback<SecurityContext> callback) {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(COMPOSITE);
        operation.get(ADDRESS).setEmptyList();

        final List<ModelNode> steps = new LinkedList<ModelNode>();

        final Set<String> requiredResources = accessControlReg.getResources(nameToken);
        final Map<String, String> step2address = new HashMap<String,String>();

        for(String resource : requiredResources)
        {

            ModelNode step = AddressMapping.fromString(resource).asResource(statementContext);
            step2address.put("step-" + (steps.size() + 1), resource);   // we need this for later retrieval

            step.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
            step.get(RECURSIVE).set(true); // TODO: Does this blow the payload size?
            step.get("access-control").set(true);
            steps.add(step);

        }

        operation.get(STEPS).set(steps);

        //System.out.println(operation);

        final long start = System.currentTimeMillis();

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onFailure(Throwable caught) {

                callback.onFailure(new RuntimeException("Failed to create security context for "+nameToken, caught));
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {


                ModelNode response = dmrResponse.get();
                ModelNode overalResult = response.get(RESULT);


                SecurityContext context = new SecurityContext(nameToken, requiredResources);
                context.setFacet(Facet.valueOf(accessControlReg.getFacet(nameToken).toUpperCase()));

                try {

                    // retrieve access constraints for each required resource and update the security context
                    for(int i=1; i<=steps.size();i++)
                    {
                        String step = "step-"+i;
                        if(overalResult.hasDefined(step))
                        {
                            String resourceAddress = step2address.get(step);
                            ModelNode modelNode = overalResult.get(step).get(RESULT);

                            ModelNode payload = null;
                            if(modelNode.getType() == ModelType.LIST)
                                payload = modelNode.asList().get(0);
                            else
                                payload = modelNode;

                            // break down into root resource and children
                            parseAccessControlChildren(resourceAddress, requiredResources, context, payload);
                        }
                    }

                    context.seal(); // makes it immutable

                    contextMapping.put(nameToken, context);

                    System.out.println("Context creation time ("+nameToken+"): "+(System.currentTimeMillis()-start) +"ms");

                    callback.onSuccess(context);

                } catch (Throwable e) {
                    Log.error("Failed to parse access control meta data", e);
                    callback.onFailure(new RuntimeException("Failed to parse access control meta data", e));
                }

            }
        });


    }

    private void parseAccessControlChildren(final String resourceAddress, Set<String> requiredResources, SecurityContext context, ModelNode payload) {

        ModelNode actualPayload = payload.hasDefined(RESULT) ? payload.get(RESULT) : payload;

        // parse the root resource itself
        parseAccessControlMetaData(resourceAddress, context, actualPayload);

        // parse the child resources
        if(actualPayload.hasDefined(CHILDREN))
        {
            List<Property> children = actualPayload.get(CHILDREN).asPropertyList();
            for(Property child : children)
            {
                String childAddress = resourceAddress+"/"+child.getName()+"=*";
                if(!requiredResources.contains(childAddress)) // might be parsed already
                {
                    requiredResources.add(childAddress); /// dynamically update the list of required resources
                    ModelNode childModel = child.getValue();
                    ModelNode childPayload = childModel.get("model-description").asPropertyList().get(0).getValue();
                    parseAccessControlChildren(childAddress, requiredResources, context, childPayload);
                }
            }
        }
    }

    private void parseAccessControlMetaData(final String resourceAddress, SecurityContext context, ModelNode payload) {
        //ModelNode accessControl = payload.hasDefined(RESULT) ?
                //payload.get(RESULT).get("access-control") : payload.get("access-control");

        ModelNode accessControl = payload.get("access-control");

        List<Property> properties = accessControl.isDefined() ?
                accessControl.asPropertyList() : Collections.EMPTY_LIST;

        if(!properties.isEmpty())
        {
            Property acl = properties.get(0);
            assert acl.getName().equals("default");   //TODO: overrides ...
            ModelNode model = acl.getValue();

            Constraints c = new Constraints();

            if(model.hasDefined("address")
                    && model.get("address").asBoolean()==false)
            {
                c.setAddress(false);
            }
            else
            {

                c.setReadConfig(model.get("read-config").asBoolean());
                c.setWriteConfig(model.get("write-config").asBoolean());
                c.setReadRuntime(model.get("read-runtime").asBoolean());
                c.setWriteRuntime(model.get("write-runtime").asBoolean());

            }

            // attribute constraints

            if(model.hasDefined("attributes"))
            {
                List<Property> attributes = model.get("attributes").asPropertyList();

                for(Property att : attributes)
                {
                    ModelNode attConstraintModel = att.getValue();
                    // config access
                    if(attConstraintModel.hasDefined("read-config"))
                    {
                        c.setAttributeRead(att.getName(), attConstraintModel.get("read-config").asBoolean());
                        c.setAttributeWrite(att.getName(), attConstraintModel.get("write-config").asBoolean());
                    }

                    // runtime access
                    else
                    {
                        c.setAttributeRead(att.getName(), attConstraintModel.get("read-runtime").asBoolean());
                        c.setAttributeWrite(att.getName(), attConstraintModel.get("write-runtime").asBoolean());
                    }
                }
            }

            context.updateResourceConstraints(resourceAddress, c);
        }
    }

    @Override
    public void flushContext(String nameToken) {
        contextMapping.remove(nameToken);
    }
}
