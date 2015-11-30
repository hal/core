package org.jboss.as.console.client.shared.general.wizard;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.general.SocketBindingPresenter;
import org.jboss.as.console.client.shared.general.model.SocketGroup;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 */
public class CloneGroupWizard {
    private final String origName;
    private SocketBindingPresenter presenter;

    public CloneGroupWizard(String origName, SocketBindingPresenter presenter) {
        this.origName = origName;
        this.presenter = presenter;
    }

    public Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.addStyleName("window-content");

        layout.add(new ContentDescription("Create a copy of socket binding group: "+origName));

        final Form<SocketGroup> form = new Form<>(SocketGroup.class);
        TextBoxItem name = new TextBoxItem("name", "New Name");
        form.setFields(name);

        FormHelpPanel helpPanel = new FormHelpPanel(new FormHelpPanel.AddressCallback() {
            @Override
            public ModelNode getAddress() {
                ModelNode address = new ModelNode();
                address.add("socket-binding-group", "*");
                return address;
            }
        }, form);

        layout.add(helpPanel.asWidget());
        layout.add(form.asWidget());

        DialogueOptions options = new DialogueOptions(
                new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        FormValidation validation = form.validate();
                        if (!validation.hasErrors()) {
                            SocketGroup entity = form.getUpdatedEntity();
                            presenter.onCloneGroup(origName, entity.getName());
                        }
                    }
                },
                new ClickHandler() {

                    @Override
                    public void onClick(ClickEvent event) {
                        presenter.closeDialogue();
                    }
                }
        );
        return new WindowContentBuilder(layout, options).build();
    }
}
