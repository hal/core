package org.jboss.as.console.client.shared.subsys.activemq.forms;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.layout.FormLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.activemq.cluster.MsgClusteringPresenter;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqClusterConnection;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @date 4/3/12
 */
public class ClusterConnectionForm {

    Form<ActivemqClusterConnection> form = new Form<>(ActivemqClusterConnection.class);
    boolean isCreate = false;
    private final MsgClusteringPresenter presenter;
    private FormToolStrip.FormCallback<ActivemqClusterConnection> callback;

    public ClusterConnectionForm(MsgClusteringPresenter presenter,
            FormToolStrip.FormCallback<ActivemqClusterConnection> callback) {
        this.presenter = presenter;
        this.callback = callback;
    }

    public ClusterConnectionForm(MsgClusteringPresenter presenter,
            FormToolStrip.FormCallback<ActivemqClusterConnection> callback, boolean create) {
        this.presenter = presenter;
        isCreate = create;
        if (!isCreate) { this.callback = callback; }
    }

    public Widget asWidget() {
        buildForm();

        if (isCreate) {
            form.setNumColumns(1);
        } else {
            form.setNumColumns(2);
            form.setEnabled(false);
        }

        FormHelpPanel helpPanel = new FormHelpPanel(() -> {
            ModelNode address = Baseadress.get();
            address.add("subsystem", "messaging-activemq");
            address.add("server", presenter.getCurrentServer());
            address.add("cluster-connection", "*");
            return address;
        }, form);

        FormLayout formLayout = new FormLayout()
                .setForm(form)
                .setHelp(helpPanel);

        if (!isCreate) {
            FormToolStrip<ActivemqClusterConnection> formTools = new FormToolStrip<>(form, callback);
            formLayout.setTools(formTools);
        }

        return formLayout.build();
    }

    private void buildForm() {
        FormItem name;

        if (isCreate) { name = new TextBoxItem("name", "Name"); } else { name = new TextItem("name", "Name"); }

        NumberBoxItem callTimeout = new NumberBoxItem("callTimeout", "Call Timeout");
        callTimeout.setRequired(false);

        NumberBoxItem checkPeriod = new NumberBoxItem("checkPeriod", "Check Period");
        checkPeriod.setRequired(false);

        TextBoxItem connectionAddress = new TextBoxItem("clusterConnectionAddress", "Connection Address");

        NumberBoxItem connectionTtl = new NumberBoxItem("connectionTTL", "Connection TTL");
        connectionTtl.setRequired(false);

        TextBoxItem connectorName = new TextBoxItem("connectorName", "Connector Name");

        TextBoxItem groupName = new TextBoxItem("discoveryGroup", "Discovery Group");
        groupName.setRequired(false);

        CheckBoxItem forward = new CheckBoxItem("forwardWhenNoConsumers", "Forward?");

        NumberBoxItem maxHops = new NumberBoxItem("maxHops", "Max Hops");
        maxHops.setRequired(false);

        NumberBoxItem retryInterval = new NumberBoxItem("retryInterval", "Retry Interval");
        retryInterval.setRequired(false);

        NumberBoxItem maxRetryInterval = new NumberBoxItem("maxRetryInterval", "Max Retry Interval");
        maxRetryInterval.setRequired(false);

        NumberBoxItem reconnect = new NumberBoxItem("reconnectAttempts", "Reconnect Attempts", -1, Integer.MAX_VALUE);
        reconnect.setRequired(false);

        CheckBoxItem duplicateDetection = new CheckBoxItem("duplicateDetection", "Duplicate Detection?");

        if (isCreate) {
            form.setFields(name, groupName, connectorName, connectionAddress);
        } else {
            form.setFields(name, groupName, connectorName, connectionAddress,
                    duplicateDetection, forward,
                    callTimeout, checkPeriod,
                    connectionTtl, maxHops,
                    retryInterval, maxRetryInterval,
                    reconnect);
        }
    }

    public Form<ActivemqClusterConnection> getForm() {
        return form;
    }

    public void setIsCreate(boolean create) {
        isCreate = create;
    }

}
