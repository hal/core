package org.jboss.as.console.client.shared.subsys.activemq.forms;

import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.layout.FormLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnectorService;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.TextAreaItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.dmr.client.ModelNode;

import java.util.Collections;

/**
 * @author Heiko Braun
 * @date 4/3/12
 */
public class ConnectorServiceForm {

    Form<ActivemqConnectorService> form = new Form<>(ActivemqConnectorService.class);
    boolean isCreate = false;
    private FormToolStrip.FormCallback<ActivemqConnectorService> callback;
    private MultiWordSuggestOracle oracle;


    public ConnectorServiceForm(FormToolStrip.FormCallback<ActivemqConnectorService> callback) {
        this.callback = callback;
        oracle = new MultiWordSuggestOracle();
        oracle.setDefaultSuggestionsFromText(Collections.emptyList());
    }

    public ConnectorServiceForm(FormToolStrip.FormCallback<ActivemqConnectorService> callback, boolean create) {
        this.callback = callback;
        isCreate = create;
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
            address.add("connector-service", "*");
            return address;
        }, form);

        FormLayout formLayout = new FormLayout()
                .setForm(form)
                .setHelp(helpPanel);

        if (!isCreate) {
            FormToolStrip<ActivemqConnectorService> formTools = new FormToolStrip<>(form, callback);
            formLayout.setTools(formTools);
        }

        return formLayout.build();
    }

    private void buildForm() {
        FormItem name;

        if (isCreate) { name = new TextBoxItem("name", "Name"); } else { name = new TextItem("name", "Name"); }
        TextAreaItem factory = new TextAreaItem("factoryClass", "Factory Class");
        form.setFields(name, factory);
    }

    public Form<ActivemqConnectorService> getForm() {
        return form;
    }

    public void setIsCreate(boolean create) {
        isCreate = create;
    }
}
