package org.jboss.as.console.client.administration.audit;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import org.jboss.as.console.client.shared.help.StaticHelpPanel;

/**
 * @author Harald Pehl
 * @date 08/13/2013
 */
public class AuditHelpPanel extends StaticHelpPanel {

    final static Resources RESOURCES = GWT.create(Resources.class);

    public AuditHelpPanel() {
        super(new SafeHtmlBuilder().appendHtmlConstant(RESOURCES.help().getText()).toSafeHtml());
    }

    interface Resources extends ClientBundle {

        @Source("help.html")
        TextResource help();
    }
}