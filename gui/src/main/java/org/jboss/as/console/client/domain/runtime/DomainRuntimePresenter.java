package org.jboss.as.console.client.domain.runtime;

import java.util.Collections;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
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
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.shared.model.SubsystemStore;
import org.jboss.as.console.client.shared.state.DomainEntityManager;
import org.jboss.as.console.client.shared.state.HostList;
import org.jboss.as.console.client.shared.state.HostSelectionChanged;
import org.jboss.as.console.client.shared.state.ServerSelectionChanged;
import org.jboss.ballroom.client.layout.LHSHighlightEvent;

/**
 * @author Heiko Braun
 */
public class DomainRuntimePresenter extends Presenter<DomainRuntimePresenter.MyView, DomainRuntimePresenter.MyProxy>
        implements StaleModelEvent.StaleModelListener,
        ServerSelectionChanged.ChangeListener,
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

    private final PlaceManager placeManager;
    private final UnauthorisedPresenter unauthorisedPresenter;
    private final DomainEntityManager domainManager;
    private HostInformationStore hostInfoStore;
    private SubsystemStore subsysStore;
    private ServerGroupStore serverGroupStore;
    private Header header;

    @Inject
    public DomainRuntimePresenter(EventBus eventBus, MyView view, MyProxy proxy, PlaceManager placeManager,
            HostInformationStore hostInfoStore, DomainEntityManager domainManager, SubsystemStore subsysStore,
            ServerGroupStore serverGroupStore, Header header, UnauthorisedPresenter unauthorisedPresenter) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.hostInfoStore = hostInfoStore;
        this.domainManager = domainManager;
        this.subsysStore = subsysStore;
        this.serverGroupStore = serverGroupStore;
        this.header = header;
        this.unauthorisedPresenter = unauthorisedPresenter;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);

        getEventBus().addHandler(HostSelectionChanged.TYPE, this);
        getEventBus().addHandler(ServerSelectionChanged.TYPE, this);
        getEventBus().addHandler(StaleModelEvent.TYPE, this);
        getEventBus().addHandler(UnauthorizedEvent.TYPE, this);
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
    protected void onReset() {
        super.onReset();
        header.highlight(NameTokens.DomainRuntimePresenter);

        String currentToken = placeManager.getCurrentPlaceRequest().getNameToken();
        if (currentToken.equals(getProxy().getNameToken())) {
            placeManager
                    .revealPlace(new PlaceRequest.Builder().nameToken(NameTokens.Topology).build());
        }
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
        if (StaleModelEvent.SERVER_INSTANCES.equals(modelName)
                || StaleModelEvent.SERVER_CONFIGURATIONS.equals(modelName)) {

            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    loadHostData();
                }
            });
        }
    }

    @Override
    public void onUnauthorized(final UnauthorizedEvent event) {
        setInSlot(TYPE_MainContent, unauthorisedPresenter);
    }

    private void loadSubsystems() {
        // clear view
        getView().setSubsystems(Collections.<SubsystemRecord>emptyList());

        // load subsystems for selected server
        hostInfoStore.getServerConfiguration(
                domainManager.getSelectedHost(), domainManager.getSelectedServer(),
                new SimpleCallback<Server>() {
                    @Override
                    public void onSuccess(Server server) {
                        serverGroupStore.loadServerGroup(server.getGroup(),
                                new SimpleCallback<ServerGroupRecord>() {
                                    @Override
                                    public void onSuccess(ServerGroupRecord group) {
                                        subsysStore.loadSubsystems(group.getProfileName(),
                                                new SimpleCallback<List<SubsystemRecord>>() {
                                                    @Override
                                                    public void onSuccess(List<SubsystemRecord> result) {
                                                        getView().setSubsystems(result);
                                                        Scheduler.get()
                                                                .scheduleDeferred(new Scheduler.ScheduledCommand() {
                                                                    @Override
                                                                    public void execute() {
                                                                        Console.getEventBus().fireEvent(
                                                                                new LHSHighlightEvent(
                                                                                        placeManager
                                                                                                .getCurrentPlaceRequest()
                                                                                                .getNameToken()
                                                                                )
                                                                        );
                                                                    }
                                                                });
                                                    }
                                                });
                                    }
                                });
                    }
                }
        );
    }
}
