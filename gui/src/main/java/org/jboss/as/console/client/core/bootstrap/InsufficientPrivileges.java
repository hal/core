package org.jboss.as.console.client.core.bootstrap;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HTML;
import org.jboss.as.console.client.core.LogoutCmd;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

/**
 * @author Heiko Braun
 * @date 9/17/13
 */
public class InsufficientPrivileges implements Command {

    @Override
    public void execute() {
        final DefaultWindow window = new DefaultWindow("Access Denied");
        window.setWidth(320);
        window.setHeight(240);
        HTML message = new HTML("Insufficient privileges to access this interface.");

        DialogueOptions options = new DialogueOptions(
                "Logout",
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        window.hide();
                        new LogoutCmd().execute();
                    }
                },
                "Cancel",
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {

                    }
                }
        );

        options.showCancel(false);

        window.trapWidget(new WindowContentBuilder(message, options).build());
        window.setGlassEnabled(true);
        window.center();
    }
}
