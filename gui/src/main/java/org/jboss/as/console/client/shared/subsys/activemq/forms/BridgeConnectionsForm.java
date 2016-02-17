package org.jboss.as.console.client.shared.subsys.activemq.forms;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.layout.FormLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.activemq.connections.MsgConnectionsPresenter;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqBridge;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.ballroom.client.widgets.forms.PasswordBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @date 4/3/12
 */
public class BridgeConnectionsForm {

    private Form<ActivemqBridge> form = new Form<>(ActivemqBridge.class);
    private final MsgConnectionsPresenter presenter;
    private FormToolStrip.FormCallback<ActivemqBridge> callback;
    private boolean provideTools = true;

    public BridgeConnectionsForm(final MsgConnectionsPresenter presenter,
            FormToolStrip.FormCallback<ActivemqBridge> callback) {
        this.presenter = presenter;
        this.callback = callback;
        form.setNumColumns(2);
    }

    public Widget asWidget() {
        NumberBoxItem retry = new NumberBoxItem("retryInterval", "Retry Interval");
        NumberBoxItem reconnect = new NumberBoxItem("reconnectAttempts", "Reconnect Attempts", -1, Integer.MAX_VALUE);
        TextBoxItem user = new TextBoxItem("user", "User", false);
        PasswordBoxItem pass = new PasswordBoxItem("password", "Password", false);

        form.setFields(
                user, pass,
                retry, reconnect
        );

        FormHelpPanel helpPanel = new FormHelpPanel(() -> {
            ModelNode address = Baseadress.get();
            address.add("subsystem", "messaging-activemq");
            address.add("server", presenter.getCurrentServer());
            address.add("bridge", "*");
            return address;
        }, form);

        FormLayout formLayout = new FormLayout()
                .setForm(form)
                .setHelp(helpPanel);

        if (provideTools) {
            FormToolStrip<ActivemqBridge> formTools = new FormToolStrip<>(form, callback);
            formLayout.setTools(formTools);
        }

        return formLayout.build();
    }

    public Form<ActivemqBridge> getForm() {
        return form;
    }
}
