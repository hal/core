package org.jboss.as.console.client.shared.subsys.mail;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.ComboBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.PasswordBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @date 11/28/11
 */
public class NewMailServerWizard {
    private MailPresenter presenter;
    private final MailSession selectedSession;

    public NewMailServerWizard(final MailPresenter presenter, final MailSession selectedSession) {
        this.presenter = presenter;
        this.selectedSession = selectedSession;
    }

    Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");

        TextBoxItem socket = new TextBoxItem("socketBinding", "Socket Binding");
        TextBoxItem user = new TextBoxItem("username", "Username");
        PasswordBoxItem pass = new PasswordBoxItem("password", "Password");
        CheckBoxItem ssl = new CheckBoxItem("ssl", "Use SSL?");

        final MailServerTypeItem type = new MailServerTypeItem();
        type.setValueMap(new String[]{
                ServerType.smtp.name(),
                ServerType.imap.name(),
                ServerType.pop3.name()
        });
        type.setDefaultToFirstOption(true);

        final Form<MailServerDefinition> form = new Form<MailServerDefinition>(MailServerDefinition.class);
        form.setFields(socket, type, user, pass, ssl);

        DialogueOptions options = new DialogueOptions(
                // save
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        // merge base
                        FormValidation validation = form.validate();
                        if(validation.hasErrors())
                            return;

                        MailServerDefinition entity = form.getUpdatedEntity();
                        entity.setType(ServerType.valueOf(type.getValue()));
                        presenter.onCreateServer(entity);
                    }
                },

                // cancel
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        presenter.closeDialoge();
                    }
                }
        );

        // ----------------------------------------

        Widget formWidget = form.asWidget();

        final FormHelpPanel helpPanel = new FormHelpPanel(
                new FormHelpPanel.AddressCallback() {
                    @Override
                    public ModelNode getAddress() {
                        ModelNode address = Baseadress.get();
                        address.add("subsystem", "mail");
                        address.add("mail-session", "*");
                        address.add("server", "smtp");
                        return address;
                    }
                }, form
        );

        layout.add(helpPanel.asWidget());
        layout.add(formWidget);
        return new WindowContentBuilder(layout, options).build();
    }

    private class MailServerTypeItem extends ComboBoxItem {

        private final String defaultErrMessage;

        public MailServerTypeItem() {
            super("type", "Type");
            this.defaultErrMessage = getErrMessage();
        }

        @Override
        public boolean validate(final String value) {
            boolean duplicateType = false;
            boolean parentValid = super.validate(value);
            if (parentValid) {
                duplicateType = sessionsContains(value);
                if (duplicateType) {
                    setErrMessage(Console.CONSTANTS.duplicate_mail_server_type());
                }
            } else {
                setErrMessage(defaultErrMessage);
            }
            return parentValid && !duplicateType;
        }

        boolean sessionsContains(String typeValue) {
            ServerType serverType = null;
            if (selectedSession.getImapServer() != null) {
                serverType = selectedSession.getImapServer().getType();
            } else if (selectedSession.getPopServer() != null) {
                serverType = selectedSession.getPopServer().getType();
            } else if (selectedSession.getSmtpServer() != null) {
                serverType = selectedSession.getSmtpServer().getType();
            }
            return serverType != null && serverType.name().equals(typeValue);
        }
    }
}
