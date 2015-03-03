package org.jboss.as.console.client.shared.general.forms;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.layout.FormLayout;
import org.jboss.as.console.client.shared.general.model.RemoteSocketBinding;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.*;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @date 4/3/12
 */
public class RemoteSocketForm {

    private Form<RemoteSocketBinding> form = new Form<>(RemoteSocketBinding.class);
    private FormToolStrip.FormCallback<RemoteSocketBinding> callback;

    public RemoteSocketForm(FormToolStrip.FormCallback<RemoteSocketBinding> callback) {
        this.callback = callback;
    }

    public Widget asWidget() {

        buildForm();
        form.setEnabled(false);

        FormHelpPanel helpPanel = new FormHelpPanel(
                new FormHelpPanel.AddressCallback() {
                    @Override
                    public ModelNode getAddress() {
                        ModelNode address = new ModelNode();
                        address.add("socket-binding-group", "*");
                        address.add("remote-destination-outbound-socket-binding", "*");
                        return address;
                    }
                }, form);

        FormToolStrip<RemoteSocketBinding> formTools = new FormToolStrip<>(form, callback);
        FormLayout formLayout = new FormLayout().setForm(form).setHelp(helpPanel);
        formLayout.setTools(formTools);
        return formLayout.build();
    }

    private void buildForm() {
        FormItem name = new TextItem("name", "Name");
        NumberBoxItem port= new NumberBoxItem("port", "Port");
        TextBoxItem host = new TextBoxItem("host", "Host");
        NumberBoxItem sourcePort = new NumberBoxItem("sourcePort", "Source Port");
        TextBoxItem sourceInterface = new TextBoxItem("sourceInterface", "Source Interface");
        CheckBoxItem fixed = new CheckBoxItem("fixedSourcePort", "Fixed Source Port?");

        form.setFields(name, host, port, sourceInterface, sourcePort, fixed);
    }

    public Form<RemoteSocketBinding> getForm() {
        return form;
    }
}
