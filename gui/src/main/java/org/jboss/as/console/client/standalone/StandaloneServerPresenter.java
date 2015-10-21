package org.jboss.as.console.client.standalone;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.runtime.ext.Extension;
import org.jboss.as.console.client.shared.runtime.ext.ExtensionManager;
import org.jboss.as.console.client.shared.runtime.ext.LoadExtensionCmd;
import org.jboss.as.console.client.shared.schedule.LongRunningTask;
import org.jboss.as.console.client.shared.state.ReloadEvent;
import org.jboss.as.console.client.shared.state.ReloadState;
import org.jboss.as.console.client.standalone.runtime.StandaloneRuntimePresenter;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.AsyncCommand;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @deprecated to be removed
 * @author Heiko Braun
 * @date 6/7/11
 */
public class StandaloneServerPresenter extends Presenter<StandaloneServerPresenter.MyView, StandaloneServerPresenter.MyProxy> implements ExtensionManager {

    private final PlaceManager placeManager;
    private DispatchAsync dispatcher;
    private BeanFactory factory;
    private ReloadState reloadState;
    private BootstrapContext bootstrap;
    private LoadExtensionCmd loadExtensionCmd;

    @ProxyCodeSplit
    @NameToken(NameTokens.StandaloneServerPresenter)
    @NoGatekeeper
    public interface MyProxy extends Proxy<StandaloneServerPresenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(StandaloneServerPresenter presenter);
        void updateFrom(StandaloneServer server);
        void setReloadRequired(ReloadState reloadState);
        void setExtensions(List<Extension> extensions);
    }

    @Inject
    public StandaloneServerPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            PlaceManager placeManager, DispatchAsync dispatcher, BeanFactory factory,
            ReloadState reloadState, BootstrapContext bootstrap) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.dispatcher = dispatcher;
        this.factory = factory;
        this.reloadState = reloadState;
        this.bootstrap = bootstrap;
        this.loadExtensionCmd = new LoadExtensionCmd(dispatcher, factory);
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    private void loadConfig()
    {
        ModelNode serverConfig = new ModelNode();
        serverConfig.get(OP).set(READ_RESOURCE_OPERATION);
        serverConfig.get(INCLUDE_RUNTIME).set(true);
        serverConfig.get(ADDRESS).setEmptyList();

        dispatcher.execute(new DMRAction(serverConfig), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                StandaloneServer server = factory.standaloneServer().as();
                ModelNode serverAttributes = response.get(RESULT).asObject();
                server.setName(serverAttributes.get("name").asString());
                server.setReleaseCodename(serverAttributes.get("release-codename").asString());
                server.setReleaseVersion(bootstrap.getProductVersion());
                server.setServerState(serverAttributes.get("server-state").asString());
                getView().updateFrom(server);
                getView().setReloadRequired(reloadState);
            }
        });

    }

    @Override
    protected void onReset() {
        super.onReset();

        /*Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                getEventBus().fireEvent(
                        new LHSHighlightEvent(null, "Configuration", "standalone-runtime")

                );
            }
        }); */

        loadConfig();
        loadExtensions();

        getView().setReloadRequired(reloadState);
    }

    @Override
    protected void revealInParent() {

        RevealContentEvent.fire(this, StandaloneRuntimePresenter.TYPE_MainContent, this);
    }

    public void onReloadServerConfig() {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("reload");
        operation.get(ADDRESS).setEmptyList();

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if(response.isFailure())
                {
                    Console.error(Console.MESSAGES.failed("Reload Server"), response.getFailureDescription());
                }
                else
                {
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
                System.out.println(response);

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
                        getView().setReloadRequired(reloadState);
                        getEventBus().fireEvent(new ReloadEvent());
                    }

                    callback.onSuccess(keepRunning);
                }
            }
        });
    }

    @Override
    public void loadExtensions()
    {
        loadExtensionCmd.execute(new SimpleCallback<List<Extension>>() {
            @Override
            public void onSuccess(List<Extension> extensions) {
                getView().setExtensions(extensions);
            }
        });
    }

    public void onDumpVersions() {

        loadExtensionCmd.dumpVersions(new SimpleCallback<String>() {
            @Override
            public void onSuccess(String s) {
                showVersionInfo(s);
            }
        });

    }

    private void showVersionInfo(String json)
    {
        DefaultWindow window = new DefaultWindow("Management Model Versions");
        window.setWidth(480);
        window.setHeight(360);
        window.addCloseHandler(new CloseHandler<PopupPanel>() {
            @Override
            public void onClose(CloseEvent<PopupPanel> event) {

            }
        });

        TextArea textArea = new TextArea();
        textArea.setStyleName("fill-layout");
        textArea.setText(json);

        window.setWidget(textArea);

        window.setGlassEnabled(true);
        window.center();
    }

}
