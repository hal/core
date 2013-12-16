package org.jboss.as.console.client.domain.runtime;

import java.util.Collections;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.UseGatekeeper;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.events.StaleModelEvent;
import org.jboss.as.console.client.domain.model.HostInformationStore;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.model.ServerGroupStore;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.HostManagementGatekeeper;
import org.jboss.as.console.client.rbac.UnauthorisedPresenter;
import org.jboss.as.console.client.rbac.UnauthorizedEvent;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.shared.model.SubsystemStore;
import org.jboss.as.console.client.shared.state.DomainEntityManager;
import org.jboss.as.console.client.shared.state.HostList;
import org.jboss.as.console.client.shared.state.HostSelectionChanged;
import org.jboss.as.console.client.shared.state.PerspectivePresenter;
import org.jboss.as.console.client.shared.state.ServerSelectionChanged;
import org.jboss.ballroom.client.layout.LHSHighlightEvent;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;
import org.jboss.gwt.flow.client.PushFlowCallback;

/**
 * @author Heiko Braun
 */
public class DomainRuntimePresenter
        extends PerspectivePresenter<DomainRuntimePresenter.MyView, DomainRuntimePresenter.MyProxy>
        implements StaleModelEvent.StaleModelListener, ServerSelectionChanged.ChangeListener,
        HostSelectionChanged.ChangeListener, UnauthorizedEvent.UnauthorizedHandler {

    @ProxyCodeSplit
    @NameToken(NameTokens.DomainRuntimePresenter)
    @UseGatekeeper(HostManagementGatekeeper.class)
    public interface MyProxy extends Proxy<DomainRuntimePresenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(DomainRuntimePresenter presenter);
        void setHosts(HostList hosts);
        void setSubsystems(List<SubsystemRecord> result);
    }

    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent =
            new GwtEvent.Type<RevealContentHandler<?>>();

    private final DomainEntityManager domainManager;
    private final PlaceManager placeManager;
    private final HostInformationStore hostInfoStore;
    private final SubsystemStore subsysStore;
    private final ServerGroupStore serverGroupStore;

    @Inject
    public DomainRuntimePresenter(EventBus eventBus, MyView view, MyProxy proxy, PlaceManager placeManager,
            HostInformationStore hostInfoStore, DomainEntityManager domainManager, SubsystemStore subsysStore,
            ServerGroupStore serverGroupStore, Header header, UnauthorisedPresenter unauthorisedPresenter) {

        super(eventBus, view, proxy, placeManager, header, NameTokens.DomainRuntimePresenter, unauthorisedPresenter,
                TYPE_MainContent);

        this.placeManager = placeManager;
        this.hostInfoStore = hostInfoStore;
        this.domainManager = domainManager;
        this.subsysStore = subsysStore;
        this.serverGroupStore = serverGroupStore;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);

        getEventBus().addHandler(HostSelectionChanged.TYPE, this);
        getEventBus().addHandler(ServerSelectionChanged.TYPE, this);
        getEventBus().addHandler(StaleModelEvent.TYPE, this);
    }

    @Override
    protected void onReveal() {
        super.onReveal();
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                loadHostData();
            }
        });
    }

    private void loadHostData() {
        // load host and server data
        domainManager.getHosts(new SimpleCallback<HostList>() {
            @Override
            public void onSuccess(final HostList hosts) {
                getView().setHosts(hosts);
            }
        });
    }

    @Override
    protected void onDefaultPlace(final PlaceManager placeManager) {
        placeManager
                .revealPlace(new PlaceRequest.Builder().nameToken(NameTokens.Topology).build());
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }

    @Override
    public void onServerSelectionChanged(boolean isRunning) {
        // we can ignore if the server is running, it only requires configuration data
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                loadSubsystems();
            }
        });
    }

    @Override
    public void onHostSelectionChanged() {
    }

    @Override
    public void onStaleModel(String modelName) {
        if (StaleModelEvent.SERVER_INSTANCES.equals(modelName) || StaleModelEvent.SERVER_CONFIGURATIONS
                .equals(modelName)) {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    loadHostData();
                }
            });
        }
    }

    private void loadSubsystems() {
        // clear view
        getView().setSubsystems(Collections.<SubsystemRecord>emptyList());

        Function<FunctionContext> f1 = new Function<FunctionContext>() {
            @Override
            public void execute(final Control<FunctionContext> control) {
                hostInfoStore.getServerConfiguration(domainManager.getSelectedHost(), domainManager.getSelectedServer(),
                        new PushFlowCallback<Server>(control));
            }
        };
        Function<FunctionContext> f2 = new Function<FunctionContext>() {
            @Override
            public void execute(final Control<FunctionContext> control) {
                final Server server = control.getContext().pop();
                serverGroupStore.loadServerGroup(server.getGroup(), new PushFlowCallback<ServerGroupRecord>(control));
            }
        };
        Function<FunctionContext> f3 = new Function<FunctionContext>() {
            @Override
            public void execute(final Control<FunctionContext> control) {
                ServerGroupRecord group = control.getContext().pop();
                subsysStore.loadSubsystems(group.getProfileName(),
                        new PushFlowCallback<List<SubsystemRecord>>(control));
            }
        };
        Outcome<FunctionContext> outcome = new Outcome<FunctionContext>() {
            @Override
            public void onFailure(final FunctionContext context) {
                // TODO i18n
                Console.error("Cannot load subsystems of selected server");
            }

            @Override
            public void onSuccess(final FunctionContext context) {
                List<SubsystemRecord> subsystems = context.pop();
                getView().setSubsystems(subsystems);
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        Console.getEventBus()
                                .fireEvent(new LHSHighlightEvent(placeManager.getCurrentPlaceRequest().getNameToken()));
                    }
                });
            }
        };

        // load subsystems for selected server
        new Async<FunctionContext>(Footer.PROGRESS_ELEMENT).waterfall(new FunctionContext(), outcome, f1, f2, f3);
    }
}
