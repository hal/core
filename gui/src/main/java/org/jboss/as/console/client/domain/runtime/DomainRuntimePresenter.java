package org.jboss.as.console.client.domain.runtime;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.hosts.CopyServerWizard;
import org.jboss.as.console.client.domain.hosts.HostMgmtPresenter;
import org.jboss.as.console.client.domain.hosts.NewServerConfigWizard;
import org.jboss.as.console.client.domain.model.HostInformationStore;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.model.ServerGroupDAO;
import org.jboss.as.console.client.domain.model.ServerInstance;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.domain.model.impl.LifecycleOperation;
import org.jboss.as.console.client.domain.topology.LifecycleCallback;
import org.jboss.as.console.client.domain.topology.ServerInstanceOp;
import org.jboss.as.console.client.rbac.UnauthorisedPresenter;
import org.jboss.as.console.client.rbac.UnauthorizedEvent;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.shared.model.SubsystemLoader;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.shared.state.PerspectivePresenter;
import org.jboss.as.console.client.v3.presenter.Finder;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.v3.stores.domain.ServerRef;
import org.jboss.as.console.client.v3.stores.domain.ServerStore;
import org.jboss.as.console.client.v3.stores.domain.actions.AddServer;
import org.jboss.as.console.client.v3.stores.domain.actions.CopyServer;
import org.jboss.as.console.client.v3.stores.domain.actions.FilterType;
import org.jboss.as.console.client.v3.stores.domain.actions.GroupSelection;
import org.jboss.as.console.client.v3.stores.domain.actions.HostSelection;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshServer;
import org.jboss.as.console.client.v3.stores.domain.actions.RemoveServer;
import org.jboss.as.console.client.v3.stores.domain.actions.SelectServer;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.PropagatesChange;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;
import org.jboss.gwt.flow.client.PushFlowCallback;

import java.util.Collections;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 */
public class DomainRuntimePresenter
        extends PerspectivePresenter<DomainRuntimePresenter.MyView, DomainRuntimePresenter.MyProxy>
        implements Finder, PreviewEvent.Handler {


    private DefaultWindow window;

    @ProxyCodeSplit
    @NameToken(NameTokens.DomainRuntimePresenter)
    @RequiredResources(
            resources = {
                    "/{selected.host}/server-config=*"
            },
            recursive = false)
    public interface MyProxy extends Proxy<DomainRuntimePresenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(DomainRuntimePresenter presenter);
        void setSubsystems(List<SubsystemRecord> result);
        void updateServerList(List<Server> serverModel);
        void setPreview(SafeHtml html);
        void clearServerList();
    }

    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent =  new GwtEvent.Type<RevealContentHandler<?>>();


    private final Dispatcher circuit;
    private final ServerStore serverStore;
    private final HostInformationStore hostInfoStore;
    private final DispatchAsync dispatcher;
    private HandlerRegistration handlerRegistration;
    private final HostStore hostStore;
    private final PlaceManager placeManager;
    private final SubsystemLoader subsysStore;
    private final ServerGroupDAO serverGroupStore;

    @Inject
    public DomainRuntimePresenter(EventBus eventBus, MyView view, MyProxy proxy, PlaceManager placeManager,
                                  HostStore hostStore, SubsystemLoader subsysStore,
                                  ServerGroupDAO serverGroupStore, Header header,
                                  Dispatcher circuit, ServerStore serverStore, HostInformationStore hostInfoStore, DispatchAsync dispatcher) {

        super(eventBus, view, proxy, placeManager, header, NameTokens.DomainRuntimePresenter, TYPE_MainContent);

        this.placeManager = placeManager;

        this.hostStore = hostStore;
        this.subsysStore = subsysStore;
        this.serverGroupStore = serverGroupStore;
        this.circuit = circuit;
        this.serverStore = serverStore;
        this.hostInfoStore = hostInfoStore;
        this.dispatcher = dispatcher;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);

        HandlerRegistration previewReg = getEventBus().addHandler(PreviewEvent.TYPE, this);

        handlerRegistration = serverStore.addChangeHandler(new PropagatesChange.Handler() {
            @Override
            public void onChange(Action action) {

                if(!isVisible()) return; // don't process anything when not visible

                if(action instanceof SelectServer)
                {
                    // changing the server selection: update subsystems on server
                    if (hostStore.hasSelectedServer()) {
                        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                            @Override
                            public void execute() {
                                loadSubsystems();
                            }
                        });
                    }
                    else {
                        getView().setSubsystems(Collections.EMPTY_LIST);
                    }
                }

                // clear the view
                else if(action instanceof FilterType)
                {
                    getView().clearServerList();
                }

                // Refresh the server list when:
                // - changes to host/group filter refresh the server list
                // - group and host selection events
                // - server's are added or removed

                else if(
                        (action instanceof FilterType)
                                || (action instanceof GroupSelection)
                                || (action instanceof HostSelection)
                                || (action instanceof RemoveServer)
                                || (action instanceof AddServer)
                                || (action instanceof CopyServer)
                                || (action instanceof RefreshServer)
                        ) {

                    refreshServerList();
                }
            }
        });
    }

    private void refreshServerList() {
        if(FilterType.HOST.equals(serverStore.getFilter()))
        {
            String selectedHost = hostStore.getSelectedHost();

            List<Server> serverModel = Collections.EMPTY_LIST;
            if (selectedHost != null) {
                serverModel = serverStore.getServerForHost(
                        hostStore.getSelectedHost()
                );

            }

            getView().updateServerList(serverModel);
        }
        else if(FilterType.GROUP.equals(serverStore.getFilter()))
        {
            List<Server> serverModel = serverStore.getServerForGroup(serverStore.getSelectedGroup());
            getView().updateServerList(serverModel);
        }
    }

    @Override
    public void onPreview(PreviewEvent event) {
        getView().setPreview(event.getHtml());
    }

    @Override
    protected void onUnbind() {
        super.onUnbind();
        if (handlerRegistration != null) {
            handlerRegistration.removeHandler();
        }
    }

    @Override
    protected void onFirstReveal(final PlaceRequest placeRequest, PlaceManager placeManager, boolean revealDefault) {
        //circuit.dispatch(new RefreshServer());
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, HostMgmtPresenter.TYPE_MainContent, this);
    }

    private void loadSubsystems() {
        // clear view
        getView().setSubsystems(Collections.<SubsystemRecord>emptyList());

        final Function<FunctionContext> f2 = new Function<FunctionContext>() {
            @Override
            public void execute(final Control<FunctionContext> control) {
                final String serverSelection = hostStore.getSelectedServer();
                ServerInstance server = serverStore.getServerInstance(serverSelection);
                serverGroupStore.loadServerGroup(server.getGroup(), new PushFlowCallback<ServerGroupRecord>(control));
            }
        };
        final Function<FunctionContext> f3 = new Function<FunctionContext>() {
            @Override
            public void execute(final Control<FunctionContext> control) {
                ServerGroupRecord group = control.getContext().pop();
                subsysStore.loadSubsystems(group.getProfileName(),
                        new PushFlowCallback<List<SubsystemRecord>>(control));
            }
        };
        final Outcome<FunctionContext> outcome = new Outcome<FunctionContext>() {
            @Override
            public void onFailure(final FunctionContext context) {
                // TODO i18n
                Console.error("Cannot load subsystems of selected server");
            }

            @Override
            public void onSuccess(final FunctionContext context) {
                List<SubsystemRecord> subsystems = context.pop();
                getView().setSubsystems(subsystems);
            }
        };

        // load subsystems for selected server
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                new Async<FunctionContext>(Footer.PROGRESS_ELEMENT).waterfall(new FunctionContext(), outcome, f2, f3);
            }
        });

    }

    @Override
    protected void onReset() {
        super.onReset();
        refreshServerList();
    }

    public String getFilter() {
        return serverStore.getFilter();
    }

    public void onServerInstanceLifecycle(final String host, final String server, final LifecycleOperation op) {

        ServerInstanceOp serverInstanceOp = new ServerInstanceOp(
                op, new LifecycleCallback() {
            @Override
            public void onSuccess() {
                circuit.dispatch(new RefreshServer());
            }

            @Override
            public void onTimeout() {
                Console.warning("Request timeout");
                circuit.dispatch(new RefreshServer());
            }

            @Override
            public void onAbort() {
                Console.warning("Request aborted.");
                circuit.dispatch(new RefreshServer());
            }

            @Override
            public void onError(Throwable caught) {
                Console.error("Server " + op.name() + " failed", caught.getMessage());
                circuit.dispatch(new RefreshServer());
            }
        }, dispatcher, hostInfoStore,
                host, server
        );
        serverInstanceOp.run();

    }

    public void launchNewConfigDialoge() {

        // TODO: server group store (circuit)
        serverGroupStore.loadServerGroups(new SimpleCallback<List<ServerGroupRecord>>() {
            @Override
            public void onSuccess(List<ServerGroupRecord> serverGroups) {
                window = new DefaultWindow(Console.MESSAGES.createTitle("New Server Configuration"));
                window.setWidth(640);
                window.setHeight(480);

                NewServerConfigWizard wizard = new NewServerConfigWizard(DomainRuntimePresenter.this);
                Widget w = wizard.asWidget();



                wizard.updateGroups(serverGroups);
                wizard.updateHosts(hostStore.getHostNames());
                window.trapWidget(w);

                window.setGlassEnabled(true);
                window.center();
            }
        });


    }


    public void onCreateServerConfig(final Server newServer) {

        circuit.dispatch(new AddServer(newServer));
        if(window!=null)
            window.hide();
    }


    public void tryDelete(final ServerRef server) {

        closeWindow();

        // check if instance exist
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).add("host", server.getHostName());
        operation.get(ADDRESS).add("server-config", server.getServerName());
        operation.get(INCLUDE_RUNTIME).set(true);
        operation.get(OP).set(READ_RESOURCE_OPERATION);

        //System.out.println(operation);
        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {

            @Override
            public void onFailure(Throwable throwable) {
                Console.error("Failed to delete server", throwable.getMessage());
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                String outcome = response.get(OUTCOME).asString();

                Boolean serverIsRunning = Boolean.FALSE;

                // 2.0.x
                if (outcome.equals(SUCCESS)) {
                    serverIsRunning = response.get(RESULT).get("status").asString().equalsIgnoreCase("started");
                }

                if (!serverIsRunning)
                {
                    performDeleteOperation(server);
                }
                else
                {
                    Console.error(
                            Console.MESSAGES.deletionFailed("Server Configuration"),
                            Console.MESSAGES.server_config_stillRunning(server.getServerName())
                    );
                }
            }
        });


    }

    private void performDeleteOperation(final ServerRef server) {

        circuit.dispatch(new RemoveServer(server));
    }

    public void closeWindow() {
        if(window!=null)
            window.hide();
    }

    public String getSelectedGroup() {
        return serverStore.getSelectedGroup();
    }

    public ServerRef getSelectedServer() {
        return serverStore.getSelectServer();
    }


    public String getSelectedHost() {
        return hostStore.getSelectedHost();
    }

    public void onLaunchCopyWizard(final Server server) {

        window = new DefaultWindow("Copy Server Configuration");
        window.setWidth(480);
        window.setHeight(380);

        CopyServerWizard wizard = new CopyServerWizard(DomainRuntimePresenter.this);
        Widget widget = wizard.asWidget();

        wizard.setCurrentServerSelection(server);
        wizard.setHosts(hostStore.getHostNames(), hostStore.getSelectedHost());
        window.trapWidget(widget);

        window.setGlassEnabled(true);
        window.center();
    }

    public void onSaveCopy(final String targetHost, final Server original, final Server newServer) {
        closeWindow();
        circuit.dispatch(new CopyServer(targetHost, original, newServer));

    }



}
