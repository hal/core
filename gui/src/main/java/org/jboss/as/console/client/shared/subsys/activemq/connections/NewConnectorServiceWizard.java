package org.jboss.as.console.client.shared.subsys.activemq.connections;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.activemq.forms.ConnectorServiceForm;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnectorService;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import java.util.Map;

/**
 * @author Heiko Braun
 * @date 4/4/12
 */
public class NewConnectorServiceWizard {

    private MsgConnectionsPresenter presenter;

    public NewConnectorServiceWizard(MsgConnectionsPresenter presenter) {
        this.presenter = presenter;
    }

    public Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.addStyleName("window-content");

        final ConnectorServiceForm form = new ConnectorServiceForm(new FormToolStrip.FormCallback<ActivemqConnectorService>() {
            @Override
            public void onSave(Map<String, Object> changeset) {}

            @Override
            public void onDelete(ActivemqConnectorService entity) {}
        });

        form.setIsCreate(true);
        layout.add(form.asWidget());

        DialogueOptions options = new DialogueOptions(
                event -> {
                    Form<ActivemqConnectorService> actualForm = form.getForm();
                    FormValidation validation = actualForm.validate();
                    if (!validation.hasErrors()) {
                        ActivemqConnectorService entity = actualForm.getUpdatedEntity();
                        presenter.onCreateConnectorService(entity);
                    }
                },
                event -> presenter.closeDialogue()
        );

        return new WindowContentBuilder(layout, options).build();
    }
}
