package org.jboss.as.console.client.shared.subsys.activemq.forms;

import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.layout.FormLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqClusterConnection;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.dmr.client.ModelNode;

import java.util.Collections;
import java.util.List;

/**
 * @author Heiko Braun
 * @date 4/3/12
 */
public class ClusterConnectionForm {

    Form<ActivemqClusterConnection> form = new Form<>(ActivemqClusterConnection.class);
    boolean isCreate = false;
    private FormToolStrip.FormCallback<ActivemqClusterConnection> callback;
    private MultiWordSuggestOracle oracle;

    public ClusterConnectionForm(FormToolStrip.FormCallback<ActivemqClusterConnection> callback) {
        this.callback = callback;
        oracle = new MultiWordSuggestOracle();
        oracle.setDefaultSuggestionsFromText(Collections.emptyList());
    }

    public ClusterConnectionForm(FormToolStrip.FormCallback<ActivemqClusterConnection> callback, boolean create) {
        isCreate = create;
        if (!isCreate) { this.callback = callback; }
        oracle = new MultiWordSuggestOracle();
        oracle.setDefaultSuggestionsFromText(Collections.emptyList());
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
            address.add("server", "*");
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
        NumberBoxItem checkPeriod = new NumberBoxItem("checkPeriod", "Check Period");
        TextBoxItem connectionAddress = new TextBoxItem("clusterConnectionAddress", "Connection Address");
        NumberBoxItem connectionTtl = new NumberBoxItem("connectionTTL", "Connection TTL");
        TextBoxItem connectorRef = new TextBoxItem("connectorRef", "Connector Ref");
        TextBoxItem groupName = new TextBoxItem("discoveryGroupName", "Discovery Group");
        CheckBoxItem forward = new CheckBoxItem("forwardWhenNoConsumers", "Forward?");
        NumberBoxItem maxHops = new NumberBoxItem("maxHops", "Max Hops");
        NumberBoxItem retryInterval = new NumberBoxItem("retryInterval", "Retry Interval");
        NumberBoxItem maxRetryInterval = new NumberBoxItem("maxRetryInterval", "Max Retry Interval");
        NumberBoxItem reconnect = new NumberBoxItem("reconnectAttempts", "Reconnect Attempts", -1, Integer.MAX_VALUE);
        CheckBoxItem duplicateDetection = new CheckBoxItem("duplicateDetection", "Duplicate Detection?");

        if (isCreate) {
            form.setFields(name, groupName, connectorRef, connectionAddress);
        } else {
            form.setFields(name, groupName, connectorRef, connectionAddress,
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

    public void setSocketBindings(List<String> socketBindings) {
        this.oracle.clear();
        this.oracle.addAll(socketBindings);
    }
}
