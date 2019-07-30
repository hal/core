package org.jboss.as.console.client.shared.subsys.activemq.forms;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.layout.FormLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.activemq.connections.MsgConnectionsPresenter;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnectionFactory;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextAreaItem;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @date 4/3/12
 */
public class CFConnectionsForm {

    private Form<ActivemqConnectionFactory> form;
    private final MsgConnectionsPresenter presenter;
    private FormToolStrip.FormCallback<ActivemqConnectionFactory> callback;

    public CFConnectionsForm(final MsgConnectionsPresenter presenter,
            FormToolStrip.FormCallback<ActivemqConnectionFactory> callback) {
        this.presenter = presenter;
        this.callback = callback;
    }

    public Widget asWidget() {
        form = new Form<>(ActivemqConnectionFactory.class);
        form.setNumColumns(2);

        NumberBoxItem callTimeout = new NumberBoxItem("callTimeout", "Call Timeout");
        NumberBoxItem connectionTTL = new NumberBoxItem("connectionTTL", "Connection TTL", -1, Integer.MAX_VALUE);
        NumberBoxItem maxRetryInterval = new NumberBoxItem("maxRetryInterval", "Max Retry");
        NumberBoxItem retryInterval = new NumberBoxItem("retryInterval", "Retry Interval");
        NumberBoxItem reconnect = new NumberBoxItem("reconnectAttempts", "Reconnect Attempts", -1, Integer.MAX_VALUE);
        TextAreaItem lbClass = new TextAreaItem("loadbalancingClassName", "Load Balacer Class");

        form.setFields(callTimeout, connectionTTL, retryInterval, maxRetryInterval, reconnect, lbClass);

        FormHelpPanel helpPanel = new FormHelpPanel(() -> {
            ModelNode address = Baseadress.get();
            address.add("subsystem", "messaging-activemq");
            address.add("server", presenter.getCurrentServer());
            address.add("connection-factory", "*");
            return address;
        }, form);

        FormToolStrip<ActivemqConnectionFactory> formTools = new FormToolStrip<>(form, callback);

        FormLayout formLayout = new FormLayout()
                .setTools(formTools)
                .setForm(form)
                .setHelp(helpPanel);

        return formLayout.build();
    }

    public Form<ActivemqConnectionFactory> getForm() {
        return form;
    }
}
