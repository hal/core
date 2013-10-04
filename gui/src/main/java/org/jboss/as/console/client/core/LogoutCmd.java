package org.jboss.as.console.client.core;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.web.bindery.event.shared.EventBus;
import org.jboss.as.console.client.Console;

/**
 * @author Heiko Braun
 * @date 2/13/12
 */
public class LogoutCmd implements Command {

    private EventBus bus;

    public LogoutCmd() {
        this.bus = Console.MODULES.getEventBus();
    }

    @Override
    public void execute() {

        bus.fireEvent(new LogoutEvent());

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                String logoutUrl = Console.getBootstrapContext().getLogoutUrl();
                clearMsie();
                Window.Location.replace(logoutUrl);
            }
        });
    }



    public static native String clearMsie() /*-{
        try {
            document.execCommand('ClearAuthenticationCache');
        } catch (error) {
        }

    }-*/;
}
