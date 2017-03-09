package org.jboss.as.console.client.shared.subsys.activemq.cluster;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.activemq.CommonMsgPresenter;
import org.jboss.as.console.client.shared.subsys.activemq.LoadActivemqServersCmd;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqBroadcastGroup;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqClusterConnection;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqDiscoveryGroup;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.behaviour.ModelNodeAdapter;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.widgets.AddResourceDialog;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.FilteringStatementContext;
import org.useware.kernel.gui.behaviour.StatementContext;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 4/18/12
 */
public class MsgClusteringPresenter
        extends Presenter<MsgClusteringPresenter.MyView, MsgClusteringPresenter.MyProxy>
        implements CommonMsgPresenter {

    // ------------------------------------------------------ proxy & view
    // @formatter:off
    @ProxyCodeSplit
    @NameToken(NameTokens.ActivemqMsgClusteringPresenter)
    @RequiredResources(resources = {
            "{selected.profile}/subsystem=messaging-activemq/server=*",
            "{selected.profile}/subsystem=messaging-activemq/server={activemq.server}/broadcast-group=*",
            "{selected.profile}/subsystem=messaging-activemq/server={activemq.server}/discovery-group=*",
            "{selected.profile}/subsystem=messaging-activemq/server={activemq.server}/cluster-connection=*"
    }
    )
    public interface MyProxy extends Proxy<MsgClusteringPresenter>, Place {}
    public interface MyView extends View {
        void setPresenter(MsgClusteringPresenter presenter);
        void setProvider(List<Property> result);
        void setSelectedProvider(String currentServer);
        void setBroadcastGroups(List<Property> groups);
        void setDiscoveryGroups(List<Property> groups);
        void setClusterConnection(List<Property> groups);
    }
    public static final AddressTemplate BROADCASTGROUP_ADDRESS = AddressTemplate
            .of("{selected.profile}/subsystem=messaging-activemq/server={activemq.server}/broadcast-group=*");
    public static final AddressTemplate DISCOVERYGROUP_ADDRESS = AddressTemplate
            .of("{selected.profile}/subsystem=messaging-activemq/server={activemq.server}/discovery-group=*");
    public static final AddressTemplate CLUSTERCONNECTION_ADDRESS = AddressTemplate
            .of("{selected.profile}/subsystem=messaging-activemq/server={activemq.server}/cluster-connection=*");
    private final CrudOperationDelegate operationDelegate;
    private final PlaceManager placeManager;
    // @formatter:on
    private final SecurityFramework securityFramework;
    private final ResourceDescriptionRegistry descriptionRegistry;
    private final StatementContext statementContext;
    private DispatchAsync dispatcher;
    private DefaultWindow window = null;
    private RevealStrategy revealStrategy;
    private String currentServer = null;
    CrudOperationDelegate.Callback defaultOpCallbacks = new CrudOperationDelegate.Callback() {
        @Override
        public void onSuccess(AddressTemplate address, String name) {
            Console.info(
                    Console.MESSAGES.successfullyModifiedResource(address.resolve(statementContext, name).toString()));
            onReset();
        }

        @Override
        public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {
            Console.error(Console.MESSAGES.failedToModifyResource(addressTemplate.toString()), t.getMessage());
        }
    };
    private EntityAdapter<ActivemqBroadcastGroup> bcastGroupAdapter;
    private EntityAdapter<ActivemqDiscoveryGroup> discGroupAdapter;
    private EntityAdapter<ActivemqClusterConnection> clusterConnectionsAdapter;

    @Inject
    public MsgClusteringPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            PlaceManager placeManager, DispatchAsync dispatcher,
            RevealStrategy revealStrategy, ApplicationMetaData propertyMetaData,
            SecurityFramework securityFramework, ResourceDescriptionRegistry descriptionRegistry,
            CoreGUIContext coreGUIContext) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;
        this.securityFramework = securityFramework;
        this.descriptionRegistry = descriptionRegistry;

        bcastGroupAdapter = new EntityAdapter<>(ActivemqBroadcastGroup.class, propertyMetaData);
        discGroupAdapter = new EntityAdapter<>(ActivemqDiscoveryGroup.class, propertyMetaData);
        clusterConnectionsAdapter = new EntityAdapter<>(ActivemqClusterConnection.class, propertyMetaData);

        this.statementContext = new FilteringStatementContext(coreGUIContext, new FilteringStatementContext.Filter() {
            @Override
            public String filter(String key) {
                if ("activemq.server".equals(key)) {
                    return currentServer;
                } else {
                    return null;
                }
            }

            @Override
            public String[] filterTuple(String key) {
                return coreGUIContext.resolveTuple(key);
            }
        });

        this.operationDelegate = new CrudOperationDelegate(this.statementContext, dispatcher);
    }

    public StatementContext getStatementContext() {
        return statementContext;
    }

    @Override
    public PlaceManager getPlaceManager() {
        return placeManager;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    private void loadProvider() {
        new LoadActivemqServersCmd(dispatcher, statementContext).execute(
                new AsyncCallback<List<Property>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Console.error("Failed to load messaging server names", caught.getMessage());
                    }

                    @Override
                    public void onSuccess(List<Property> result) {
                        getView().setProvider(result);
                        getView().setSelectedProvider(currentServer);
                    }
                }
        );
    }

    @Override
    protected void onReset() {
        super.onReset();
        loadProvider();
    }

    @Override
    public void prepareFromRequest(PlaceRequest request) {
        super.prepareFromRequest(request);
        currentServer = request.getParameter("name", null);
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    public void loadDetails() {
        loadBroadcastGroups();
        loadDiscoveryGroups();
        loadClusterConnections();
    }

    private void loadBroadcastGroups() {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", getCurrentServer());
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(CHILD_TYPE).set("broadcast-group");
        operation.get(RECURSIVE).set(true);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failed("Loading broadcast groups " + getCurrentServer()),
                            response.getFailureDescription());
                } else {
                    List<Property> model = response.get(RESULT).asPropertyList();
                    getView().setBroadcastGroups(model);
                }
            }
        });
    }

    private void loadDiscoveryGroups() {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", getCurrentServer());
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(CHILD_TYPE).set("discovery-group");
        operation.get(RECURSIVE).set(true);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failed("Loading discovery groups " + getCurrentServer()),
                            response.getFailureDescription());
                } else {
                    List<Property> model = response.get(RESULT).asPropertyList();
                    getView().setDiscoveryGroups(model);
                }
            }
        });
    }

    private void loadClusterConnections() {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", getCurrentServer());
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(CHILD_TYPE).set("cluster-connection");
        operation.get(RECURSIVE).set(true);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failed("Loading cluster connections " + getCurrentServer()),
                            response.getFailureDescription());
                } else {
                    List<Property> model = response.get(RESULT).asPropertyList();
                    getView().setClusterConnection(model);
                }
            }
        });
    }

    public String getCurrentServer() {
        return currentServer;
    }

    public void closeDialogue() {
        window.hide();
    }

    @Override
    public void onAddResource(ResourceAddress address, final ModelNode payload) {
        // ignore address, currently one use supported only
        window.hide();

        ModelNode op = address.asOperation(payload);
        op.get(OP).set(ADD);
        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error("Failed to add messaging provider", response.getFailureDescription());
                } else {
                    Console.info("Added messaging provider " + payload.get(NAME).asString());
                    currentServer = null;
                    loadProvider();
                }
            }
        });
    }

    @Override
    public void launchAddProviderDialog() {
        window = new DefaultWindow(Console.MESSAGES.createTitle("Messaging Provider"));
        window.setWidth(480);
        window.setHeight(360);

        window.trapWidget(
                new AddResourceDialog(
                        "{selected.profile}/subsystem=messaging-activemq/server=*",
                        Console.MODULES.getSecurityFramework()
                                .getSecurityContext(NameTokens.ActivemqMsgClusteringPresenter),
                        MsgClusteringPresenter.this
                )
        );
        window.setGlassEnabled(true);
        window.center();
    }

    @Override
    public void removeProvider(final String name) {
        ModelNode op = new ModelNode();
        op.get(ADDRESS).set(Baseadress.get());
        op.get(ADDRESS).add("subsystem", "messaging-activemq");
        op.get(ADDRESS).add("server", name);
        op.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error("Failed to remove messaging provider", response.getFailureDescription());
                } else {
                    if (name.equals(currentServer)) {
                        currentServer = null;
                    }
                    Console.info("Removed messaging provider " + name);
                    loadProvider();
                }
            }
        });
    }

    @Override
    public void onSaveProvider(final String name, Map<String, Object> changeset) {
        final ModelNodeAdapter adapter = new ModelNodeAdapter();

        ModelNode address = new ModelNode();
        address.get(ADDRESS).set(Baseadress.get());
        address.get(ADDRESS).add("subsystem", "messaging-activemq");
        address.get(ADDRESS).add("server", name);

        ModelNode operation = adapter.fromChangeset(changeset, address);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();

                if (response.isFailure()) {
                    Console.error("Failed to save provider " + name, response.getFailureDescription());
                } else {
                    Console.info("Successfully saved provider " + name);
                    loadProvider(); // refresh
                }
            }
        });
    }

    @Override
    public SecurityFramework getSecurityFramework() {
        return securityFramework;
    }

    @Override
    public ResourceDescriptionRegistry getDescriptionRegistry() {
        return descriptionRegistry;
    }

    @Override
    public String getNameToken() {
        return getProxy().getNameToken();
    }

    public void onAddSubResource(AddressTemplate address, ModelNode payload) {

        operationDelegate.onCreateResource(address, payload.get(NAME).asString(), payload, defaultOpCallbacks);
    }

    public void onRemoveResource(final AddressTemplate address, final String name) {

        operationDelegate.onRemoveResource(address, name, defaultOpCallbacks);
    }

    public void onSaveResource(final AddressTemplate address, String name, Map<String, Object> changeset) {

        operationDelegate.onSaveResource(address, name, changeset, defaultOpCallbacks);
    }
}
