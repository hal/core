package org.jboss.as.console.client.shared.subsys.jgroups;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.ballroom.client.widgets.forms.ComboBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.NETWORK_SOCKET_BINDING;

/**
 * @author Heiko Braun
 * @date 2/22/12
 */
public class StackStep1 {

    NewStackWizard presenter;

    public StackStep1(NewStackWizard presenter) {
        this.presenter = presenter;
    }

    Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");

        layout.add(new HTML("<h3>"+ Console.CONSTANTS.subsys_jgroups_step1()+"</h3>"));

        final Form<JGroupsStack> form = new Form<JGroupsStack>(JGroupsStack.class);

        TextBoxItem nameField = new TextBoxItem("type", "Name");
        ComboBoxItem transportType = new ComboBoxItem("transportType", "Transport");
        transportType.setDefaultToFirstOption(true);
        transportType.setValueMap(new String[]{"UDP", "TCP", "TUNNEL"});

        FormItem socket = new SuggestionResource("transportSocket", "Transport Socket Binding", true,
            Console.MODULES.getCapabilities().lookup(NETWORK_SOCKET_BINDING))
            .buildFormItem();

        form.setFields(nameField, transportType, socket);

        // ----------------------------------------

        Widget formWidget = form.asWidget();

        layout.add(AddStackHelpPanel.helpStep1().asWidget());
        layout.add(formWidget);

        ClickHandler submitHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                FormValidation validation = form.validate();
                if(validation.hasErrors())
                    return;

                presenter.onFinishStep1(form.getUpdatedEntity());

            }
        };

        ClickHandler cancelHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.cancel();
            }
        };

        DialogueOptions options = new DialogueOptions(
                Console.CONSTANTS.common_label_next(),submitHandler,
                Console.CONSTANTS.common_label_cancel(),cancelHandler
        );

        return new WindowContentBuilder(layout, options).build();
    }
}
