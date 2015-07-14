package org.jboss.as.console.client.shared.subsys.activemq;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.activemq.forms.DivertForm;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqDivert;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 * @date 4/3/12
 */
public class NewDivertWizard {

    private MsgDestinationsPresenter presenter;
    private List<String> queueNames;

    public NewDivertWizard(MsgDestinationsPresenter presenter, List<String> queueNames) {
        this.presenter = presenter;
        this.queueNames = queueNames;
    }

    Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.addStyleName("window-content");

        DivertForm divertForm = new DivertForm(
                new FormToolStrip.FormCallback<ActivemqDivert>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {}

                    @Override
                    public void onDelete(ActivemqDivert entity) {}
                }, false
        );

        divertForm.setQueueNames(queueNames);
        divertForm.getForm().setNumColumns(1);
        divertForm.setIsCreate(true);

        layout.add(divertForm.asWidget());

        DialogueOptions options = new DialogueOptions(
                event -> {
                    Form<ActivemqDivert> form = divertForm.getForm();
                    FormValidation validation = form.validate();
                    if (!validation.hasErrors()) { presenter.onCreateDivert(form.getUpdatedEntity()); }
                },
                event -> presenter.closeDialogue()
        );

        return new WindowContentBuilder(layout, options).build();
    }
}
