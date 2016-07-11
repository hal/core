package org.jboss.as.console.client.shared.subsys.activemq.cluster;

import java.util.ArrayList;
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
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.behaviour.ModelNodeAdapter;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.widgets.AddResourceDialog;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.FilteringStatementContext;
import org.useware.kernel.gui.behaviour.StatementContext;

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.JGROUPS_CHANNEL;
import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.JGROUPS_STACK;
import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.NETWORK_SOCKET_BINDING;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 4/18/12
 */
public class MsgClusteringPresenter
        extends Presenter<MsgClusteringPresenter.MyView, MsgClusteringPresenter.MyProxy>
        implements CommonMsgPresenter {

    private final CrudOperationDelegate operationDelegate;

    CrudOperationDelegate.Callback defaultOpCallbacks = new CrudOperationDelegate.Callback() {
        @Override
        public void onSuccess(AddressTemplate address, String name) {
            Console.info(Console.MESSAGES.successfullyModifiedResource(address.resolve(statementContext, name).toString()));
            onReset();
        }

        @Override
        public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {
            Console.error(Console.MESSAGES.failedToModifyResource(addressTemplate.toString()), t.getMessage());
        }
    };

    // ------------------------------------------------------ proxy & view
    // @formatter:off
    @ProxyCodeSplit
    @NameToken(NameTokens.ActivemqMsgClusteringPresenter)
    @RequiredResources(resources = {
            "{selected.profile}/subsystem=messaging-activemq/server=*",
            "{selected.profile}/subsystem=messaging-activemq/server={activemq.server}/broadcast-group=*"
    }
    )
    @SearchIndex(keywords = {"jms", "messaging", "cluster", "broadcast", "discovery"})
    public interface MyProxy extends Proxy<MsgClusteringPresenter>, Place {}

    public interface MyView extends View {
        void setPresenter(MsgClusteringPresenter presenter);
        void setProvider(List<Property> result);
        void setSelectedProvider(String currentServer);
        void setBroadcastGroups(List<Property> groups);
        void setDiscoveryGroups(List<ActivemqDiscoveryGroup> groups);
        void setClusterConnection(List<ActivemqClusterConnection> groups);
    }
    // @formatter:on


    private final PlaceManager placeManager;
    private DispatchAsync dispatcher;
    private DefaultWindow window = null;
    private RevealStrategy revealStrategy;
    private final SecurityFramework securityFramework;
    private final ResourceDescriptionRegistry descriptionRegistry;
    private final StatementContext statementContext;
    private String currentServer = null;
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
                if("activemq.server".equals(key))
                    return currentServer;
                else
                    return null;
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
                    List<ActivemqDiscoveryGroup> groups = new ArrayList<>();
                    for (Property prop : model) {
                        ModelNode node = prop.getValue();
                        ActivemqDiscoveryGroup entity = discGroupAdapter.fromDMR(node);
                        entity.setName(prop.getName());
                        groups.add(entity);
                    }
                    getView().setDiscoveryGroups(groups);
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
                    List<ActivemqClusterConnection> groups = new ArrayList<>();
                    for (Property prop : model) {
                        ModelNode node = prop.getValue();
                        ActivemqClusterConnection entity = clusterConnectionsAdapter.fromDMR(node);
                        entity.setName(prop.getName());
                        groups.add(entity);
                    }
                    getView().setClusterConnection(groups);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void saveBroadcastGroup(final String name, Map<String, Object> changeset) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("broadcast-group", name);

        ModelNode addressNode = new ModelNode();
        addressNode.get(ADDRESS).set(address);

        ModelNode extra = null;
        List<String> items = (List<String>) changeset.get("connectors");
        if (items != null) {
            extra = new ModelNode();
            extra.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            extra.get(NAME).set("connectors");
            extra.get(ADDRESS).set(address);
            extra.get(VALUE).setEmptyList();
            for (String item : items) { extra.get(VALUE).add(item); }
        }

        ModelNode operation = extra != null ?
                bcastGroupAdapter.fromChangeset(changeset, addressNode, extra) :
                bcastGroupAdapter.fromChangeset(changeset, addressNode);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.modificationFailed("Broadcast Group " + name),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.modified("Broadcast Group " + name));
                }
                loadBroadcastGroups();
            }
        });
    }

    /*public void launchNewBroadcastGroupWizard() {
        loadExistingSocketBindings(new AsyncCallback<List<String>>() {
            @Override
            public void onFailure(Throwable throwable) {
                Console.error(Console.MESSAGES.failed("Loading socket bindings"), throwable.getMessage());
            }

            @Override
            public void onSuccess(List<String> names) {
                window = new DefaultWindow(Console.MESSAGES.createTitle("Broadcast Group"));
                window.setWidth(480);
                window.setHeight(450);
                window.trapWidget(new NewBroadcastGroupWizard(MsgClusteringPresenter.this, names).asWidget());
                window.setGlassEnabled(true);
                window.center();
            }
        });
    }
*/
    public String getCurrentServer() {
        return currentServer;
    }

    public void onCreateBroadcastGroup(final ActivemqBroadcastGroup entity) {
        closeDialogue();

        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("broadcast-group", entity.getName());

        ModelNode operation = bcastGroupAdapter.fromEntity(entity);
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(ADD);

        List<String> values = entity.getConnectors();
        if (!values.isEmpty()) {
            ModelNode list = new ModelNode();
            for (String con : values) { list.add(con); }
            operation.get("connectors").set(list);
        }

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.addingFailed("Broadcast Group " + entity.getName()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.added("Broadcast Group " + entity.getName()));
                }
                loadBroadcastGroups();
            }
        });
    }

    public void onDeleteBroadcastGroup(final String name) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("broadcast-group", name);

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.deletionFailed("Broadcast Group " + name),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.deleted("Broadcast Group " + name));
                }
                loadBroadcastGroups();
            }
        });
    }

    public void closeDialogue() {
        window.hide();
    }

    public void saveDiscoveryGroup(final String name, Map<String, Object> changeset) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("discovery-group", name);

        ModelNode addressNode = new ModelNode();
        addressNode.get(ADDRESS).set(address);

        ModelNode operation = discGroupAdapter.fromChangeset(changeset, addressNode);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.modificationFailed("Broadcast Group " + name),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.modified("Broadcast Group " + name));
                }
                loadDiscoveryGroups();
            }
        });
    }

    public void launchNewDiscoveryGroupWizard() {
        window = new DefaultWindow(Console.MESSAGES.createTitle("Discovery Group"));
        window.setWidth(480);
        window.setHeight(450);
        window.trapWidget(new NewDiscoveryGroupWizard(MsgClusteringPresenter.this).asWidget());
        window.setGlassEnabled(true);
        window.center();
    }

    public void onDeleteDiscoveryGroup(final String name) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("discovery-group", name);

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.deletionFailed("Discovery Group " + name),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.deleted("Discovery Group " + name));
                }
                loadDiscoveryGroups();
            }
        });
    }

    public void onCreateDiscoveryGroup(final ActivemqDiscoveryGroup entity) {
        closeDialogue();

        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("discovery-group", entity.getName());

        ModelNode operation = discGroupAdapter.fromEntity(entity);
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(ADD);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.addingFailed("Discovery Group " + entity.getName()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.added("Discovery Group " + entity.getName()));
                }
                loadDiscoveryGroups();
            }
        });
    }

    public void saveClusterConnection(final String name, Map<String, Object> changeset) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("cluster-connection", name);

        ModelNode addressNode = new ModelNode();
        addressNode.get(ADDRESS).set(address);

        ModelNode operation = clusterConnectionsAdapter.fromChangeset(changeset, addressNode);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.modificationFailed("Cluster Connection " + name),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.modified("Cluster Connection " + name));
                }
                loadClusterConnections();
            }
        });
    }

    public void launchNewClusterConnectionWizard() {
        window = new DefaultWindow(Console.MESSAGES.createTitle("Cluster Connection"));
        window.setWidth(480);
        window.setHeight(450);
        window.trapWidget(
                new NewClusterConnectionWizard(MsgClusteringPresenter.this).asWidget());
        window.setGlassEnabled(true);
        window.center();
    }

    public void onCreateClusterConnection(final ActivemqClusterConnection entity) {
        closeDialogue();

        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("cluster-connection", entity.getName());

        ModelNode operation = clusterConnectionsAdapter.fromEntity(entity);
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(ADD);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.addingFailed("Cluster Connection " + entity.getName()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.added("Cluster Connection " + entity.getName()));
                }
                loadClusterConnections();
            }
        });
    }

    public void onDeleteClusterConnection(final String name) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("cluster-connection", name);

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.deletionFailed("Cluster Connection " + name),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.deleted("Cluster Connection " + name));
                }
                loadClusterConnections();
            }
        });
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
                    if (name.equals(currentServer)) { currentServer = null; }
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

    public void onLaunchAddResourceDialog(AddressTemplate address) {
        String type = address.getResourceType();

        window = new DefaultWindow(Console.MESSAGES.createTitle(type.toUpperCase()));
        window.setWidth(480);
        window.setHeight(360);

        SecurityContext securityContext = Console.MODULES.getSecurityFramework().getSecurityContext(getProxy().getNameToken());
        ResourceDescription resourceDescription = descriptionRegistry.lookup(address);

        ModelNodeFormBuilder.FormAssets formAssets = new ModelNodeFormBuilder()
                                    .setCreateMode(true)
                                    .setConfigOnly()
                                    .setRequiredOnly(false)
                                    .includeOptionals(false)
                                    .include("connectors", "broadcast-period", "jgroups-channel", "jgroups-stack", "socket-binding")
                                    .addFactory("socket-binding", attributeDescription ->  {
                                        SuggestionResource suggestionResource = new SuggestionResource("socket-binding", "Socket binding", false,
                                                Console.MODULES.getCapabilities().lookup(NETWORK_SOCKET_BINDING));
                                        return suggestionResource.buildFormItem();
                                    })
                                    .addFactory("jgroups-stack", attributeDescription ->  {
                                        SuggestionResource suggestionResource = new SuggestionResource("jgroups-stack", "Jgroups stack", false,
                                                Console.MODULES.getCapabilities().lookup(JGROUPS_STACK));
                                        return suggestionResource.buildFormItem();
                                    })
                                    .addFactory("jgroups-channel", attributeDescription ->  {
                                        SuggestionResource suggestionResource = new SuggestionResource("jgroups-channel", "Jgroups channel", false,
                                                Console.MODULES.getCapabilities().lookup(JGROUPS_CHANNEL));
                                        return suggestionResource.buildFormItem();
                                    })
                                    .setResourceDescription(resourceDescription)
                                    .setSecurityContext(securityContext)
                                    .build();
                            formAssets.getForm().setEnabled(true);

        window.setWidget(
                new org.jboss.as.console.client.v3.widgets.AddResourceDialog(formAssets, resourceDescription,
                        new org.jboss.as.console.client.v3.widgets.AddResourceDialog.Callback() {
                            @Override
                            public void onAdd(ModelNode payload) {
                                window.hide();
                                operationDelegate.onCreateResource(
                                        address, payload.get("name").asString(), payload, defaultOpCallbacks);
                            }

                            @Override
                            public void onCancel() {
                                window.hide();
                            }
                        }
                )
        );

        window.setGlassEnabled(true);
        window.center();
    }

    public void onRemoveResource(final AddressTemplate address, final String name) {

        operationDelegate.onRemoveResource(address, name, defaultOpCallbacks);
    }

    public void onSaveResource(final AddressTemplate address, String name, Map<String, Object> changeset) {

        operationDelegate.onSaveResource(address, name, changeset, defaultOpCallbacks);
    }
}
