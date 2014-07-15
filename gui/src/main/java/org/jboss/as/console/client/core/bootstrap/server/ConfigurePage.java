package org.jboss.as.console.client.core.bootstrap.server;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.ballroom.client.widgets.forms.ButtonItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import java.util.List;

/**
 * @author Harald Pehl
 * @date 02/28/2013
 */
public class ConfigurePage implements IsWidget
{
    private final BootstrapServerSetup serverSetup;
    private final BootstrapServerStore bootstrapServerStore;
    private VerticalPanel page;
    private Form<BootstrapServer> form;
    private DialogueOptions options;

    public ConfigurePage(final BootstrapServerSetup serverSetup)
    {
        this.serverSetup = serverSetup;
        this.bootstrapServerStore = new BootstrapServerStore();
        initUI();
    }

    private void initUI()
    {
        page = new VerticalPanel();
        page.setStyleName("window-content");

        final Label description = new Label("Enter the name and the URL of the new server.");
        description.getElement().getStyle().setPaddingBottom(15, Style.Unit.PX);
        page.add(description);

        final Label configureErrorMessages = new Label();
        configureErrorMessages.setStyleName("error-panel");

        form = new Form<BootstrapServer>(BootstrapServer.class);
        final TextBoxItem nameItem = new TextBoxItem("name", "Name");
        TextBoxItem urlItem = new TextBoxItem("url", "URL");
        ButtonItem pingItem = new ButtonItem("", "", "Ping");
        pingItem.addClickHandler(new ClickHandler()
        {
            @Override
            public void onClick(final ClickEvent event)
            {
                FormValidation validation = form.validate();
                if (!validation.hasErrors())
                {
                    configureErrorMessages.setText("");
                    BootstrapServer server = form.getUpdatedEntity();
                    serverSetup.pingServer(server, new AsyncCallback<Void>()
                    {
                        @Override
                        public void onFailure(final Throwable caught)
                        {
                            configureErrorMessages.setText("The server is not running.");
                        }

                        @Override
                        public void onSuccess(final Void result)
                        {
                            configureErrorMessages.setText("The server is running.");
                        }
                    });
                }
            }
        });
        form.setFields(nameItem, urlItem, pingItem);
        page.add(form);

        page.add(configureErrorMessages);

        options = new DialogueOptions(
                "Add",
                new ClickHandler()
                {
                    @Override
                    public void onClick(ClickEvent event)
                    {
                        FormValidation validation = form.validate();
                        if (!validation.hasErrors())
                        {
                            configureErrorMessages.setText("");
                            BootstrapServer newServer = form.getUpdatedEntity();

                            boolean sameName = false;
                            List<BootstrapServer> servers = bootstrapServerStore.load();
                            for (BootstrapServer server : servers)
                            {
                                if (server.getName().equals(newServer.getName()))
                                {
                                    sameName = true;
                                    break;
                                }
                            }
                            if (sameName)
                            {
                                configureErrorMessages.setText(
                                        "Server with this name already exists. Please choose another name.");
                                nameItem.getInputElement().focus();
                            }
                            else
                            {
                                bootstrapServerStore.add(newServer);
                                serverSetup.onConfigureOk();
                            }
                        }
                    }
                },
                "Cancel",
                new ClickHandler()
                {
                    @Override
                    public void onClick(final ClickEvent event)
                    {
                        serverSetup.onConfigureCancel();
                    }
                }
        );
    }

    @Override
    public Widget asWidget()
    {
        return new WindowContentBuilder(page, options).build();
    }

    void reset()
    {
        form.clearValues();
    }
}
