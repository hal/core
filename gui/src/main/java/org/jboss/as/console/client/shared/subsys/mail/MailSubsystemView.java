package org.jboss.as.console.client.shared.subsys.mail;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.DisposableViewImpl;
import org.jboss.as.console.client.core.MultiViewImpl;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.ballroom.client.widgets.tabs.FakeTabPanel;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 11/28/11
 */
public class MailSubsystemView extends MultiViewImpl implements MailPresenter.MyView{

    private MailPresenter presenter;
    private ServerConfigView serverConfigEditor;


    @Override
    public void createWidget() {

        serverConfigEditor = new ServerConfigView(
                Console.MESSAGES.available("Mail Server"),
                Console.CONSTANTS.subsys_mail_server_desc(),
                presenter);

        register("server", serverConfigEditor.asWidget());

    }

    @Override
    public void setPresenter(MailPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void updateFrom(String name, List<MailServerDefinition> list) {
        serverConfigEditor.setServerConfigs(name, list);
    }
}
