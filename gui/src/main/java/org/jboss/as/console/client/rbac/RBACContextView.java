package org.jboss.as.console.client.rbac;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

/**
 * @author Heiko Braun
 * @date 8/7/13
 */
public class RBACContextView {
    public static void launch() {
        final DefaultWindow window = new DefaultWindow("RBAC Diagnostics");


        ClickHandler clickHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                window.hide();
            }
        };
        window.setWidget(
                new WindowContentBuilder(createContent(), new DialogueOptions(
                        "Done",clickHandler, "Cancel",clickHandler)
                ).build()
        );

        window.setWidth(480);
        window.setHeight(360);
        window.center();
    }

    private static Widget createContent() {

        SecurityContext securityContext = Console.MODULES.getSecurityService().getSecurityContext(
                Console.MODULES.getPlaceManager().getCurrentPlaceRequest().getNameToken()
        );

        return new HTML(securityContext.asHtml());
    }
}
