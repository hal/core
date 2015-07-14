package org.jboss.as.console.client.shared.subsys.activemq.forms;

import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.layout.FormLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqDiscoveryGroup;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
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
public class DiscoveryGroupForm {

    Form<ActivemqDiscoveryGroup> form = new Form<>(ActivemqDiscoveryGroup.class);
    boolean isCreate = false;
    private FormToolStrip.FormCallback<ActivemqDiscoveryGroup> callback;
    private MultiWordSuggestOracle oracle;

    public DiscoveryGroupForm(FormToolStrip.FormCallback<ActivemqDiscoveryGroup> callback) {
        this.callback = callback;
        oracle = new MultiWordSuggestOracle();
        oracle.setDefaultSuggestionsFromText(Collections.emptyList());
    }

    public DiscoveryGroupForm(FormToolStrip.FormCallback<ActivemqDiscoveryGroup> callback, boolean create) {
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
            address.add("discovery-group", "*");
            return address;
        }, form);

        FormLayout formLayout = new FormLayout()
                .setForm(form)
                .setHelp(helpPanel);

        if (!isCreate) {
            FormToolStrip<ActivemqDiscoveryGroup> formTools = new FormToolStrip<>(form, callback);
            formLayout.setTools(formTools);
        }

        return formLayout.build();
    }

    private void buildForm() {
        FormItem name;

        if (isCreate) { name = new TextBoxItem("name", "Name"); } else { name = new TextItem("name", "Name"); }
        NumberBoxItem initialWait = new NumberBoxItem("initialWaitTimeout", "Initial Wait Timeout");
        NumberBoxItem refresh = new NumberBoxItem("refreshTimeout", "Refresh Timeout");
        TextBoxItem socket = new TextBoxItem("socketBinding", "Socket Binding");
        if (isCreate) { form.setFields(name, socket); } else { form.setFields(name, socket, initialWait, refresh); }
    }

    public Form<ActivemqDiscoveryGroup> getForm() {
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
