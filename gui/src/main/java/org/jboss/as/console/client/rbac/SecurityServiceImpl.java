package org.jboss.as.console.client.rbac;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
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

        final Set<String> resources = accessControlReg.getResources(nameToken);
        final Map<String, String> step2address = new HashMap<String,String>();

        for(String resource : resources)
        {

            ModelNode step = AddressMapping.fromString(resource).asResource(statementContext);
            step2address.put("step-" + (steps.size() + 1), resource);   // we need this for later retrieval

            step.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
            step.get("access-control").set(true);
            //step.get("access-control").set(true);
            steps.add(step);

        }

        operation.get(STEPS).set(steps);

        System.out.println(operation);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onFailure(Throwable caught) {

                callback.onFailure(new RuntimeException("Failed to create security context for "+nameToken, caught));
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {


                ModelNode response = dmrResponse.get();
                ModelNode overalResult = response.get(RESULT);

                SecurityContext context = new SecurityContext(nameToken, resources);
                context.setFacet(Facet.valueOf(accessControlReg.getFacet(nameToken).toUpperCase()));

                try {

                    // retrieve access constraints per required resource and update the security context
                    for(int i=1; i<=steps.size();i++)
                    {
                        String step = "step-"+i;
                        if(overalResult.hasDefined(step))
                        {
                            ModelNode modelNode = overalResult.get(step).get(RESULT);

                            ModelNode stepResult = null;
                            if(modelNode.getType() == ModelType.LIST)
                                stepResult = modelNode.asList().get(0);
                            else
                                stepResult = modelNode;

                            ModelNode accessControl = stepResult.hasDefined(RESULT) ?
                                    stepResult.get(RESULT).get("access-control") : stepResult.get("access-control");

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

                                // TODO: attribute constraints

                                context.updateResourceConstraints(step2address.get(step), c);
                            }
                        }
                    }
                } catch (Throwable e) {
                    Log.error("Failed to parse response", e);
                    callback.onFailure(new RuntimeException("Failed to parse response", e));
                }

                context.seal(); // makes it immutable

                contextMapping.put(nameToken, context);

                callback.onSuccess(context);

            }
        });


    }

    @Override
    public void flushContext(String nameToken) {
        contextMapping.remove(nameToken);
    }
}
