package org.jboss.as.console.client.shared.subsys.activemq.forms;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.layout.FormLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnectionFactory;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.as.console.client.widgets.forms.items.JndiNameItem;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @date 4/3/12
 */
public class DefaultCFForm {

    private Form<ActivemqConnectionFactory> form = new Form<>(ActivemqConnectionFactory.class);
    private FormToolStrip.FormCallback<ActivemqConnectionFactory> callback;
    private boolean provideTools = true;
    private boolean isCreate = false;

    public DefaultCFForm(FormToolStrip.FormCallback<ActivemqConnectionFactory> callback) {
        this.callback = callback;
        form.setNumColumns(2);
    }

    public DefaultCFForm(FormToolStrip.FormCallback<ActivemqConnectionFactory> callback, boolean provideTools) {
        this.callback = callback;
        this.provideTools = provideTools;
        form.setNumColumns(2);
    }

    public void setIsCreate(boolean b) {
        this.isCreate = b;
    }

    public Widget asWidget() {
        JndiNameItem jndiName = new JndiNameItem("jndiName", "JNDI Name");
        TextBoxItem groupId = new TextBoxItem("groupId", "Group ID", false);
        TextBoxItem connector = new TextBoxItem("connector", "Connector");
        CheckBoxItem failoverInitial = new CheckBoxItem("failoverInitial", "Failover Initial?");
        CheckBoxItem globalPools = new CheckBoxItem("useGlobalPools", "Global Pools?");
        NumberBoxItem threadPool = new NumberBoxItem("threadPoolMax", "Thread Pool Max");
        NumberBoxItem txBatch = new NumberBoxItem("transactionBatchSize", "Transaction Batch Size");

        if (isCreate) {
            TextBoxItem name = new TextBoxItem("name", "Name");
            form.setFields(name, jndiName, connector);
        } else {
            TextItem name = new TextItem("name", "Name");

            form.setFields(
                    name, jndiName,
                    connector, groupId,
                    failoverInitial,
                    threadPool, txBatch,
                    globalPools);
        }

        FormHelpPanel helpPanel = new FormHelpPanel(() -> {
            ModelNode address = Baseadress.get();
            address.add("subsystem", "messaging-activemq");
            address.add("server", "*");
            address.add("connection-factory", "*");
            return address;
        }, form);

        FormLayout formLayout = new FormLayout()
                .setForm(form)
                .setHelp(helpPanel);

        if (provideTools) {
            FormToolStrip<ActivemqConnectionFactory> formTools = new FormToolStrip<ActivemqConnectionFactory>(form, callback);
            formLayout.setTools(formTools);
        }
        return formLayout.build();
    }

    public Form<ActivemqConnectionFactory> getForm() {
        return form;
    }
}
