package org.jboss.as.console.client.shared.subsys.mail;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.DisposableViewImpl;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.ballroom.client.widgets.tabs.FakeTabPanel;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 11/28/11
 */
public class MailSubsystemView extends DisposableViewImpl implements MailPresenter.MyView{

    private MailPresenter presenter;
    private PagedView panel;
    private MailSessionEditor sessionEditor;
    private List<MailSession> sessions;
    private ServerConfigView serverConfigEditor;

    @Override
    public Widget createWidget() {

        DefaultTabLayoutPanel layout  = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        layout.addStyleName("default-tabpanel");

        panel = new PagedView();

        sessionEditor = new MailSessionEditor(presenter);
        serverConfigEditor = new ServerConfigView(
                Console.MESSAGES.available("Mail Server"),
                Console.CONSTANTS.subsys_mail_server_desc(),
                presenter);

        panel.addPage(Console.CONSTANTS.common_label_back(), sessionEditor.asWidget());
        panel.addPage("Mail Server", serverConfigEditor.asWidget());
        //panel.addPage("JMS Destinations", jmsEditor.asWidget()) ;

        // default page
        panel.showPage(0);

        Widget panelWidget = panel.asWidget();
        layout.add(panelWidget, "Mail");

        layout.selectTab(0);

        return layout;
    }

    @Override
    public void setPresenter(MailPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setSelectedSession(String selectedSession) {
        if (null == selectedSession) {
            panel.showPage(0);
        } else {
            for (MailSession session : sessions) {
                if (session.getName().equals(selectedSession)) {
                    // update subpages
                    serverConfigEditor.setServerConfig(session);
                    break;
                }
            }
            // move to first page if still showing topology
            if (0 == panel.getPage()) { panel.showPage(1); }
        }
    }

    @Override
    public void updateFrom(List<MailSession> list) {
        this.sessions = list;
        sessionEditor.updateFrom(list);
    }
}
