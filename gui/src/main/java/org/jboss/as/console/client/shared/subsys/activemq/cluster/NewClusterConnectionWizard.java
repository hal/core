package org.jboss.as.console.client.shared.subsys.activemq.cluster;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.activemq.forms.ClusterConnectionForm;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqClusterConnection;
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
public class NewClusterConnectionWizard {
    private MsgClusteringPresenter presenter;
    private List<String> names;

    public NewClusterConnectionWizard(MsgClusteringPresenter presenter, List<String> names) {
        this.presenter = presenter;
        this.names = names;
    }

    public Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.addStyleName("window-content");

        final ClusterConnectionForm form = new ClusterConnectionForm(new FormToolStrip.FormCallback<ActivemqClusterConnection>() {
            @Override
            public void onSave(Map<String, Object> changeset) {}

            @Override
            public void onDelete(ActivemqClusterConnection entity) {}
        }, true);

        form.setSocketBindings(names);
        layout.add(form.asWidget());

        DialogueOptions options = new DialogueOptions(
                event -> {
                    Form<ActivemqClusterConnection> actualForm = form.getForm();
                    FormValidation validation = actualForm .validate();
                    if(!validation.hasErrors()) {
                        ActivemqClusterConnection entity = actualForm.getUpdatedEntity();
                        presenter.onCreateClusterConnection(entity);
                    }
                },
                event -> presenter.closeDialogue()
        );

        return new WindowContentBuilder(layout, options).build();
    }
}
