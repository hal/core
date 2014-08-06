package org.jboss.as.console.client.tools;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.rbac.SecurityContextImpl;
import org.jboss.ballroom.client.rbac.SecurityContext;

/**
 * @author Heiko Braun
 * @since 06/08/14
 */
public class SecurityView {

    private VerticalPanel layout;

    Widget asWidget() {
        layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width");

        return layout;
    }

    public void display(SecurityContext securityContext)
    {
        layout.clear();
        if(securityContext instanceof SecurityContextImpl)
        {
            layout.add(new ScrollPanel(new HTML(((SecurityContextImpl)securityContext).asHtml())));
        }
        else
        {
            layout.add(new HTML("Are you using the read-only context?"));
        }
    }
}
