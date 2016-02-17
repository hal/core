package org.jboss.as.console.client.shared.subsys.activemq.forms;

import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.layout.FormLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.activemq.MsgDestinationsPresenter;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqDivert;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.SuggestBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextAreaItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.dmr.client.ModelNode;

import java.util.Collections;
import java.util.List;

/**
 * @author Heiko Braun
 * @date 4/3/12
 */
public class DivertForm {

    Form<ActivemqDivert> form = new Form<>(ActivemqDivert.class);
    boolean isCreate = false;
    private final MsgDestinationsPresenter presenter;
    private FormToolStrip.FormCallback<ActivemqDivert> callback;
    private MultiWordSuggestOracle oracle;

    public DivertForm(MsgDestinationsPresenter presenter,
            FormToolStrip.FormCallback<ActivemqDivert> callback) {
        this.presenter = presenter;
        this.callback = callback;
        oracle = new MultiWordSuggestOracle();
        oracle.setDefaultSuggestionsFromText(Collections.emptyList());
    }

    public DivertForm(MsgDestinationsPresenter presenter,
            FormToolStrip.FormCallback<ActivemqDivert> callback, boolean create) {
        this.presenter = presenter;
        this.callback = callback;
        isCreate = create;
        oracle = new MultiWordSuggestOracle();
        oracle.setDefaultSuggestionsFromText(Collections.emptyList());
    }

    public Widget asWidget() {
        TextBoxItem routingName = new TextBoxItem("routingName", "Routing Name");
        SuggestBoxItem divertFrom = new SuggestBoxItem("divertAddress", "Divert Address");
        SuggestBoxItem divertTo = new SuggestBoxItem("forwardingAddress", "Forwarding Address");

        divertFrom.setOracle(oracle);
        divertTo.setOracle(oracle);

        TextAreaItem filter = new TextAreaItem("filter", "Filter", false);
        TextAreaItem transformer = new TextAreaItem("transformerClass", "Transformer Class");
        CheckBoxItem exclusive = new CheckBoxItem("exclusive", "Exlusive?");

        if (isCreate) {
            form.setFields(routingName, divertFrom, divertTo);
            form.setNumColumns(1);

        } else {
            form.setFields(routingName, divertFrom, divertTo, exclusive, filter, transformer);
            form.setNumColumns(2);
            form.setEnabled(false);
        }

        FormHelpPanel helpPanel = new FormHelpPanel(() -> {
            ModelNode address = Baseadress.get();
            address.add("subsystem", "messaging-activemq");
            address.add("server", presenter.getCurrentServer());
            address.add("divert", "*");
            return address;
        }, form);

        FormLayout formLayout = new FormLayout()
                .setForm(form)
                .setHelp(helpPanel);

        if (!isCreate) {
            FormToolStrip<ActivemqDivert> formTools = new FormToolStrip<>(form, callback);
            formLayout.setTools(formTools);
        }

        return formLayout.build();
    }

    public Form<ActivemqDivert> getForm() {
        return form;
    }

    public void setIsCreate(boolean create) {
        isCreate = create;
    }

    public void setQueueNames(List<String> queueNames) {
        oracle.addAll(queueNames);
    }
}
