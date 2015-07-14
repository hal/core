package org.jboss.as.console.client.shared.subsys.activemq.cluster;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.activemq.forms.DiscoveryGroupForm;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqDiscoveryGroup;
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
public class NewDiscoveryGroupWizard {
    private MsgClusteringPresenter presenter;
    private List<String> names;

    public NewDiscoveryGroupWizard(MsgClusteringPresenter presenter, List<String> names) {
        this.presenter = presenter;
        this.names = names;
    }

    public Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.addStyleName("window-content");

        final DiscoveryGroupForm form = new DiscoveryGroupForm(new FormToolStrip.FormCallback<ActivemqDiscoveryGroup>() {
            @Override
            public void onSave(Map<String, Object> changeset) {}

            @Override
            public void onDelete(ActivemqDiscoveryGroup entity) {}
        }, true);

        form.setSocketBindings(names);
        layout.add(form.asWidget());

        DialogueOptions options = new DialogueOptions(
                event -> {
                    Form<ActivemqDiscoveryGroup> actualForm = form.getForm();
                    FormValidation validation = actualForm .validate();
                    if(!validation.hasErrors()) {
                        ActivemqDiscoveryGroup entity = actualForm.getUpdatedEntity();
                        presenter.onCreateDiscoveryGroup(entity);
                    }
                },
                event -> presenter.closeDialogue()
        );

        return new WindowContentBuilder(layout, options).build();
    }
}
