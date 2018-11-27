package org.jboss.as.console.client.domain;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
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
 * @author Heiko Braun
 * @date 11/28/11
 */
public class ServerSuspendDialogue {
    private DomainRuntimePresenter presenter;
    private final Server server;

    public ServerSuspendDialogue(final DomainRuntimePresenter presenter, final Server server) {
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
                // save
                "Suspend Server",
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        // merge base
                        FormValidation validation = form.validate();
                        if (validation.hasErrors())
                            return;

                        Map<String, Object> params = new HashMap<>();
                        params.put("timeout", timeout.getValue());
                        presenter.onServerInstanceLifecycle(server.getHostName(), server.getName(), params, LifecycleOperation.SUSPEND);
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

        layout.add(new HTML("<h3> Suspend server " + SafeHtmlUtils.fromString(server.getName()).asString() + "?</h3>"));
        layout.add(new ContentDescription(Console.CONSTANTS.suspendTimeoutDescription()));
        layout.add(formWidget);
        return new WindowContentBuilder(layout, options).build();
    }

}
