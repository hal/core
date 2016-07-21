package org.jboss.as.console.client.domain;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.impl.LifecycleOperation;
import org.jboss.as.console.client.domain.runtime.DomainRuntimePresenter;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

/**
 * @author Claudio Miranda
 */
public class ServerStopDialogue {
    
    private DomainRuntimePresenter presenter;
    private final Server server;

    public ServerStopDialogue(final DomainRuntimePresenter presenter, final Server server) {
        this.presenter = presenter;
        this.server = server;
    }

    public Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");

        NumberBoxItem timeout = new NumberBoxItem("timeout", "Timeout", true);
        timeout.setValue(0);
        final Form<Void> form = new Form<Void>(Void.class);
        form.setFields(timeout);

        DialogueOptions options = new DialogueOptions(
                "Stop Server",
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        FormValidation validation = form.validate();
                        if (validation.hasErrors())
                            return;

                        Map<String, Object> params = new HashMap<>();
                        params.put("timeout", timeout.getValue());
                        presenter.onServerInstanceLifecycle(server.getHostName(), server.getName(), params, LifecycleOperation.STOP);
                    }
                },

                "Cancel",
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        presenter.closeDialoge();
                    }
                }
        );

        // ----------------------------------------

        Widget formWidget = form.asWidget();

        layout.add(new HTML("<h3> Stop server " + server.getName() + " ?</h3>"));
        layout.add(new ContentDescription(Console.CONSTANTS.stopTimeoutDescription()));
        layout.add(formWidget);
        return new WindowContentBuilder(layout, options).build();
    }

}
