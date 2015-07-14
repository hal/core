package org.jboss.as.console.client.shared.subsys.activemq.connections;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.activemq.forms.ConnectorForm;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnector;
import org.jboss.as.console.client.shared.subsys.activemq.model.ConnectorType;
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
public class NewConnectorWizard {
    private MsgConnectionsPresenter presenter;
    private ConnectorType type;

    public NewConnectorWizard(MsgConnectionsPresenter presenter, ConnectorType type) {
        this.presenter = presenter;
        this.type = type;
    }

    public Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.addStyleName("window-content");

        ConnectorForm form = new ConnectorForm(new FormToolStrip.FormCallback<ActivemqConnector>() {
            @Override
            public void onSave(Map<String, Object> changeset) {}

            @Override
            public void onDelete(ActivemqConnector entity) {}
        }, type);

        form.setIsCreate(true);
        layout.add(form.asWidget());

        DialogueOptions options = new DialogueOptions(
                event -> {
                    Form<ActivemqConnector> actualForm = form.getForm();
                    FormValidation validation = actualForm .validate();
                    if(!validation.hasErrors()) {
                        ActivemqConnector entity = actualForm.getUpdatedEntity();
                        entity.setType(type);
                        presenter.onCreateConnector(entity);
                    }
                },
                event -> presenter.closeDialogue()
        );

        return new WindowContentBuilder(layout, options).build();
    }
}
