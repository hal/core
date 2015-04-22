package org.jboss.as.console.client.v3.stores.domain;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.general.model.SocketBinding;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshSocketBindings;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.dmr.client.ModelDescriptionConstants;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.ChangeSupport;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Heiko Braun
 * @since 21/04/15
 */
@Store
public class SocketBindingStore extends ChangeSupport {


    private final DispatchAsync dispatcher;
    private final BeanFactory factory;
    private final ApplicationMetaData propertyMetaData;
    private final EntityAdapter<SocketBinding> entityAdapter;
    private Map<String, List<SocketBinding>> bindings = new HashMap<>();

    @Inject
    public SocketBindingStore(DispatchAsync dispatcher, BeanFactory factory,
                ApplicationMetaData propertyMetaData) {

        this.dispatcher = dispatcher;
        this.factory = factory;
        this.propertyMetaData = propertyMetaData;
        this.entityAdapter = new EntityAdapter<SocketBinding>(SocketBinding.class, propertyMetaData);
    }

    @Process(actionType = RefreshSocketBindings.class)
    public void onRefreshBindings(final RefreshSocketBindings action, final Dispatcher.Channel channel) {

        refresh(channel);
    }

    private void refresh(final Dispatcher.Channel channel) {

        this.bindings.clear();

        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        op.get(ModelDescriptionConstants.ADDRESS).add("socket-binding-group", "*");
        op.get(ModelDescriptionConstants.RECURSIVE).set(true);

        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                Console.error("Failed to load socket bindings: "+ caught.getMessage());
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if(response.isFailure()) {
                    Console.error("Failed to load socket bindings: "+ response.getFailureDescription());
                    channel.ack();
                }
                else
                {
                    List<ModelNode> groups = response.get("result").asList();
                    for (ModelNode group : groups) {
                        List<Property> tokens = group.get(ModelDescriptionConstants.ADDRESS).asPropertyList();
                        String groupName = tokens.get(tokens.size()-1).getValue().asString();
                        String defaultInterface = group.get("default-interface").asString();

                        List<Property> bindings = group.get(ModelDescriptionConstants.RESULT).get("socket-binding").asPropertyList();
                        for (Property binding : bindings) {

                            SocketBinding socketBinding = entityAdapter.fromDMR(binding.getValue());
                            socketBinding.setGroup(groupName);
                            socketBinding.setDefaultInterface(
                                    socketBinding.getInterface() != null ?
                                            socketBinding.getInterface() : defaultInterface
                            );
                            socketBinding.setDefaultInterface(defaultInterface);

                            if(null==SocketBindingStore.this.bindings.get(groupName))
                            {
                                SocketBindingStore.this.bindings.put(groupName, new ArrayList<SocketBinding>());
                            }

                            SocketBindingStore.this.bindings.get(groupName).add(socketBinding);

                        }
                    }
                    channel.ack();
                }
            }
        });

    }

    public Set<String> getGroupNames() {
        return bindings.keySet();
    }

    public List<SocketBinding> getSocketsForGroup(String name) {
        return bindings.get(name);
    }

}
