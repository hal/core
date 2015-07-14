package org.jboss.as.console.client.shared.subsys.activemq.cluster;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.activemq.forms.BroadcastGroupForm;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqBroadcastGroup;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 * @date 4/4/12
 */
public class NewBroadcastGroupWizard {

    private MsgClusteringPresenter presenter;
    private List<String> names;

    public NewBroadcastGroupWizard(MsgClusteringPresenter presenter, List<String> names) {
        this.presenter = presenter;
        this.names = names;
    }

    public Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.addStyleName("window-content");

        BroadcastGroupForm form = new BroadcastGroupForm(new FormToolStrip.FormCallback<ActivemqBroadcastGroup>() {
            @Override
            public void onSave(Map<String, Object> changeset) {}

            @Override
            public void onDelete(ActivemqBroadcastGroup entity) {}
        }, true);

        form.setSocketBindings(names);
        layout.add(form.asWidget());

        DialogueOptions options = new DialogueOptions(
                event -> {
                    Form<ActivemqBroadcastGroup> actualForm = form.getForm();
                    FormValidation validation = actualForm.validate();
                    if (!validation.hasErrors()) {
                        ActivemqBroadcastGroup entity = actualForm.getUpdatedEntity();
                        presenter.onCreateBroadcastGroup(entity);
                    }
                },
                event -> presenter.closeDialogue()
        );

        return new WindowContentBuilder(layout, options).build();
    }
}
