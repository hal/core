package org.jboss.as.console.client.shared.subsys.activemq.connections;

import java.util.ArrayList;
import java.util.Collections;
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
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.properties.NewPropertyWizard;
import org.jboss.as.console.client.shared.properties.PropertyManagement;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.activemq.AggregatedJMSModel;
import org.jboss.as.console.client.shared.subsys.activemq.CommonMsgPresenter;
import org.jboss.as.console.client.shared.subsys.activemq.LoadActivemqServersCmd;
import org.jboss.as.console.client.shared.subsys.activemq.LoadJMSCmd;
import org.jboss.as.console.client.shared.subsys.activemq.model.AcceptorType;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqAcceptor;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqBridge;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnectionFactory;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnector;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnectorService;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqJMSEndpoint;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqJMSQueue;
import org.jboss.as.console.client.shared.subsys.activemq.model.ConnectorType;
import org.jboss.as.console.client.shared.subsys.jca.model.CredentialReference;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.as.console.mbui.behaviour.ModelNodeAdapter;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.widgets.AddResourceDialog;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.StatementContext;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 4/2/12
 */
public class MsgConnectionsPresenter extends Presenter<MsgConnectionsPresenter.MyView, MsgConnectionsPresenter.MyProxy>
        implements CommonMsgPresenter, PropertyManagement {

    // ------------------------------------------------------ proxy & view
    // @formatter:off
    @ProxyCodeSplit
    @NameToken(NameTokens.ActivemqMsgConnectionsPresenter)
    @RequiredResources(resources = {
            "{selected.profile}/subsystem=messaging-activemq/server=*",
            "{selected.profile}/subsystem=messaging-activemq/jms-bridge=*"})
    @SearchIndex(keywords = {"jms", "acceptor", "bridge", "connector"})
    public interface MyProxy extends Proxy<MsgConnectionsPresenter>, Place {}


    public interface MyView extends View, HasPresenter<MsgConnectionsPresenter> {
        void setSelectedProvider(String selectedProvider);
        void setProvider(List<Property> provider);
        void setGenericAcceptors(List<ActivemqAcceptor> genericAcceptors);
        void setRemoteAcceptors(List<ActivemqAcceptor> remote);
        void setInvmAcceptors(List<ActivemqAcceptor> invm);
        void setGenericConnectors(List<ActivemqConnector> generic);
        void setRemoteConnectors(List<ActivemqConnector> remote);
        void setInvmConnectors(List<ActivemqConnector> invm);
        void setConnetorServices(List<ActivemqConnectorService> services);
        void setBridges(List<ActivemqBridge> bridges);
        void setConnectionFactories(List<ActivemqConnectionFactory> factories);
        void setPooledConnectionFactories(List<Property> models);
    }
    // @formatter:on


    static final String PARAMS_MAP = "params";
    public static final AddressTemplate MESSAGING_SERVER = AddressTemplate.of("{selected.profile}/subsystem=messaging-activemq/server=*");
    public static final AddressTemplate BRIDGE_TEMPLATE = MESSAGING_SERVER.append("bridge=*");

    private final PlaceManager placeManager;
    private DispatchAsync dispatcher;
    private BeanFactory factory;
    private DefaultWindow window = null;
    private RevealStrategy revealStrategy;
    private final SecurityFramework securityFramework;
    private final ResourceDescriptionRegistry descriptionRegistry;
    private final StatementContext statementContext;
    private String currentServer = null;
    private EntityAdapter<ActivemqAcceptor> acceptorAdapter;
    private EntityAdapter<ActivemqConnector> connectorAdapter;
    private EntityAdapter<ActivemqConnectorService> connectorServiceAdapter;
    private EntityAdapter<ActivemqBridge> bridgeAdapter;
    private EntityAdapter<ActivemqConnectionFactory> factoryAdapter;
    private final EntityAdapter<CredentialReference> credentialReferenceAdapter;
    private LoadJMSCmd loadJMSCmd;
    private DefaultWindow propertyWindow;

    @Inject
    public MsgConnectionsPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            PlaceManager placeManager, DispatchAsync dispatcher,
            BeanFactory factory, RevealStrategy revealStrategy,
            ApplicationMetaData propertyMetaData, SecurityFramework securityFramework,
            ResourceDescriptionRegistry descriptionRegistry, StatementContext statementContext) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.dispatcher = dispatcher;
        this.factory = factory;
        this.revealStrategy = revealStrategy;
        this.securityFramework = securityFramework;
        this.descriptionRegistry = descriptionRegistry;
        this.statementContext = statementContext;

        acceptorAdapter = new EntityAdapter<>(ActivemqAcceptor.class, propertyMetaData);
        connectorAdapter = new EntityAdapter<>(ActivemqConnector.class, propertyMetaData);
        connectorServiceAdapter = new EntityAdapter<>(ActivemqConnectorService.class, propertyMetaData);
        bridgeAdapter = new EntityAdapter<>(ActivemqBridge.class, propertyMetaData);
        factoryAdapter = new EntityAdapter<>(ActivemqConnectionFactory.class, propertyMetaData);
        this.credentialReferenceAdapter = new EntityAdapter<>(CredentialReference.class, propertyMetaData);
        loadJMSCmd = new LoadJMSCmd(dispatcher, factory, propertyMetaData);
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

    @Override
    public void prepareFromRequest(PlaceRequest request) {
        currentServer = request.getParameter("name", null);
    }

    @Override
    protected void onReset() {
        super.onReset();

        loadProvider();
    }

    public void loadDetails(String selectedProvider) {
        loadAcceptors();
        loadConnectors();
        loadConnectorServices();
        loadBridges();
        loadConnectionFactories();
        loadPooledConnectionFactory();
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


    public void loadBridges() {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", getCurrentServer());
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(CHILD_TYPE).set("bridge");
        operation.get(RECURSIVE).set(true);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failed("Loading bridges " + getCurrentServer()),
                            response.getFailureDescription());
                } else {
                    List<Property> model = response.get(RESULT).asPropertyList();
                    List<ActivemqBridge> bridges = new ArrayList<ActivemqBridge>();
                    for (Property prop : model) {
                        ModelNode svc = prop.getValue();
                        ActivemqBridge entity = bridgeAdapter.fromDMR(svc);
                        entity.setName(prop.getName());
                        if (svc.hasDefined(CREDENTIAL_REFERENCE)) {
                            ModelNode cred = svc.get(CREDENTIAL_REFERENCE);
                            CredentialReference credentialReference = credentialReferenceAdapter.fromDMR(cred);
                            entity.setCredentialReference(credentialReference);
                        }

                        entity.setStaticConnectors(EntityAdapter.modelToList(svc, "static-connectors"));
                        bridges.add(entity);
                    }
                    getView().setBridges(bridges);
                }
            }
        });
    }

    public void loadConnectorServices() {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", getCurrentServer());
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(CHILD_TYPE).set("connector-service");
        operation.get(RECURSIVE).set(true);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failed("Loading connector services " + getCurrentServer()),
                            response.getFailureDescription());
                } else {
                    List<Property> model = response.get(RESULT).asPropertyList();
                    List<ActivemqConnectorService> services = new ArrayList<>();
                    for (Property prop : model) {
                        ModelNode svc = prop.getValue();
                        ActivemqConnectorService entity = connectorServiceAdapter.fromDMR(svc);
                        entity.setName(prop.getName());

                        if (svc.hasDefined(PARAMS_MAP)) {
                            List<PropertyRecord> param = parseProperties(svc.get(PARAMS_MAP).asPropertyList());
                            entity.setParameter(param);
                        } else {
                            entity.setParameter(Collections.emptyList());
                        }
                        services.add(entity);
                    }
                    getView().setConnetorServices(services);
                }
            }
        });
    }

    public void loadConnectors() {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).setEmptyList();
        operation.get(OP).set(COMPOSITE);

        List<ModelNode> steps = new ArrayList<>();

        ModelNode generic = new ModelNode();
        generic.get(ADDRESS).set(Baseadress.get());
        generic.get(ADDRESS).add("subsystem", "messaging-activemq");
        generic.get(ADDRESS).add("server", getCurrentServer());
        generic.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        generic.get(CHILD_TYPE).set("connector");
        generic.get(RECURSIVE).set(true);
        steps.add(generic);

        ModelNode remote = new ModelNode();
        remote.get(ADDRESS).set(Baseadress.get());
        remote.get(ADDRESS).add("subsystem", "messaging-activemq");
        remote.get(ADDRESS).add("server", getCurrentServer());
        remote.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        remote.get(CHILD_TYPE).set("remote-connector");
        remote.get(RECURSIVE).set(true);
        steps.add(remote);

        ModelNode invm = new ModelNode();
        invm.get(ADDRESS).set(Baseadress.get());
        invm.get(ADDRESS).add("subsystem", "messaging-activemq");
        invm.get(ADDRESS).add("server", getCurrentServer());
        invm.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        invm.get(CHILD_TYPE).set("in-vm-connector");
        invm.get(RECURSIVE).set(true);
        steps.add(invm);

        operation.get(STEPS).set(steps);
        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failed("Loading connectors " + getCurrentServer()),
                            response.getFailureDescription());
                } else {
                    List<ActivemqConnector> generic = parseConnectors(response.get(RESULT).get("step-1"),
                            ConnectorType.GENERIC);
                    getView().setGenericConnectors(generic);

                    List<ActivemqConnector> remote = parseConnectors(response.get(RESULT).get("step-2"),
                            ConnectorType.REMOTE);
                    getView().setRemoteConnectors(remote);

                    List<ActivemqConnector> invm = parseConnectors(response.get(RESULT).get("step-3"),
                            ConnectorType.INVM);
                    getView().setInvmConnectors(invm);
                }
            }
        });
    }

    public void loadAcceptors() {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).setEmptyList();
        operation.get(OP).set(COMPOSITE);

        List<ModelNode> steps = new ArrayList<>();

        ModelNode generic = new ModelNode();
        generic.get(ADDRESS).set(Baseadress.get());
        generic.get(ADDRESS).add("subsystem", "messaging-activemq");
        generic.get(ADDRESS).add("server", getCurrentServer());
        generic.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        generic.get(CHILD_TYPE).set("acceptor");
        generic.get(RECURSIVE).set(true);
        steps.add(generic);

        ModelNode remote = new ModelNode();
        remote.get(ADDRESS).set(Baseadress.get());
        remote.get(ADDRESS).add("subsystem", "messaging-activemq");
        remote.get(ADDRESS).add("server", getCurrentServer());
        remote.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        remote.get(CHILD_TYPE).set("remote-acceptor");
        remote.get(RECURSIVE).set(true);
        steps.add(remote);

        ModelNode invm = new ModelNode();
        invm.get(ADDRESS).set(Baseadress.get());
        invm.get(ADDRESS).add("subsystem", "messaging-activemq");
        invm.get(ADDRESS).add("server", getCurrentServer());
        invm.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        invm.get(CHILD_TYPE).set("in-vm-acceptor");
        invm.get(RECURSIVE).set(true);
        steps.add(invm);

        operation.get(STEPS).set(steps);
        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failed("Loading acceptors " + getCurrentServer()),
                            response.getFailureDescription());
                } else {
                    List<ActivemqAcceptor> generic = parseAcceptors(response.get(RESULT).get("step-1"),
                            AcceptorType.GENERIC);
                    getView().setGenericAcceptors(generic);

                    List<ActivemqAcceptor> remote = parseAcceptors(response.get(RESULT).get("step-2"),
                            AcceptorType.REMOTE);
                    getView().setRemoteAcceptors(remote);

                    List<ActivemqAcceptor> invm = parseAcceptors(response.get(RESULT).get("step-3"), AcceptorType.INVM);
                    getView().setInvmAcceptors(invm);
                }
            }
        });

    }

    private List<ActivemqConnector> parseConnectors(ModelNode step, ConnectorType type) {
        List<Property> generic = step.get(RESULT).asPropertyList();
        List<ActivemqConnector> genericAcceptors = new ArrayList<>();
        for (Property prop : generic) {
            ModelNode acceptor = prop.getValue();
            ActivemqConnector model = connectorAdapter.fromDMR(acceptor);
            model.setName(prop.getName());
            model.setType(type);

            if (acceptor.hasDefined(PARAMS_MAP)) {
                List<PropertyRecord> param = parseProperties(acceptor.get(PARAMS_MAP).asPropertyList());
                model.setParameter(param);
            } else {
                model.setParameter(Collections.emptyList());
            }
            genericAcceptors.add(model);
        }
        return genericAcceptors;
    }

    private List<ActivemqAcceptor> parseAcceptors(ModelNode step, AcceptorType type) {
        List<Property> generic = step.get(RESULT).asPropertyList();
        List<ActivemqAcceptor> genericAcceptors = new ArrayList<>();
        for (Property prop : generic) {
            ModelNode acceptor = prop.getValue();
            ActivemqAcceptor model = acceptorAdapter.fromDMR(acceptor);
            model.setName(prop.getName());
            model.setType(type);

            if (acceptor.hasDefined(PARAMS_MAP)) {
                List<PropertyRecord> param = parseProperties(acceptor.get(PARAMS_MAP).asPropertyList());
                model.setParameter(param);
            } else {
                model.setParameter(Collections.emptyList());
            }
            genericAcceptors.add(model);
        }
        return genericAcceptors;
    }

    private List<PropertyRecord> parseProperties(List<Property> properties) {
        List<PropertyRecord> records = new ArrayList<>(properties.size());
        for (Property prop : properties) {
            String name = prop.getName();
            String value = prop.getValue().asString();
            PropertyRecord propertyRecord = factory.property().as();
            propertyRecord.setKey(name);
            propertyRecord.setValue(value);
            records.add(propertyRecord);
        }
        return records;
    }

    public String getCurrentServer() {
        return currentServer;
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    public void launchNewAcceptorWizard(final AcceptorType type) {
        window = new DefaultWindow(Console.MESSAGES.createTitle(type.name().toUpperCase() + " Acceptor"));
        window.setWidth(480);
        window.setHeight(360);
        window.trapWidget(new NewAcceptorWizard(MsgConnectionsPresenter.this, type).asWidget());
        window.setGlassEnabled(true);
        window.center();
    }

    public void onDeleteAcceptor(final ActivemqAcceptor entity) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add(entity.getType().getResource(), entity.getName());

        ModelNode operation = acceptorAdapter.fromEntity(entity);
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.deletionFailed("Acceptor " + entity.getName()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.deleted("Acceptor " + entity.getName()));
                }
                loadAcceptors();
            }
        });
    }

    public void onSaveAcceptor(final ActivemqAcceptor entity, Map<String, Object> changeset) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add(entity.getType().getResource(), entity.getName());

        ModelNode addressNode = new ModelNode();
        addressNode.get(ADDRESS).set(address);

        ModelNode operation = acceptorAdapter.fromChangeset(changeset, addressNode);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.modificationFailed("Acceptor " + entity.getName()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.modified("Acceptor " + entity.getName()));
                }
                loadAcceptors();
            }
        });
    }

    public void loadSocketBindings(AsyncCallback<List<String>> callback) {
        // TODO
        callback.onSuccess(Collections.emptyList());
    }

    public void onCreateAcceptor(final ActivemqAcceptor entity) {
        window.hide();

        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add(entity.getType().getResource(), entity.getName());

        ModelNode operation = acceptorAdapter.fromEntity(entity);
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(ADD);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.addingFailed("Acceptor " + entity.getName()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.added("Acceptor " + entity.getName()));
                }
                loadAcceptors();
            }
        });
    }

    public void launchNewConnectorWizard(final ConnectorType type) {
        window = new DefaultWindow(Console.MESSAGES.createTitle(type.name().toUpperCase() + " Connector"));
        window.setWidth(480);
        window.setHeight(360);
        window.trapWidget(new NewConnectorWizard(MsgConnectionsPresenter.this, type).asWidget());
        window.setGlassEnabled(true);
        window.center();
    }

    public void onDeleteConnector(final ActivemqConnector entity) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add(entity.getType().getResource(), entity.getName());

        ModelNode operation = connectorAdapter.fromEntity(entity);
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.deletionFailed("Connector " + entity.getName()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.deleted("Connector " + entity.getName()));
                }
                loadConnectors();
            }
        });
    }

    public void onSaveConnector(final ActivemqConnector entity, Map<String, Object> changeset) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add(entity.getType().getResource(), entity.getName());

        ModelNode addressNode = new ModelNode();
        addressNode.get(ADDRESS).set(address);

        ModelNode operation = connectorAdapter.fromChangeset(changeset, addressNode);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.modificationFailed("Connector " + entity.getName()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.modified("Connector " + entity.getName()));
                }
                loadConnectors();
            }
        });
    }

    public void onCreateConnector(final ActivemqConnector entity) {
        closeDialogue();

        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add(entity.getType().getResource(), entity.getName());

        ModelNode operation = connectorAdapter.fromEntity(entity);
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(ADD);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.addingFailed("Connector " + entity.getName()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.added("Connector " + entity.getName()));
                }
                loadConnectors();
            }
        });
    }

    public void closeDialogue() {
        window.hide();
    }

    public void launchNewConnectorServiceWizard() {
        window = new DefaultWindow(Console.MESSAGES.createTitle("Connector Service"));
        window.setWidth(480);
        window.setHeight(360);
        window.trapWidget(new NewConnectorServiceWizard(MsgConnectionsPresenter.this).asWidget());
        window.setGlassEnabled(true);
        window.center();
    }

    public void onCreateConnectorService(final ActivemqConnectorService entity) {
        closeDialogue();

        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("connector-service", entity.getName());

        ModelNode operation = connectorServiceAdapter.fromEntity(entity);
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(ADD);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.addingFailed("Connector Service " + entity.getName()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.added("Connector Service " + entity.getName()));
                }
                loadConnectorServices();
            }
        });
    }

    public void onDeleteConnectorService(final ActivemqConnectorService entity) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("connector-service", entity.getName());

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.deletionFailed("Connector Service " + entity.getName()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.deleted("Connector Service " + entity.getName()));
                }
                loadConnectorServices();
            }
        });
    }

    public void onSaveConnectorService(final ActivemqConnectorService entity, Map<String, Object> changeset) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("connector-service", entity.getName());

        ModelNode addressNode = new ModelNode();
        addressNode.get(ADDRESS).set(address);

        ModelNode operation = connectorServiceAdapter.fromChangeset(changeset, addressNode);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.modificationFailed("Connector Service " + entity.getName()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.modified("Connector Service " + entity.getName()));
                }
                loadConnectorServices();
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void onSaveBridge(final String name, Map<String, Object> changeset) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("bridge", name);

        ModelNode addressNode = new ModelNode();
        addressNode.get(ADDRESS).set(address);

        ModelNode extra = null;
        List<String> items = null;
        Object staticConnectors = changeset.get("staticConnectors");
        if (staticConnectors instanceof List) {
            items = (List<String>) staticConnectors;
        }
        if (items != null && items.size() > 0) { // non-empty list
            extra = new ModelNode();
            extra.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            extra.get(NAME).set("static-connectors");
            extra.get(ADDRESS).set(address);
            extra.get(VALUE).setEmptyList();
            for (String item : items) { extra.get(VALUE).add(item); }
        } else if ((items != null && items.size() == 0)
                || FormItem.VALUE_SEMANTICS.UNDEFINED.equals(staticConnectors)) { // empty list or "undefined"
            extra = new ModelNode();
            extra.get(OP).set(UNDEFINE_ATTRIBUTE_OPERATION);
            extra.get(NAME).set("static-connectors");
            extra.get(ADDRESS).set(address);
        }

        ModelNode operation = extra != null ?
                bridgeAdapter.fromChangeset(changeset, addressNode, extra) :
                bridgeAdapter.fromChangeset(changeset, addressNode);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.modificationFailed("Bridge " + name),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.modified("Bridge " + name));
                }
                loadBridges();
            }
        });
    }

    public void launchNewBridgeWizard() {
        loadExistingQueueNames(new AsyncCallback<List<String>>() {
            @Override
            public void onFailure(Throwable throwable) {
                Console.error(Console.MESSAGES.failed("Load queue names"));
            }

            @Override
            public void onSuccess(List<String> names) {
                window = new DefaultWindow(Console.MESSAGES.createTitle("Bridge"));
                window.setWidth(480);
                window.setHeight(450);
                window.trapWidget(new NewBridgeWizard(MsgConnectionsPresenter.this, names).asWidget());
                window.setGlassEnabled(true);
                window.center();
            }
        });
    }

    public void onCreateBridge(final ActivemqBridge entity) {
        closeDialogue();

        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("bridge", entity.getName());

        ModelNode operation = bridgeAdapter.fromEntity(entity);
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(ADD);

        List<String> values = entity.getStaticConnectors();
        if (!values.isEmpty()) {
            ModelNode list = new ModelNode();
            for (String con : values) { list.add(con); }

            operation.get("static-connectors").set(list);
            operation.remove("discovery-group");
        }

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.addingFailed("Bridge " + entity.getName()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.added("Bridge " + entity.getName()));
                }
                loadBridges();
            }
        });
    }

    public void onDeleteBridge(final String name) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("bridge", name);

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.deletionFailed("Bridge " + name), response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.deleted("Bridge " + name));
                }
                loadBridges();
            }
        });
    }

    public void loadExistingQueueNames(final AsyncCallback<List<String>> callback) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());

        loadJMSCmd.execute(address, new SimpleCallback<AggregatedJMSModel>() {
            @Override
            public void onSuccess(AggregatedJMSModel result) {

                final List<String> names = new ArrayList<>();

                for (ActivemqJMSQueue queue : result.getJMSQueues()) {
                    names.add(queue.getName());
                }

                for (ActivemqJMSEndpoint topic : result.getTopics()) {
                    names.add(topic.getName());
                }
                callback.onSuccess(names);
            }
        });
    }

    @Override
    public void onCreateProperty(String reference, PropertyRecord prop) {
        closePropertyDialoge();

        String[] tokens = reference.split("_#_");

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", getCurrentServer());
        operation.get(ADDRESS).add(tokens[0], tokens[1]);

        operation.get(OP).set("map-put");
        operation.get(NAME).set(PARAMS_MAP);
        operation.get("key").set(prop.getKey());
        operation.get(VALUE).set(prop.getValue());

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.addingFailed("Config Parameter"), response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.added("Config Parameter"));
                    loadDetails(currentServer);
                }
            }
        });
    }

    @Override
    public void onDeleteProperty(String reference, PropertyRecord prop) {
        String[] tokens = reference.split("_#_");

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", getCurrentServer());
        operation.get(ADDRESS).add(tokens[0], tokens[1]);

        operation.get(OP).set("map-remove");
        operation.get(NAME).set(PARAMS_MAP);
        operation.get("key").set(prop.getKey());

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.deletionFailed("Config Parameter"),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.deleted("Config Parameter"));
                    loadDetails(currentServer);
                }
            }
        });
    }

    @Override
    public void onChangeProperty(String reference, PropertyRecord prop) {}

    @Override
    public void launchNewPropertyDialoge(String reference) {
        propertyWindow = new DefaultWindow(Console.MESSAGES.createTitle("Config Parameter"));
        propertyWindow.setWidth(480);
        propertyWindow.setHeight(360);
        propertyWindow.trapWidget(new NewPropertyWizard(this, reference, false).asWidget());
        propertyWindow.setGlassEnabled(true);
        propertyWindow.center();
    }

    @Override
    public void closePropertyDialoge() {
        propertyWindow.hide();
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
                                .getSecurityContext(NameTokens.ActivemqMsgConnectionsPresenter),
                        MsgConnectionsPresenter.this
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

    @SuppressWarnings("unchecked")
    public void saveConnnectionFactory(String name, Map<String, Object> changeset) {
        ModelNode address = new ModelNode();
        address.get(ADDRESS).set(Baseadress.get());
        address.get(ADDRESS).add("subsystem", "messaging-activemq");
        address.get(ADDRESS).add("server", getCurrentServer());
        address.get(ADDRESS).add("connection-factory", name);

        ModelNode operation = factoryAdapter.fromChangeset(changeset, address);
        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.saveFailed("Connection Factory " + getCurrentServer()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.saved("Connection Factory " + getCurrentServer()));
                }
                loadConnectionFactories();
            }
        });
    }

    public void onDeleteCF(final String name) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("connection-factory", name);

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.deletionFailed("Connection Factory " + name),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.deleted("Connection Factory " + name));
                }
                loadConnectionFactories();
            }
        });
    }

    public void launchNewCFWizard() {
        window = new DefaultWindow(Console.MESSAGES.createTitle("Connection Factory"));
        window.setWidth(480);
        window.setHeight(360);
        window.trapWidget(new NewCFWizard(this).asWidget());
        window.setGlassEnabled(true);
        window.center();
    }

    public void onCreateCF(final ActivemqConnectionFactory entity) {
        window.hide();

        // default values
        entity.setUseGlobalPools(true);

        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("connection-factory", entity.getName());

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(ADD);

        // jndi names
        operation.get("entries").setEmptyList();
        for (String jndiEntry : entity.getEntries()) {
            operation.get("entries").add(jndiEntry);
        }

        // connectors
        for (String connector : entity.getConnectors()) {
            operation.get("connectors").add(connector);
        }

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.addingFailed("Connection Factory " + entity.getName()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.added("Connection Factory " + entity.getName()));
                }
                loadConnectionFactories();
            }
        });
    }

    private void loadConnectionFactories() {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", getCurrentServer());
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(CHILD_TYPE).set("connection-factory");
        operation.get(RECURSIVE).set(true);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failed("Loading connection factories " + getCurrentServer()),
                            response.getFailureDescription());
                } else {
                    List<Property> model = response.get(RESULT).asPropertyList();
                    List<ActivemqConnectionFactory> connectionFactories = new ArrayList<>();
                    for (Property prop : model) {
                        ModelNode svc = prop.getValue();
                        ActivemqConnectionFactory entity = factoryAdapter.fromDMR(svc);
                        entity.setName(prop.getName());

                        entity.setConnectors(EntityAdapter.modelToList(svc, "connectors"));
                        entity.setEntries(EntityAdapter.modelToList(svc, "entries"));
                        connectionFactories.add(entity);
                    }
                    getView().setConnectionFactories(connectionFactories);
                }
            }
        });
    }

    private void loadPooledConnectionFactory() {

        org.jboss.as.console.client.v3.dmr.ResourceAddress pooledAddress = MESSAGING_SERVER
                .resolve(statementContext, getCurrentServer());
        Operation op = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, pooledAddress)
                .param(CHILD_TYPE, "pooled-connection-factory")
                .param(RECURSIVE, true)
                .build();

        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failed("Loading pooled connection factory " + getCurrentServer()),
                            response.getFailureDescription());
                } else {
                    List<Property> model = response.get(RESULT).asPropertyList();
                    getView().setPooledConnectionFactories(model);
                }
            }
        });
    }

    public void addPooledConnectionFactory(ModelNode payload) {

        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        String resourceName = payload.get(NAME).asString();
        address.add("pooled-connection-factory", resourceName);

        ModelNode operation = payload;
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(ADD);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.addingFailed("Pooled Connection Factory " + resourceName),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.added("pooled Connection Factory " + resourceName));
                }
                loadPooledConnectionFactory();
            }
        });
    }

    public void onDeletePooledCF(final String name) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("pooled-connection-factory", name);

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.deletionFailed("Pooled Connection Factory " + name),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.deleted("Pooled Connection Factory " + name));
                }
                loadPooledConnectionFactory();
            }
        });
    }


    public void onSavePooledCF(final String name, Map<String, Object> changeset) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("pooled-connection-factory", name);

        ModelNode addressNode = new ModelNode();
        addressNode.get(ADDRESS).set(address);

        final ModelNodeAdapter adapter = new ModelNodeAdapter();
        ModelNode op = adapter.fromChangeset(changeset, addressNode);

        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.modificationFailed("Pooled Connector Factory " + name),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.modified("Pooled Connector Factory " + name));
                }
                loadPooledConnectionFactory();
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

    public void saveAttribute(final String resourceName, final ModelNode payload) {
        org.jboss.as.console.client.v3.dmr.ResourceAddress address = BRIDGE_TEMPLATE.resolve(statementContext, currentServer, resourceName);
        for (Property prop : payload.asPropertyList()) {
            if (!prop.getValue().isDefined()) {
                payload.remove(prop.getName());
            }
        }
        ModelNode operation;
        if (payload.asList().size()  > 0) {
            org.jboss.as.console.client.v3.behaviour.ModelNodeAdapter adapter = new org.jboss.as.console.client.v3.behaviour.ModelNodeAdapter();
            operation = adapter.fromComplexAttribute(address, CREDENTIAL_REFERENCE, payload);
        } else {
            // if the payload is empty, undefine the complex attribute
            // otherwise an empty attribute is a defined attribute and as the user wants to remove all
            // values, it is better to undefine it.
            operation = new ModelNode();
            operation.get(ADDRESS).set(address);
            operation.get(OP).set(UNDEFINE_ATTRIBUTE_OPERATION);
            operation.get(NAME).set(CREDENTIAL_REFERENCE);
        }

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                Console.error(Console.MESSAGES.modificationFailed("Bridge " + resourceName),
                        caught.getMessage());
                loadBridges();
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                Console.info(Console.MESSAGES.modified("Bridge " + resourceName));
                loadBridges();
            }
        });
    }




    public EntityAdapter<CredentialReference> getCredentialReferenceAdapter() {
        return credentialReferenceAdapter;
    }

}
