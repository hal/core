package org.jboss.as.console.client.core;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.web.bindery.event.shared.EventBus;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.tools.SSOUtils;

/**
 * @author Heiko Braun
 * @date 2/13/12
 */
public class LogoutCmd implements Command {

    private final EventBus bus;
    private boolean ssoEnabled;

    public LogoutCmd(boolean ssoEnabled) {
        this.bus = Console.MODULES.getEventBus();
        this.ssoEnabled = ssoEnabled;
    }

    @Override
    public void execute() {

        bus.fireEvent(new LogoutEvent());

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                String logoutUrl = Console.getBootstrapContext().getLogoutUrl();
                if (ssoEnabled) {
                    logoutUrl = SSOUtils.getSsoLogoutUrl();
                }
                clearMsie(); 
                Window.Location.replace(logoutUrl);
            }
        });
    }

    public static native String clearMsie() /*-{
        try {
            document.execCommand('ClearAuthenticationCache');
        } catch (error) {}
    }-*/;
}
