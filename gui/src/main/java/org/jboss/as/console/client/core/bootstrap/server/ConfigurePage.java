package org.jboss.as.console.client.core.bootstrap.server;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.forms.ButtonItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import java.util.List;

/**
 * @author Harald Pehl
 */
class ConfigurePage implements IsWidget {

    private final BootstrapServerSetup serverSetup;
    private final BootstrapServerDialog serverDialog;
    private final BootstrapServerStore serverStore;

    private Form<BootstrapServer> form;
    private TextBoxItem nameItem;

    ConfigurePage(final BootstrapServerSetup serverSetup, final BootstrapServerDialog serverDialog) {
        this.serverSetup = serverSetup;
        this.serverDialog = serverDialog;
        this.serverStore = new BootstrapServerStore();
    }

    public Widget asWidget() {
        FlowPanel content = new FlowPanel();
        content.add(new ContentHeaderLabel("Configure Management Interface"));
        content.add(new ContentDescription("Enter the name and URL for a management interface."));

        final HTML configureStatus = new HTML();

        form = new Form<BootstrapServer>(BootstrapServer.class);
        nameItem = new TextBoxItem("name", "Name");
        nameItem.getInputElement().setAttribute("style", "-moz-box-sizing:border-box;box-sizing:border-box;width:100%;");
        nameItem.getInputElement().setAttribute("placeholder", "A name for this management interface");
        final UrlItem urlItem = new UrlItem("url", "URL");
        ButtonItem pingItem = new ButtonItem("", "", "Ping");
        pingItem.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(final ClickEvent event) {
                boolean valid = urlItem.validate(urlItem.getValue());
                urlItem.setErroneous(!valid);
                if (valid) {
                    configureStatus.setVisible(false);
                    BootstrapServer server = form.getUpdatedEntity();
                    serverSetup.pingServer(server, new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(final Throwable caught) {
                            configureStatus.setHTML(StatusMessage.warning("The management interface does not respond."));
                            configureStatus.setVisible(true);
                        }

                        @Override
                        public void onSuccess(final Void result) {
                            configureStatus.setHTML(StatusMessage.success("The management interface is running."));
                            configureStatus.setVisible(true);
                        }
                    });
                }
            }
        });
        form.setFields(nameItem, urlItem, pingItem);

        content.add(form);
        content.add(configureStatus);

        DialogueOptions options = new DialogueOptions(
                "Add",
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        FormValidation validation = form.validate();
                        if (!validation.hasErrors()) {
                            configureStatus.setVisible(false);
                            BootstrapServer newServer = form.getUpdatedEntity();

                            boolean sameName = false;
                            List<BootstrapServer> servers = serverStore.load();
                            for (BootstrapServer server : servers) {
                                if (server.getName().equals(newServer.getName())) {
                                    sameName = true;
                                    break;
                                }
                            }
                            if (sameName) {
                                configureStatus.setHTML(StatusMessage.error(
                                        "An entry with this name already exists. Please choose another name."));
                                nameItem.getInputElement().focus();
                            } else {
                                serverStore.add(newServer);
                                serverDialog.onConfigureOk();
                            }
                        }
                    }
                },
                "Cancel",
                new ClickHandler() {
                    @Override
                    public void onClick(final ClickEvent event) {
                        serverDialog.onConfigureCancel();
                    }
                }
        );

        return new WindowContentBuilder(content, options).build();
    }

    void reset() {
        form.clearValues();
        nameItem.getInputElement().focus();
    }
}
