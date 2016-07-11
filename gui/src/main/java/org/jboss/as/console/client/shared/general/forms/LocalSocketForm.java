package org.jboss.as.console.client.shared.general.forms;

import java.util.Collections;
import java.util.List;

import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.FormLayout;
import org.jboss.as.console.client.shared.general.model.LocalSocketBinding;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
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
public class LocalSocketForm {

    private Form<LocalSocketBinding> form = new Form<>(LocalSocketBinding.class);
    private FormToolStrip.FormCallback<LocalSocketBinding> callback;
    private MultiWordSuggestOracle oracle;

    public LocalSocketForm(FormToolStrip.FormCallback<LocalSocketBinding> callback) {
        this.callback = callback;
        oracle = new MultiWordSuggestOracle();
        oracle.setDefaultSuggestionsFromText(Collections.<String>emptyList());
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
                        address.add("local-destination-outbound-socket-binding", "*");
                        return address;
                    }
                }, form);

        FormToolStrip<LocalSocketBinding> formTools = new FormToolStrip<LocalSocketBinding>(form, callback);
        FormLayout formLayout = new FormLayout().setForm(form).setHelp(helpPanel);
        formLayout.setTools(formTools);
        return formLayout.build();
    }

    private void buildForm() {
        FormItem name = new TextItem("name", "Name");
        FormItem socket = new SuggestionResource("socketBinding", "Socket Binding", true,
                Console.MODULES.getCapabilities().lookup(NETWORK_SOCKET_BINDING))
                .buildFormItem();
        NumberBoxItem sourcePort = new NumberBoxItem("sourcePort", "Source Port");
        TextBoxItem sourceInterface = new TextBoxItem("sourceInterface", "Source Interface");
        CheckBoxItem fixed = new CheckBoxItem("fixedSourcePort", "Fixed Source Port?");

        form.setFields(name, socket, sourceInterface, sourcePort, fixed);
    }

    public Form<LocalSocketBinding> getForm() {
        return form;
    }

    public void setSocketBindings(List<String> socketBindings) {
        this.oracle.clear();
        this.oracle.addAll(socketBindings);
    }
}
