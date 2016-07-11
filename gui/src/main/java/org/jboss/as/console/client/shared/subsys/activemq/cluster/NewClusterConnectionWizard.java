package org.jboss.as.console.client.shared.subsys.activemq.cluster;

import java.util.Map;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.activemq.forms.ClusterConnectionForm;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqClusterConnection;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

/**
 * @author Heiko Braun
 * @date 4/4/12
 */
public class NewClusterConnectionWizard {

    private MsgClusteringPresenter presenter;

    public NewClusterConnectionWizard(MsgClusteringPresenter presenter) {
        this.presenter = presenter;
    }

    public Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.addStyleName("window-content");

        final ClusterConnectionForm form = new ClusterConnectionForm(presenter,
                new FormToolStrip.FormCallback<ActivemqClusterConnection>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {}

                    @Override
                    public void onDelete(ActivemqClusterConnection entity) {}
                }, true);

        layout.add(form.asWidget());

        DialogueOptions options = new DialogueOptions(
                event -> {
                    Form<ActivemqClusterConnection> actualForm = form.getForm();
                    FormValidation validation = actualForm.validate();
                    if (!validation.hasErrors()) {
                        ActivemqClusterConnection entity = actualForm.getUpdatedEntity();
                        presenter.onCreateClusterConnection(entity);
                    }
                },
                event -> presenter.closeDialogue()
        );

        return new WindowContentBuilder(layout, options).build();
    }
}
