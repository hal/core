package org.jboss.as.console.client.shared.subsys.mail;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.BeanFactory;
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
    private final BeanFactory beanFactory;

    public NewMailServerWizard(final MailPresenter presenter, final MailSession selectedSession, BeanFactory beanFactory) {
        this.presenter = presenter;
        this.selectedSession = selectedSession;
        this.beanFactory = beanFactory;
    }

    Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");

        TextBoxItem socket = new TextBoxItem("socketBinding", "Socket Binding");
        TextBoxItem user = new TextBoxItem("username", "Username", false);
        PasswordBoxItem pass = new PasswordBoxItem("password", "Password", false);
        CheckBoxItem ssl = new CheckBoxItem("ssl", "Use SSL?");

        final MailServerTypeItem type = new MailServerTypeItem();
        type.setValueMap(new String[]{
                ServerType.smtp.name(),
                ServerType.imap.name(),
                ServerType.pop3.name()
        });
        type.setDefaultToFirstOption(true);

        final Form<MailServerDefinition> form = new Form<MailServerDefinition>(MailServerDefinition.class);
        form.editTransient(beanFactory.mailServerDefinition().as());
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
                        presenter.onCreateServer("TODO", entity); // TODO fix me
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

        boolean sessionsContains(String value) {
            boolean matched = match(selectedSession.getImapServer(), value);
            if (!matched) {
                matched = match(selectedSession.getPopServer(), value);
            }
            if (!matched) {
                matched = match(selectedSession.getSmtpServer(), value);
            }
            return matched;
        }

        boolean match(MailServerDefinition msd, String value) {
            return msd != null && msd.getType() != null && msd.getType().name().equals(value);
        }
    }
}
