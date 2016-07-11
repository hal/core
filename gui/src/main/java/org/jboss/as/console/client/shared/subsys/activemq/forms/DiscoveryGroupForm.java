package org.jboss.as.console.client.shared.subsys.activemq.forms;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.FormLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.activemq.cluster.MsgClusteringPresenter;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqDiscoveryGroup;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.dmr.client.ModelNode;

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.NETWORK_SOCKET_BINDING;

/**
 * @author Heiko Braun
 * @date 4/3/12
 */
public class DiscoveryGroupForm {

    Form<ActivemqDiscoveryGroup> form = new Form<>(ActivemqDiscoveryGroup.class);
    boolean isCreate = false;
    private final MsgClusteringPresenter presenter;
    private FormToolStrip.FormCallback<ActivemqDiscoveryGroup> callback;

    public DiscoveryGroupForm(MsgClusteringPresenter presenter,
            FormToolStrip.FormCallback<ActivemqDiscoveryGroup> callback) {
        this.presenter = presenter;
        this.callback = callback;
    }

    public DiscoveryGroupForm(MsgClusteringPresenter presenter,
            FormToolStrip.FormCallback<ActivemqDiscoveryGroup> callback, boolean create) {
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
        SuggestionResource suggestionResource = new SuggestionResource("socketBinding", "Socket Binding", true,
                Console.MODULES.getCapabilities().lookup(NETWORK_SOCKET_BINDING));
        FormItem socket = suggestionResource.buildFormItem();

        if (isCreate) { form.setFields(name, socket); } else { form.setFields(name, socket, initialWait, refresh); }
    }

    public Form<ActivemqDiscoveryGroup> getForm() {
        return form;
    }

    public void setIsCreate(boolean create) {
        isCreate = create;
    }

}
