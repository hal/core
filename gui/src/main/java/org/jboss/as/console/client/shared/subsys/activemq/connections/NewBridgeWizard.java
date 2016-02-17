package org.jboss.as.console.client.shared.subsys.activemq.connections;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.activemq.forms.DefaultBridgeForm;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqBridge;
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
public class NewBridgeWizard {
    private MsgConnectionsPresenter presenter;
    private List<String> names;

    public NewBridgeWizard(MsgConnectionsPresenter presenter, List<String> names) {
        this.presenter = presenter;
        this.names = names;
        
    }

    public Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.addStyleName("window-content");

        DefaultBridgeForm form = new DefaultBridgeForm(presenter, new FormToolStrip.FormCallback<ActivemqBridge>() {
            @Override
            public void onSave(Map<String, Object> changeset) {}

            @Override
            public void onDelete(ActivemqBridge entity) {}
        }, false);

        form.setIsCreate(true);
        form.setQueueNames(names);
        layout.add(form.asWidget());

        DialogueOptions options = new DialogueOptions(
                event -> {
                    Form<ActivemqBridge> actualForm = form.getForm();
                    FormValidation validation = actualForm .validate();
                    if(!validation.hasErrors()) {
                        ActivemqBridge entity = actualForm.getUpdatedEntity();
                        presenter.onCreateBridge(entity);
                    }
                },
                event -> presenter.closeDialogue()
        );

        return new WindowContentBuilder(layout, options).build();
    }
}
