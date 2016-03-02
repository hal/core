package org.jboss.as.console.client.shared.subsys.activemq.connections;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.activemq.forms.AcceptorForm;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqAcceptor;
import org.jboss.as.console.client.shared.subsys.activemq.model.AcceptorType;
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
public class NewAcceptorWizard {
    private MsgConnectionsPresenter presenter;
    private AcceptorType type;

    public NewAcceptorWizard(MsgConnectionsPresenter presenter, AcceptorType type) {
        this.presenter = presenter;
        this.type = type;
    }

    public Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.addStyleName("window-content");

        final AcceptorForm form = new AcceptorForm(presenter,
                new FormToolStrip.FormCallback<ActivemqAcceptor>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                    }

                    @Override
                    public void onDelete(ActivemqAcceptor entity) {
                    }
                }, type);

        form.setIsCreate(true);
        layout.add(form.asWidget());

        DialogueOptions options = new DialogueOptions(
                event -> {
                    Form<ActivemqAcceptor> actualForm = form.getForm();
                    FormValidation validation = actualForm .validate();
                    if(!validation.hasErrors()) {
                        ActivemqAcceptor entity = actualForm.getUpdatedEntity();
                        entity.setType(type);
                        presenter.onCreateAcceptor(entity);
                    }
                },
                event -> presenter.closeDialogue()
        );

        return new WindowContentBuilder(layout, options).build();
    }
}
