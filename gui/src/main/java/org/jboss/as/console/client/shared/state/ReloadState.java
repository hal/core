package org.jboss.as.console.client.shared.state;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.core.message.Message;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshServer;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.TrappedFocusPanel;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Heiko Braun
 * @date 12/14/11
 */
@Singleton
public class ReloadState {

    private Map<String, ServerState> serverStates = new HashMap<String, ServerState>();
    private int lastFiredSize = 0;

    public boolean isStaleModel() {
        return serverStates.size()>0;
    }

    public void reset() {
        serverStates.clear();
        lastFiredSize = 0;
    }

    public void propagateChanges() {
        if(isStaleModel() && lastFiredSize<serverStates.size())
        {
            lastFiredSize = serverStates.size();

            StringBuffer sb = new StringBuffer();
            ServerState serverState = serverStates.values().iterator().next();
            String message = serverState.isReloadRequired() ?
                    Console.CONSTANTS.server_instance_servers_needReload() :
                    Console.CONSTANTS.server_instance_servers_needRestart();

            sb.append(message).append("\n\n");

            boolean restartRequired = false;
            for(ServerState server : serverStates.values())
            {
                if(server.isRestartRequired())
                    restartRequired = true;

                sb.append("  - ");
                sb.append(server.getName());
                sb.append("\n");
            }

            // state update, log warning
            //Console.warning(Console.CONSTANTS.server_configuration_changed(), sb.toString(), true);


            Message msg = new Message(Console.CONSTANTS.serverConfigurationChanged(), sb.toString(), Message.Severity.Warning);
            showDetail(msg, restartRequired, Console.MODULES.getBootstrapContext().isStandalone());

            if(Console.MODULES.getBootstrapContext().isStandalone())
            {
                Console.MODULES.getEventBus().fireEvent(new StandaloneRuntimeRefresh());
            }
            else
            {
                Console.MODULES.getCircuitDispatcher().dispatch(new RefreshServer());
            }
        }
    }

    private void showDetail(final Message msg, boolean restartRequired, boolean isStandalone) {

        msg.setNew(false);

        final DefaultWindow window = new DefaultWindow(Console.CONSTANTS.common_label_messageDetailTitle());

        window.setWidth(480);
        window.setHeight(360);
        window.setGlassEnabled(true);

        SafeHtmlBuilder html = new SafeHtmlBuilder();

        String style = "list-"+msg.getSeverity().getStyle();

        // TODO: XSS prevention?
        html.appendHtmlConstant(msg.getSeverity().getTag());
        html.appendHtmlConstant("&nbsp;");
        html.appendHtmlConstant(msg.getFired().toString());
        html.appendHtmlConstant("<h3 id='consise-message' class='"+style+"' style='padding:10px;box-shadow:none!important;border-width:5px'>");
        html.appendHtmlConstant(msg.getConciseMessage());
        html.appendHtmlConstant("</h3>");
        html.appendHtmlConstant("<p/>");

        String detail = msg.getDetailedMessage() != null ? msg.getDetailedMessage() : "";

        html.appendHtmlConstant("<pre style='font-family:tahoma, verdana, sans-serif;' id='detail-message'>");
        html.appendEscaped(detail);
        html.appendHtmlConstant("</pre>");

        final HTML widget = new HTML(html.toSafeHtml());
        widget.getElement().setAttribute("style", "margin:5px");

        ClickHandler reloadHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                if (Console.MODULES.getBootstrapContext().isStandalone()) {
                    window.hide();
                    Scheduler.get().scheduleDeferred(() -> {
                        Console.MODULES.getPlaceManager().revealPlace(new PlaceRequest(NameTokens.StandaloneRuntimePresenter));
                    });

                } else {
                    window.hide();
                    Scheduler.get().scheduleDeferred(() -> {
                        Console.MODULES.getPlaceManager().revealPlace(new PlaceRequest(NameTokens.HostMgmtPresenter));
                    });

                }
            }
        };

        ClickHandler restartHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                if (Console.MODULES.getBootstrapContext().isStandalone()) {
                    window.hide();
                    Scheduler.get().scheduleDeferred(() -> {
                        Console.MODULES.getPlaceManager().revealPlace(new PlaceRequest(NameTokens.StandaloneRuntimePresenter));
                    });

                } else {
                    window.hide();
                    Scheduler.get().scheduleDeferred(() -> {
                        Console.MODULES.getPlaceManager().revealPlace(new PlaceRequest(NameTokens.HostMgmtPresenter));
                    });

                }
            }
        };

        DialogueOptions options = null;

        final ClickHandler dismissHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                window.hide();
            }
        };

        if(restartRequired && isStandalone)
        {
            // standalone servers cannot be restarted from the UI
            options = new DialogueOptions(
                    Console.CONSTANTS.dismiss(),
                    dismissHandler,
                    "",
                    new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent clickEvent) {
                            // not used
                        }
                    }

            ).showCancel(false);
        }
        else if(restartRequired && !isStandalone)
        {
            // domain mode restart
            options = new DialogueOptions(
                    "Restart servers now!",
                    restartHandler,
                    Console.CONSTANTS.dismiss(),
                    dismissHandler
            );
        }
        else if(!restartRequired) // reload required
        {
            options = new DialogueOptions(
                    Console.CONSTANTS.reloadServerNow(),
                    reloadHandler,
                    Console.CONSTANTS.dismiss(),
                    dismissHandler
            );
        }

        options.getSubmit().setAttribute("aria-describedby", "consise-message detail-message");

        Widget windowContent = new WindowContentBuilder(widget, options).build();

        TrappedFocusPanel trap = new TrappedFocusPanel(windowContent)
        {
            @Override
            protected void onAttach() {
                super.onAttach();

                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        getFocus().onFirstButton();
                    }
                });
            }
        };

        window.setWidget(trap);

        window.center();
    }

    public void updateFrom(Map<String, ServerState> states) {
        this.serverStates = states;
    }

    public Map<String, ServerState> getServerStates() {
        return serverStates;
    }
}
