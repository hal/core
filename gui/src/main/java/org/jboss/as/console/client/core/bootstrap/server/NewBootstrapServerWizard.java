package org.jboss.as.console.client.core.bootstrap.server;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Harald Pehl
 * @date 02/27/2013
 */
public class NewBootstrapServerWizard implements IsWidget
{
    private final BootstrapServerDialog view;

    public NewBootstrapServerWizard(final BootstrapServerDialog view)
    {
        this.view = view;
    }

    @Override
    public Widget asWidget()
    {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");

        final Form<BootstrapServer> form = new Form<BootstrapServer>(BootstrapServer.class);
        TextBoxItem nameItem = new TextBoxItem("name", "Name");
        TextBoxItem urlItem = new TextBoxItem("url", "URL");
        form.setFields(nameItem, urlItem);

        final Label errorMessages = new Label();
        errorMessages.setStyleName("error-panel");

        DialogueOptions options = new DialogueOptions(
                // add
                new ClickHandler()
                {
                    @Override
                    public void onClick(ClickEvent event)
                    {
                        FormValidation validation = form.validate();
                        if (!validation.hasErrors())
                        {
                            errorMessages.setText("");
                            BootstrapServer entity = form.getUpdatedEntity();

                            boolean sameName = false;
//                            List<BootstrapServer> servers = view.getTable().getDataProvider().getList();
//                            for (BootstrapServer server : servers)
//                            {
//                                if (server.getName().equals(entity.getName()))
//                                {
//                                    sameName = true;
//                                    break;
//                                }
//                            }
//
//                            if (sameName)
//                            {
//                                errorMessages.setText("Server with this name already exists");
//                            }
//                            else
//                            {
//                                view.add(entity);
//                            }
                        }
                    }
                },

                // cancel
                new ClickHandler()
                {
                    @Override
                    public void onClick(ClickEvent event)
                    {
//                        view.closeWizard();
                    }
                }
        );

        Widget formWidget = form.asWidget();
        final FormHelpPanel helpPanel = new FormHelpPanel(new FormHelpPanel.AddressCallback()
        {
            @Override
            public ModelNode getAddress()
            {
                ModelNode address = new ModelNode();
                address.add("path", "*");
                return address;
            }
        }, form);
        layout.add(helpPanel.asWidget());
        layout.add(formWidget);
        layout.add(errorMessages);

        return new WindowContentBuilder(layout, options).build();
    }
}
