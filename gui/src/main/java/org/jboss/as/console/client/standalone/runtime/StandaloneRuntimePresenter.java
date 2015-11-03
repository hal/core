package org.jboss.as.console.client.standalone.runtime;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.domain.model.SuspendState;
import org.jboss.as.console.client.shared.model.SubsystemLoader;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.shared.schedule.LongRunningTask;
import org.jboss.as.console.client.shared.state.ReloadEvent;
import org.jboss.as.console.client.shared.state.ReloadState;
import org.jboss.as.console.client.shared.state.StandaloneRuntimeRefresh;
import org.jboss.as.console.client.v3.presenter.Finder;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.FinderScrollEvent;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.AsyncCommand;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 */
public class StandaloneRuntimePresenter
        extends Presenter<StandaloneRuntimePresenter.MyView, StandaloneRuntimePresenter.MyProxy>
        implements Finder, PreviewEvent.Handler, FinderScrollEvent.Handler, StandaloneRuntimeRefresh.Handler{

    private final PlaceManager placeManager;
    private final SubsystemLoader subsysStore;
    private final Header header;
    private final ReloadState reloadState;
    private final DispatchAsync dispatcher;

    private boolean hasBeenLoaded;
    private DefaultWindow window;

    public void closeDialoge() {
         window.hide();
    }

    public void onLaunchSuspendDialogue() {
        window = new DefaultWindow("Suspend Server");
        window.setWidth(480);
        window.setHeight(360);
        window.trapWidget(new SuspendStandaloneDialogue(this).asWidget());
        window.setGlassEnabled(true);
        window.center();
    }


    @NoGatekeeper
    @ProxyCodeSplit
    @NameToken(NameTokens.StandaloneRuntimePresenter)
    @RequiredResources(resources = {"/"}, recursive = false)
    public interface MyProxy extends Proxy<StandaloneRuntimePresenter>, Place {}

    public interface MyView extends View {
        void setPresenter(StandaloneRuntimePresenter presenter);
        void setSubsystems(List<SubsystemRecord> result);
        void setPreview(final SafeHtml html);
        void toggleScrolling(boolean enforceScrolling, int requiredWidth);
        void updateServer(StandaloneServer standaloneServer);
    }

    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent = new GwtEvent.Type<RevealContentHandler<?>>();



    @Inject
    public StandaloneRuntimePresenter(EventBus eventBus, MyView view, MyProxy proxy, PlaceManager placeManager,
                                      SubsystemLoader subsysStore, Header header, ReloadState reloadState, DispatchAsync dispatcher) {

        super(eventBus, view, proxy);
        this.placeManager = placeManager;
        this.subsysStore = subsysStore;
        this.header = header;
        this.reloadState = reloadState;
        this.dispatcher = dispatcher;
    }

    @Override
    public FinderColumn.FinderId getFinderId() {
        return FinderColumn.FinderId.RUNTIME;
    }

    @Override
    public void onStaleModel() {
        loadServer();
    }

    @Override
    public void onPreview(PreviewEvent event) {
        if(isVisible())
            getView().setPreview(event.getHtml());
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        getEventBus().addHandler(PreviewEvent.TYPE, this);
        getEventBus().addHandler(FinderScrollEvent.TYPE, this);
        getEventBus().addHandler(StandaloneRuntimeRefresh.TYPE, this);
    }

    @Override
    protected void onReset() {

        header.highlight(getProxy().getNameToken());


        if(!hasBeenLoaded) {
            if(getProxy().getNameToken().equals(placeManager.getCurrentPlaceRequest().getNameToken()))
                loadServer();

            hasBeenLoaded = true;
        }

    }

    public void loadSubsystems() {
        subsysStore.loadSubsystems("default", new SimpleCallback<List<SubsystemRecord>>() {
            @Override
            public void onSuccess(List<SubsystemRecord> result) {
                getView().setSubsystems(result);
            }
        });
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }

    @Override
    public void onToggleScrolling(FinderScrollEvent event) {
        if(isVisible())
            getView().toggleScrolling(event.isEnforceScrolling(), event.getRequiredWidth());
    }

    public void onReloadServerConfig() {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("reload");
        operation.get(ADDRESS).setEmptyList();

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failed("Reload Server"), response.getFailureDescription());
                } else {
                    pollState();
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                Console.error(Console.MESSAGES.failed("Reload Server"), caught.getMessage());
            }
        });
    }

    private void pollState() {

        LongRunningTask poll = new LongRunningTask(new AsyncCommand<Boolean>() {
            @Override
            public void execute(final AsyncCallback<Boolean> callback) {
                checkReloadState(callback);
            }
        }, 10);

        // kick of the polling request
        poll.schedule(500);
    }

    /**
     * Simply query the process state attribute to get to the required headers
     */
    public void checkReloadState(final AsyncCallback<Boolean> callback) {

        // :read-attribute(name=process-type)
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("server-state");
        operation.get(ADDRESS).setEmptyList();

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {

                ModelNode response = result.get();

                if(response.isFailure()) {
                    callback.onFailure(new RuntimeException("Failed to poll server state"));
                }
                else
                {
                    // TODO: only works when this response changes the reload state
                    String outcome = response.get(RESULT).asString();
                    boolean keepRunning = !outcome.equalsIgnoreCase("running");//reloadState.isStaleModel();

                    if(!keepRunning)
                    {

                        // clear state
                        reloadState.reset();

                        Console.info(Console.MESSAGES.successful("Reload Server"));

                        // clear reload state
                        getEventBus().fireEvent(new ReloadEvent());

                        getView().updateServer(new StandaloneServer(false, SuspendState.UNKOWN));
                    }

                    callback.onSuccess(keepRunning);
                }
            }
        });
    }


    private void loadServer() {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);
        operation.get(ADDRESS).setEmptyList();

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {

                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(response.getFailureDescription());
                } else {
                    // TODO: only works when this response changes the reload state
                    ModelNode model = response.get(RESULT);

                    boolean isRunning = model.get("server-state").asString().equalsIgnoreCase("RUNNING");
                    StandaloneServer server = new StandaloneServer(
                            !isRunning,
                            SuspendState.valueOf(model.get("suspend-state").asString())
                    );

                    getView().updateServer(server);

                }
            }
        });
    }

    public void onResumeServer() {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("resume");
        operation.get(ADDRESS).setEmptyList();

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {

                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(response.getFailureDescription());
                } else {
                    Console.info("Successfully resumed server");
                    loadServer();
                }
            }
        });
    }

    public void onSuspendServer(Long value) {


        closeDialoge();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set("suspend");
        operation.get(ADDRESS).setEmptyList();
        operation.get("timeout").set(value);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {

                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(response.getFailureDescription());
                } else {
                    Console.info("Successfully suspended server");
                    loadServer();
                }
            }
        });
    }

}
