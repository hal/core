package org.jboss.as.console.client.shared.subsys.mail;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.MultiViewImpl;

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
                Console.CONSTANTS.subsys_mail_server_desc(),
                presenter);

        register("server", serverConfigEditor.asWidget());

    }

    @Override
    public void setPresenter(MailPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void updateFrom(MailSession session) {
        serverConfigEditor.updateFrom(session);
    }
}
