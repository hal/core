package org.jboss.as.console.client.shared.subsys.activemq.cluster;

import java.util.Map;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.activemq.forms.DiscoveryGroupForm;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqDiscoveryGroup;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

/**
 * @author Heiko Braun
 * @date 4/4/12
 */
public class NewDiscoveryGroupWizard {

    private MsgClusteringPresenter presenter;

    public NewDiscoveryGroupWizard(MsgClusteringPresenter presenter) {
        this.presenter = presenter;
    }

    public Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.addStyleName("window-content");

        final DiscoveryGroupForm form = new DiscoveryGroupForm(presenter,
                new FormToolStrip.FormCallback<ActivemqDiscoveryGroup>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {}

                    @Override
                    public void onDelete(ActivemqDiscoveryGroup entity) {}
                }, true);

        layout.add(form.asWidget());

        DialogueOptions options = new DialogueOptions(
                event -> {
                    Form<ActivemqDiscoveryGroup> actualForm = form.getForm();
                    FormValidation validation = actualForm.validate();
                    if (!validation.hasErrors()) {
                        ActivemqDiscoveryGroup entity = actualForm.getUpdatedEntity();
                        presenter.onCreateDiscoveryGroup(entity);
                    }
                },
                event -> presenter.closeDialogue()
        );

        return new WindowContentBuilder(layout, options).build();
    }
}
