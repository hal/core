package org.jboss.as.console.client.rbac;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.plugins.AccessControlRegistry;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.model.mapping.AddressMapping;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
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
            throw new IllegalStateException("Security should have been created upfront");

        return securityContext;
    }

    public void createSecurityContext(final String nameToken, final AsyncCallback<SecurityContext> callback) {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(COMPOSITE);
        operation.get(ADDRESS).setEmptyList();

        final List<ModelNode> steps = new LinkedList<ModelNode>();

        final Set<String> resources = accessControlReg.getResources(nameToken);

        for(String resource : resources)
        {

            ModelNode step = AddressMapping.fromString(resource).asResource(statementContext);
            step.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
            step.get("include-access").set(true);
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

                boolean accessGranted = true;

                try {
                    for(int i=1; i<=steps.size();i++)
                    {
                        String step = "step-"+i;
                        if(overalResult.hasDefined(step))
                        {
                            ModelNode modelNode = overalResult.get(step).get(RESULT);

                            // TODO: why is it sometimes a list ?

                            ModelNode stepResult = null;
                            if(modelNode.getType() == ModelType.LIST)
                                stepResult = modelNode.asList().get(0);
                            else
                                stepResult = modelNode;

                            ModelNode accessControl = stepResult.hasDefined("access-control") ?
                                    stepResult.get("access-control") : stepResult.get(RESULT).get("access-control");

                            List<Property> properties = accessControl.asPropertyList();
                            Property acl = properties.get(0);
                            String address = acl.getName();
                            ModelNode model = acl.getValue();

                            if(!model.get("read-config").asBoolean())
                            {
                                accessGranted = false;
                                break; // all or nothing
                            }
                        }
                    }
                } catch (Throwable e) {
                    Log.error("Failed to parse response", e);
                    callback.onFailure(new RuntimeException("Failed to parse response",e));
                }

                SecurityContext context = new SecurityContext(
                        nameToken, resources
                );

                context.setGrantPlaceAccess(accessGranted);

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
