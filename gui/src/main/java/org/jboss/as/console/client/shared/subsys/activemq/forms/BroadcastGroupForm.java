package org.jboss.as.console.client.shared.subsys.activemq.forms;

import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.layout.FormLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqBroadcastGroup;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.ListItem;
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
public class BroadcastGroupForm {

    Form<ActivemqBroadcastGroup> form = new Form<>(ActivemqBroadcastGroup.class);
    boolean isCreate = false;
    private FormToolStrip.FormCallback<ActivemqBroadcastGroup> callback;
    private MultiWordSuggestOracle oracle;

    public BroadcastGroupForm(FormToolStrip.FormCallback<ActivemqBroadcastGroup> callback) {
        this.callback = callback;
        oracle = new MultiWordSuggestOracle();
        oracle.setDefaultSuggestionsFromText(Collections.emptyList());
    }

    public BroadcastGroupForm(FormToolStrip.FormCallback<ActivemqBroadcastGroup> callback, boolean create) {
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
            address.add("broadcast-group", "*");
            return address;
        }, form);

        FormLayout formLayout = new FormLayout()
                .setForm(form)
                .setHelp(helpPanel);

        if (!isCreate) {
            FormToolStrip<ActivemqBroadcastGroup> formTools = new FormToolStrip<>(form, callback);
            formLayout.setTools(formTools);
        }

        return formLayout.build();
    }

    private void buildForm() {
        FormItem name;

        if (isCreate) { name = new TextBoxItem("name", "Name"); } else { name = new TextItem("name", "Name"); }
        ListItem connectors = new ListItem("connectors", "Connectors");
        TextBoxItem socket = new TextBoxItem("socketBinding", "Socket Binding");
        NumberBoxItem period = new NumberBoxItem("broadcastPeriod", "Broadcast Period");
        if (isCreate) { form.setFields(name, socket, connectors); } else {
            form.setFields(name, socket, connectors, period);
        }
    }

    public Form<ActivemqBroadcastGroup> getForm() {
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
