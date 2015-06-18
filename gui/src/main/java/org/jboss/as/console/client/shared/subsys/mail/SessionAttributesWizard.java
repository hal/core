package org.jboss.as.console.client.shared.subsys.mail;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @since 18/06/15
 */
public class SessionAttributesWizard {

    MailSession mailSession;
    MailFinder presenter;
    private Form<MailSession> form;

    public SessionAttributesWizard(MailFinder presenter) {
        this.presenter = presenter;
    }

    Widget asWidget() {

        VerticalPanel layout = new VerticalPanel();
               layout.setStyleName("window-content");




        form = new Form<MailSession>(MailSession.class);
        form.setNumColumns(2);

        TextItem name= new TextItem("name", "Name");
        TextBoxItem jndi = new TextBoxItem("jndiName", "JNDI Name");
        CheckBoxItem debug = new CheckBoxItem("debug", "Debug Enabled?");
        TextBoxItem from = new TextBoxItem("from", "Default From", false);

        form.setFields(name, jndi, debug, from);


        FormHelpPanel helpPanel = new FormHelpPanel(new FormHelpPanel.AddressCallback() {
            @Override
            public ModelNode getAddress() {
                ModelNode address = Baseadress.get();
                address.add("subsystem", "mail");
                address.add("mail-session", "*");
                return address;
            }
        }, form);

        DialogueOptions options = new DialogueOptions(

                // save
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        FormValidation validation = form.validate();
                        if(validation.hasErrors())
                            return;

                        presenter.onSave(mailSession, form.getChangedValues());

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

        layout.add(helpPanel.asWidget());
        layout.add(formWidget);

        return new WindowContentBuilder(layout, options).build();
    }

    public void updateFrom(MailSession session) {
        this.mailSession = session;
        form.edit(session);
    }
}
