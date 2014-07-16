package org.jboss.as.console.client.domain.runtime;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.as.console.client.shared.homepage.ContentBox;
import org.jboss.as.console.client.tools.UUID;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;

/**
 * @author Heiko Braun
 * @since 16/07/14
 */
public class NoServerView extends SuspendableViewImpl implements NoServerPresenter.MyView {


    private ContentHeaderLabel header;

    @Override
    public Widget createWidget() {

        header = new ContentHeaderLabel();


        SafeHtmlBuilder createServerDesc = new SafeHtmlBuilder();
        createServerDesc.appendEscaped("A server configuration does specify the overall configuration of a server. A server configuration can be started and perform work.").appendHtmlConstant("<p/>");

        ContentBox createServer = new ContentBox(
                UUID.uuid(), "Create Server",
                createServerDesc.toSafeHtml(),
                "Server Configurations", NameTokens.ServerPresenter
        );

        SimpleLayout layout = new SimpleLayout()
                .setHeadlineWidget(header)
                .setPlain(true)
                .setDescription("It seems there is no server configured for this host.")
                .addContent("", createServer);

        return layout.build();
    }


    @Override
    public void setHostName(String selectedHost) {
        header.setText("No server configured for host '"+selectedHost+"'");
    }
}
