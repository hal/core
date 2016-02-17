package org.jboss.as.console.client.shared.subsys.activemq.forms;

import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.layout.FormLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqAcceptor;
import org.jboss.as.console.client.shared.subsys.activemq.model.AcceptorType;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
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
public class AcceptorForm {

    Form<ActivemqAcceptor> form = new Form<>(ActivemqAcceptor.class);
    private AcceptorType type = null;

    boolean isCreate = false;
    private FormToolStrip.FormCallback<ActivemqAcceptor> callback;

    private MultiWordSuggestOracle oracle;

    public AcceptorForm(FormToolStrip.FormCallback<ActivemqAcceptor> callback, AcceptorType type) {
        this.callback = callback;
        oracle = new MultiWordSuggestOracle();
        oracle.setDefaultSuggestionsFromText(Collections.emptyList());
        this.type = type;
    }

    public AcceptorForm(FormToolStrip.FormCallback<ActivemqAcceptor> callback, AcceptorType type, boolean create) {
        this.callback = callback;
        isCreate = create;
        oracle = new MultiWordSuggestOracle();
        oracle.setDefaultSuggestionsFromText(Collections.emptyList());
        this.type = type;
    }

    public Widget asWidget() {
        switch (type) {
            case GENERIC:
                buildGenericForm();
                break;
            case REMOTE:
                buildRemoteForm();
                break;
            case INVM:
                buildInvmForm();
        }

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
            address.add(type.getResource(), "*");
            return address;
        }, form);

        FormLayout formLayout = new FormLayout()
                .setForm(form)
                .setHelp(helpPanel);

        if (!isCreate) {
            FormToolStrip<ActivemqAcceptor> formTools = new FormToolStrip<ActivemqAcceptor>(form, callback);
            formLayout.setTools(formTools);
        }

        return formLayout.build();
    }

    private void buildInvmForm() {
        FormItem name;
        if (isCreate) { name = new TextBoxItem("name", "Name"); } else { name = new TextItem("name", "Name"); }
        NumberBoxItem server = new NumberBoxItem("serverId", "Server ID");
        form.setFields(name, server);
    }

    private void buildRemoteForm() {
        FormItem name;
        if (isCreate) { name = new TextBoxItem("name", "Name"); } else { name = new TextItem("name", "Name"); }
        SuggestBoxItem socket = new SuggestBoxItem("socketBinding", "Socket Binding");
        socket.setOracle(oracle);
        form.setFields(name, socket);
    }

    private void buildGenericForm() {
        FormItem name;
        if (isCreate) { name = new TextBoxItem("name", "Name"); } else { name = new TextItem("name", "Name"); }
        SuggestBoxItem socket = new SuggestBoxItem("socketBinding", "Socket Binding");
        TextAreaItem factory = new TextAreaItem("factoryClass", "Factory Class");
        socket.setOracle(oracle);
        form.setFields(name, socket, factory);
    }

    public Form<ActivemqAcceptor> getForm() {
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
