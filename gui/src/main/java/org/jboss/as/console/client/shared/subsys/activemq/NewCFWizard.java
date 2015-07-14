package org.jboss.as.console.client.shared.subsys.activemq;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.activemq.forms.DefaultCFForm;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnectionFactory;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import java.util.Map;

/**
 * @author Heiko Braun
 * @date 4/3/12
 */
public class NewCFWizard {

    MsgDestinationsPresenter presenter;

    public NewCFWizard(MsgDestinationsPresenter presenter) {
        this.presenter = presenter;
    }

    Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.addStyleName("window-content");

        DefaultCFForm defaultAttributes = new DefaultCFForm(
                new FormToolStrip.FormCallback<ActivemqConnectionFactory>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {}

                    @Override
                    public void onDelete(ActivemqConnectionFactory entity) {}
                }, false
        );

        defaultAttributes.getForm().setNumColumns(1);
        defaultAttributes.getForm().setEnabled(true);
        defaultAttributes.setIsCreate(true);

        layout.add(defaultAttributes.asWidget());

        DialogueOptions options = new DialogueOptions(
                event -> {
                    Form<ActivemqConnectionFactory> form = defaultAttributes.getForm();
                    FormValidation validation = form.validate();
                    if (!validation.hasErrors()) { presenter.onCreateCF(form.getUpdatedEntity()); }
                },
                event -> presenter.closeDialogue()
        );

        return new WindowContentBuilder(layout, options).build();
    }
}
