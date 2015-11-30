package org.jboss.as.console.client.domain.groups;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.UIMessages;
import org.jboss.as.console.client.domain.hosts.HostMgmtPresenter;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
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
 * @date 10/22/12
 */
public class CopyGroupWizard {
    private HostMgmtPresenter presenter;
    private ServerGroupRecord orig;

    public CopyGroupWizard(HostMgmtPresenter presenter, ServerGroupRecord orig) {
        this.presenter = presenter;
        this.orig = orig;
    }

    public Widget asWidget() {

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");


        layout.add(new ContentDescription(
                Console.MESSAGES.copyGroupDescription(orig.getName())));

        final Form<ServerGroupRecord> form = new Form<ServerGroupRecord>(ServerGroupRecord.class);
        form.setNumColumns(1);

        TextBoxItem nameItem = new TextBoxItem("name", Console.CONSTANTS.common_label_name())
        {
            @Override
            public boolean validate(String value) {
                boolean hasValue = super.validate(value);
                boolean hasWhitespace = value.contains(" ");
                return hasValue && !hasWhitespace;
            }

            @Override
            public String getErrMessage() {
                return Console.MESSAGES.common_validation_notEmptyNoSpace();
            }
        };

        nameItem.setValue(orig.getName()+"_copy");

        form.setFields(nameItem);

        final FormHelpPanel helpPanel = new FormHelpPanel(
                new FormHelpPanel.AddressCallback() {
                    @Override
                    public ModelNode getAddress() {
                        ModelNode address = new ModelNode();
                        address.add("server-group", "*");
                        return address;
                    }
                }, form
        );
        layout.add(helpPanel.asWidget());

        layout.add(form.asWidget());


        // ---

        ClickHandler saveHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final ServerGroupRecord newGroup = form.getUpdatedEntity();

                FormValidation validation = form.validate();
                if (!validation.hasErrors())
                {
                    presenter.onSaveCopy(orig, newGroup);
                }
            }
        };


        ClickHandler cancelHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.closeDialoge();
            }
        };

        DialogueOptions options = new DialogueOptions(saveHandler, cancelHandler);

        return new WindowContentBuilder(layout, options).build();

    }

}
