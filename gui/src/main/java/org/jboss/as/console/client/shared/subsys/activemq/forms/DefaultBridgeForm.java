package org.jboss.as.console.client.shared.subsys.activemq.forms;

import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.layout.FormLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqBridge;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.ListItem;
import org.jboss.ballroom.client.widgets.forms.SuggestBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextAreaItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.dmr.client.ModelNode;

import java.util.Collections;
import java.util.List;

/**
 * @author Heiko Braun
 * @date 4/3/12
 */
public class DefaultBridgeForm {

    private Form<ActivemqBridge> form = new Form<>(ActivemqBridge.class);
    private FormToolStrip.FormCallback<ActivemqBridge> callback;
    private boolean provideTools = true;
    private boolean isCreate = false;
    private MultiWordSuggestOracle oracle;
    private TextBoxItem discoveryGroup;
    private ListItem connectors;

    public DefaultBridgeForm(FormToolStrip.FormCallback<ActivemqBridge> callback) {
        this.callback = callback;
        form.setNumColumns(2);
        oracle = new MultiWordSuggestOracle();
        oracle.setDefaultSuggestionsFromText(Collections.emptyList());
    }

    public DefaultBridgeForm(FormToolStrip.FormCallback<ActivemqBridge> callback, boolean provideTools) {
        this.callback = callback;
        this.provideTools = provideTools;
        form.setNumColumns(2);
        oracle = new MultiWordSuggestOracle();
        oracle.setDefaultSuggestionsFromText(Collections.emptyList());
    }

    public void setIsCreate(boolean b) {
        this.isCreate = b;
    }

    public Widget asWidget() {
        form.addFormValidator((formItems, outcome) -> {
            if (!discoveryGroup.getValue().equals("") && connectors.getValue().size() > 0) {
                discoveryGroup.setErroneous(true);
                connectors.setErroneous(true);

                String errMessage = "Discovery group or connectors can be defined, not both";
                discoveryGroup.setErrMessage(errMessage);
                connectors.setErrMessage(errMessage);

                outcome.addError("discoveryGroup");
                outcome.addError("connectors");
            }
        });

        SuggestBoxItem queueName = new SuggestBoxItem("queueName", "Queue Name");
        SuggestBoxItem forward = new SuggestBoxItem("forwardingAddress", "Forward Address");
        TextAreaItem filter = new TextAreaItem("filter", "Filter", false);
        TextAreaItem transformer = new TextAreaItem("transformerClass", "Transformer Class", false);

        queueName.setOracle(oracle);
        forward.setOracle(oracle);

        discoveryGroup = new TextBoxItem("discoveryGroup", "Discovery Group", false);
        connectors = new ListItem("staticConnectors", "Static Connectors", false);

        if (isCreate) {
            TextBoxItem name = new TextBoxItem("name", "Name");

            form.setFields(
                    name, queueName,
                    forward, discoveryGroup,
                    connectors);

            form.setNumColumns(1);
        } else {
            TextItem name = new TextItem("name", "Name");

            form.setFields(
                    name,
                    queueName, forward,
                    discoveryGroup, connectors,
                    filter, transformer
            );
        }

        FormHelpPanel helpPanel = new FormHelpPanel(() -> {
            ModelNode address = Baseadress.get();
            address.add("subsystem", "messaging-activemq");
            address.add("server", "*");
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

    public void setQueueNames(List<String> queueNames) {
        oracle.addAll(queueNames);
    }
}
