package org.jboss.as.console.client.core.bootstrap.hal;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.HTML;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.LogoutCmd;
import org.jboss.as.console.client.core.UIConstants;
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
        final DefaultWindow window = new DefaultWindow(Console.CONSTANTS.accessDenied());
        window.setWidth(320);
        window.setHeight(240);
        HTML message = new HTML(Console.CONSTANTS.insufficientPrivileges());

        DialogueOptions options = new DialogueOptions(
                Console.CONSTANTS.common_label_logout(),
                event -> {
                    window.hide();
                    new LogoutCmd().execute();
                },
                Console.CONSTANTS.common_label_cancel(),
                event -> {}
        );
        options.showCancel(false);

        window.trapWidget(new WindowContentBuilder(message, options).build());
        window.setGlassEnabled(true);
        window.center();
    }
}
