package org.jboss.as.console.client.domain.runtime;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.UseGatekeeper;
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
import org.jboss.as.console.client.domain.hosts.HostMgmtPresenter;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.model.ServerGroupStore;
import org.jboss.as.console.client.domain.model.ServerInstance;
import org.jboss.as.console.client.rbac.UnauthorisedPresenter;
import org.jboss.as.console.client.rbac.UnauthorizedEvent;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.shared.model.SubsystemLoader;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.shared.state.PerspectivePresenter;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.v3.stores.domain.ServerStore;
import org.jboss.as.console.client.v3.stores.domain.actions.AddServer;
import org.jboss.as.console.client.v3.stores.domain.actions.FilterType;
import org.jboss.as.console.client.v3.stores.domain.actions.GroupSelection;
import org.jboss.as.console.client.v3.stores.domain.actions.HostSelection;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshServer;
import org.jboss.as.console.client.v3.stores.domain.actions.RemoveServer;
import org.jboss.as.console.client.v3.stores.domain.actions.SelectServer;
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

/**
 * @author Heiko Braun
 */
public class DomainRuntimePresenter
        extends PerspectivePresenter<DomainRuntimePresenter.MyView, DomainRuntimePresenter.MyProxy>
        implements UnauthorizedEvent.UnauthorizedHandler {



    @ProxyCodeSplit
    @NameToken(NameTokens.DomainRuntimePresenter)
    @UseGatekeeper(DomainRuntimegateKeeper.class)
    public interface MyProxy extends Proxy<DomainRuntimePresenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(DomainRuntimePresenter presenter);
        void setSubsystems(List<SubsystemRecord> result);
        void updateServerList(List<Server> serverModel);
    }


    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent =
            new GwtEvent.Type<RevealContentHandler<?>>();


    private final Dispatcher circuit;
    private final ServerStore serverStore;
    private HandlerRegistration handlerRegistration;
    private final HostStore hostStore;
    private final PlaceManager placeManager;
    private final SubsystemLoader subsysStore;
    private final ServerGroupStore serverGroupStore;

    @Inject
    public DomainRuntimePresenter(EventBus eventBus, MyView view, MyProxy proxy, PlaceManager placeManager,
                                  HostStore hostStore, SubsystemLoader subsysStore,
                                  ServerGroupStore serverGroupStore, Header header, UnauthorisedPresenter unauthorisedPresenter,
                                  Dispatcher circuit, ServerStore serverStore) {

        super(eventBus, view, proxy, placeManager, header, NameTokens.DomainRuntimePresenter, unauthorisedPresenter,
                TYPE_MainContent);

        this.placeManager = placeManager;

        this.hostStore = hostStore;
        this.subsysStore = subsysStore;
        this.serverGroupStore = serverGroupStore;
        this.circuit = circuit;
        this.serverStore = serverStore;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);

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
                        ) {

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
            }
        });
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
        circuit.dispatch(new RefreshServer());
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

    public String getFilter() {
        return serverStore.getFilter();
    }
}
