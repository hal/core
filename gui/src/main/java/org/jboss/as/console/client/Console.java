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
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.DelayedBindRegistry;
import com.gwtplatform.mvp.client.proxy.AsyncCallFailEvent;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import org.jboss.as.console.client.core.AsyncCallHandler;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.LoadingPanel;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.core.UIDebugConstants;
import org.jboss.as.console.client.core.UIMessages;
import org.jboss.as.console.client.core.UITokens;
import org.jboss.as.console.client.core.bootstrap.hal.InsufficientPrivileges;
import org.jboss.as.console.client.core.bootstrap.hal.LoadMainApp;
import org.jboss.as.console.client.core.gin.CompositeGinjector;
import org.jboss.as.console.client.core.message.Message;
import org.jboss.as.console.client.core.message.MessageCenter;
import org.jboss.as.console.client.plugins.RuntimeExtensionRegistry;
import org.jboss.as.console.client.plugins.SubsystemRegistry;
import org.jboss.as.console.client.shared.help.HelpSystem;
import org.jboss.as.console.client.shared.state.ReloadNotification;
import org.jboss.as.console.client.shared.state.ReloadState;
import org.jboss.as.console.client.shared.state.ServerState;
import org.jboss.as.console.spi.Entrypoint;
import org.jboss.dmr.client.dispatch.DispatchError;
import org.jboss.dmr.client.notify.Notifications;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.flow.client.Outcome;

import java.util.EnumSet;
import java.util.Map;

/**
 * Main application entry point. Executes several initialisation phases.
 *
 * @author Heiko Braun
 */
@Entrypoint
public class Console implements EntryPoint, ReloadNotification.Handler {

    public final static UIConstants CONSTANTS = GWT.create(UIConstants.class);
    public final static UIDebugConstants DEBUG_CONSTANTS = GWT.create(UIDebugConstants.class);
    public final static UIMessages MESSAGES = GWT.create(UIMessages.class);
    public final static UITokens TOKENS = GWT.create(UITokens.class);
    public final static CompositeGinjector MODULES = CompositeGinjector.INSTANCE;

    public void onModuleLoad() {
        LoadingPanel.get().on();
        Log.setUncaughtExceptionHandler();
        Scheduler.get().scheduleDeferred(this::bootstrap);
    }

    public void bootstrap() {
        GWT.runAsync(new RunAsyncCallback() {
            @Override
            public void onFailure(Throwable reason) {
                Window.alert("Failed to load application components!");
            }

            @Override
            public void onSuccess() {
                DelayedBindRegistry.bind(MODULES);
                MODULES.getEventBus().addHandler(AsyncCallFailEvent.getType(),
                        new AsyncCallHandler(MODULES.getPlaceManager()));
                MODULES.getBootstrapper().go(new BootstrapOutcome());
            }
        });
    }

    private class BootstrapOutcome implements Outcome<BootstrapContext> {
        @Override
        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        public void onFailure(BootstrapContext context) {
            LoadingPanel.get().off();

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
                HTMLPanel explanation = new HTMLPanel("<div style='padding-top:150px;padding-left:120px;'><h2>" + CONSTANTS
                        .unableToLoadConsole() + "</h2><pre>" + cause + "</pre></div>");
                RootLayoutPanel.get().add(explanation);
            }
        }

        @Override
        public void onSuccess(BootstrapContext context) {
            LoadingPanel.get().off();

            // DMR notifications
            Notifications.addReloadHandler(Console.this);

           /* StringBuilder title = new StringBuilder();
            title.append(context.getProductName()).append(" Management");
            if (context.getServerName() != null) {
                title.append(" | ").append(context.getServerName());
            }
            Window.setTitle(title.toString());*/

            ProductConfig productConfig = GWT.create(ProductConfig.class);
            new LoadMainApp(productConfig, context, MODULES.getPlaceManager(), MODULES.getTokenFormatter()).execute();
        }
    }


    // ------------------------------------------------------ reload handler

    @Override
    public void onReloadRequired(Map<String, ServerState> states) {
        ReloadState reloadState = MODULES.getReloadState();
        reloadState.updateFrom(states);
        reloadState.propagateChanges();
    }


    // ------------------------------------------------------ messages

    public static void message(String message) {
           getMessageCenter().notify(new Message(message, Message.Severity.Info));
       }

    public static void info(String message) {
        getMessageCenter().notify(new Message(message, Message.Severity.Confirmation));
    }

    public static void info(String message, String detail) {
        getMessageCenter().notify(new Message(message, detail, Message.Severity.Confirmation));
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


    // ------------------------------------------------------ TODO Remove / replace deprecated methods

    @Deprecated
    public static EventBus getEventBus() {
        return MODULES.getEventBus();
    }

    @Deprecated
    public static Dispatcher getCircuit() {
        return MODULES.getCircuitDispatcher();
    }

    @Deprecated
    public static MessageCenter getMessageCenter() {
        return MODULES.getMessageCenter();
    }

    @Deprecated
    public static PlaceManager getPlaceManager() {
        return MODULES.getPlaceManager();
    }

    @Deprecated
    public static BootstrapContext getBootstrapContext() {
        return MODULES.getBootstrapContext();
    }

    @Deprecated
    public static HelpSystem getHelpSystem() {
        return MODULES.getHelpSystem();
    }

    @Deprecated
    public static SubsystemRegistry getSubsystemRegistry() {
        return MODULES.getSubsystemRegistry();
    }

    @Deprecated
    public static RuntimeExtensionRegistry getRuntimeLHSItemExtensionRegistry() {
        return MODULES.getRuntimeLHSItemExtensionRegistry();
    }

    @Deprecated
    public static boolean protovisAvailable() {
        /*String userAgent = Window.Navigator.getUserAgent();
        return !(userAgent.contains("MSIE") || userAgent.contains("msie"));*/
        return true;
    }
}
