/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.console.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.*;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.DelayedBindRegistry;
import com.gwtplatform.mvp.client.proxy.AsyncCallFailEvent;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import org.jboss.as.console.client.core.*;
import org.jboss.as.console.client.core.bootstrap.*;
import org.jboss.as.console.client.core.bootstrap.server.BootstrapServerSetup;
import org.jboss.as.console.client.core.gin.Composite;
import org.jboss.as.console.client.core.message.Message;
import org.jboss.as.console.client.core.message.MessageCenter;
import org.jboss.as.console.client.plugins.RuntimeExtensionRegistry;
import org.jboss.as.console.client.plugins.SubsystemRegistry;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.Preferences;
import org.jboss.as.console.client.shared.help.HelpSystem;
import org.jboss.as.console.client.shared.state.ReloadNotification;
import org.jboss.as.console.client.shared.state.ReloadState;
import org.jboss.as.console.client.shared.state.ServerState;
import org.jboss.as.console.client.widgets.progress.ProgressPolyfill;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.dispatch.DispatchError;
import org.jboss.dmr.client.notify.Notifications;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Outcome;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

/**
 * Main application entry point. Executes several initialisation phases.
 *
 * @author Heiko Braun
 */
public class Console implements EntryPoint, ReloadNotification.Handler {

    public final static Composite MODULES = GWT.create(Composite.class);
    public final static UIConstants CONSTANTS = GWT.create(UIConstants.class);
    public final static UIDebugConstants DEBUG_CONSTANTS = GWT.create(UIDebugConstants.class);
    public final static UIMessages MESSAGES = GWT.create(UIMessages.class);
    public final static UITokens TOKENS = GWT.create(UITokens.class);
    public final static ProductConfig prodConfig = GWT.create(ProductConfig.class);

    public void onModuleLoad() {
        Log.setUncaughtExceptionHandler();
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                onModuleLoad2();
            }
        });
    }

    public void onModuleLoad2() {
        // register global code split call handler
        MODULES.getEventBus().addHandler(AsyncCallFailEvent.getType(), new AsyncCallHandler(MODULES.getPlaceManager()));

        // inject resources
        injectResources();

        // display the loading panel
        final Widget loadingPanel = new LoadingPanel().asWidget();
        RootLayoutPanel.get().add(loadingPanel);

        // start the GIN engine and kick off the bootstrap process
        GWT.runAsync(new RunAsyncCallback() {
            public void onFailure(Throwable caught) {
                Window.alert("Failed to load application components!");
            }

            public void onSuccess() {
                DelayedBindRegistry.bind(MODULES);

                // dump prefs
                for (Preferences.Key key : Preferences.Key.values()) {
                    String prefValue = Preferences.get(key) != null ? Preferences.get(key) : "n/a";
                    Log.info(key.getTitle() + ": " + prefValue);
                }

                // Create and activate an anonymous, empty and fully granted
                // security context which will be used during the bootstrap steps
                SecurityFramework securityFramework = MODULES.getSecurityFramework();
                securityFramework.createSecurityContext(null, Collections.<String>emptySet(), false,
                        new AsyncCallback<SecurityContext>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                bootstrap(loadingPanel);
                            }

                            @Override
                            public void onSuccess(SecurityContext result) {
                                bootstrap(loadingPanel);
                            }
                        });
            }
        });
    }

    private void injectResources() {
        ConsoleResources.INSTANCE.css().ensureInjected();
        if (prodConfig.getProfile().equals(ProductConfig.Profile.COMMUNITY)) {
            ConsoleResources.INSTANCE.communityStyles().ensureInjected();
        } else {
            ConsoleResources.INSTANCE.productStyles().ensureInjected();
        }
        ConsoleResources.INSTANCE.prettifyCss().ensureInjected();
        ProgressPolyfill.inject();
        ScriptInjector.fromString(ConsoleResources.INSTANCE.prettifyJs().getText()).setWindow(ScriptInjector.TOP_WINDOW)
                .inject();
        ScriptInjector.fromString(ConsoleResources.INSTANCE.lunrJs().getText()).setWindow(ScriptInjector.TOP_WINDOW)
                .inject();
        ScriptInjector.fromString(ConsoleResources.INSTANCE.mousetrapJs().getText()).setWindow(
                ScriptInjector.TOP_WINDOW).inject();
        ScriptInjector.fromString(ConsoleResources.INSTANCE.protovis().getText()).setWindow(ScriptInjector.TOP_WINDOW)
                .inject();
    }

    private void bootstrap(final Widget loadingPanel) {

        // Bootstrap outcome: Load main application or display error message
        Outcome<BootstrapContext> bootstrapOutcome = new Outcome<BootstrapContext>() {
            @Override
            public void onFailure(BootstrapContext context) {
                RootLayoutPanel.get().remove(loadingPanel);

                String cause = "";
                int status = 500;
                if (context.getLastError() != null) {
                    cause = context.getLastError().getMessage();
                    if (context.getLastError() instanceof DispatchError) {
                        status = ((DispatchError) context.getLastError()).getStatusCode();
                    }
                }

                if (403 == status) {
                    // authorisation error (lack of privileges)
                    new InsufficientPrivileges().execute();
                } else {
                    // unknown error
                    HTMLPanel explanation = new HTMLPanel("<div style='padding-top:150px;padding-left:120px;'><h2>The management interface could not be loaded.</h2><pre>" + cause + "</pre></div>");
                    RootLayoutPanel.get().add(explanation);
                }
            }

            @Override
            public void onSuccess(BootstrapContext context) {
                RootLayoutPanel.get().remove(loadingPanel);

                // DMR notifications
                Notifications.addReloadHandler(Console.this);

                StringBuilder title = new StringBuilder();
                title.append(context.getProductName()).append(" Management");
                if (context.getServerName() != null) {
                    title.append(" | ").append(context.getServerName());
                }
                Window.setTitle(title.toString());
                new LoadMainApp(
                        MODULES.getBootstrapContext(),
                        MODULES.getPlaceManager(),
                        MODULES.getTokenFormatter()).execute();
            }
        };

        // Ordered execution: if any of these fail, the interface will not be loaded
        new Async<BootstrapContext>().waterfall(
                MODULES.getBootstrapContext(), // shared context
                bootstrapOutcome, // outcome

                // bootstrap functions
                new BootstrapServerSetup(),
                new StoreAdapterInit(),
                new LoadGoogleViz(),
                new ExecutionMode(MODULES.getDispatchAsync()),
                new TrackExecutionMode(MODULES.getAnalytics()),
                new LoadCompatMatrix(MODULES.modelVersions()),
                new RegisterSubsystems(MODULES.getSubsystemRegistry()),
                new EagerLoadProfiles(MODULES.getProfileStore(), MODULES.getCurrentSelectedProfile()),
                new HostStoreInit(MODULES.getHostStore()),
                new ServerStoreInit(MODULES.getHostStore(), MODULES.getServerStore()),
                new EagerLoadGroups(MODULES.getServerGroupStore())
        );
    }

    public static void info(String message) {
        getMessageCenter().notify(new Message(message, Message.Severity.Info));
    }

    public static void error(String message) {
        getMessageCenter().notify(new Message(message, Message.Severity.Error));
    }

    public static void error(String message, String detail) {
        getMessageCenter().notify(new Message(message, detail, Message.Severity.Error));
    }

    public static void warning(String message) {
        getMessageCenter().notify(new Message(message, Message.Severity.Warning));
    }

    public static void warning(String message, boolean sticky) {
        Message msg = sticky ?
                new Message(message, Message.Severity.Warning, EnumSet.of(Message.Option.Sticky)) :
                new Message(message, Message.Severity.Warning);
        getMessageCenter().notify(msg);
    }

    public static void warning(String message, String detail, boolean sticky) {
        Message msg = sticky ?
                new Message(message, detail, Message.Severity.Warning, EnumSet.of(Message.Option.Sticky)) :
                new Message(message, detail, Message.Severity.Warning);
        getMessageCenter().notify(msg);
    }

    public static void warning(String message, String detail) {
        getMessageCenter().notify(new Message(message, detail, Message.Severity.Warning));
    }

    public static void schedule(final Command cmd) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                cmd.execute();
            }
        });
    }

    public static EventBus getEventBus() {
        return MODULES.getEventBus();
    }

    public static Dispatcher getCircuit() {
        return MODULES.getCircuitDispatcher();
    }

    @Deprecated
    public static MessageCenter getMessageCenter() {
        return MODULES.getMessageCenter();
    }

    public static PlaceManager getPlaceManager() {
        return MODULES.getPlaceManager();
    }

    public static BootstrapContext getBootstrapContext() {
        return MODULES.getBootstrapContext();
    }

    @Deprecated
    public static HelpSystem getHelpSystem() {
        return MODULES.getHelpSystem();
    }

    @Deprecated
    public static native boolean visAPILoaded() /*-{
        return false; // prevent usage of charts within metrics
    }-*/;

    public static SubsystemRegistry getSubsystemRegistry() {
        return MODULES.getSubsystemRegistry();
    }

    public static RuntimeExtensionRegistry getRuntimeLHSItemExtensionRegistry() {
        return MODULES.getRuntimeLHSItemExtensionRegistry();
    }

    public static boolean protovisAvailable() {
        /*String userAgent = Window.Navigator.getUserAgent();
        return !(userAgent.contains("MSIE") || userAgent.contains("msie"));*/
        return true;
    }

    @Override
    public void onReloadRequired(Map<String, ServerState> states) {
        ReloadState reloadState = Console.MODULES.getReloadState();
        reloadState.updateFrom(states);
        reloadState.propagateChanges();
    }
}
