package org.jboss.as.console.client.core.bootstrap.hal;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.HTML;
import com.google.web.bindery.event.shared.EventBus;
import org.jboss.as.console.client.core.LogoutCmd;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

/**
 * @author Heiko Braun
 * @date 9/17/13
 */
public class InsufficientPrivileges implements ScheduledCommand {

    @Override
    public void execute() {
        final DefaultWindow window = new DefaultWindow("Access Denied");
        window.setWidth(320);
        window.setHeight(240);
        HTML message = new HTML("Insufficient privileges to access this interface.");

        DialogueOptions options = new DialogueOptions(
                "Logout",
                event -> {
                    window.hide();
                    new LogoutCmd().execute();
                },
                "Cancel",
                event -> {}
        );
        options.showCancel(false);

        window.trapWidget(new WindowContentBuilder(message, options).build());
        window.setGlassEnabled(true);
        window.center();
    }
}
