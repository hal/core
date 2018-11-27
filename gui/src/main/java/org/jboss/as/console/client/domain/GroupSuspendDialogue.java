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
import org.jboss.as.console.client.domain.hosts.HostMgmtPresenter;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.model.impl.LifecycleOperation;
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
public class GroupSuspendDialogue {
    private HostMgmtPresenter presenter;
    private final ServerGroupRecord group;

    public GroupSuspendDialogue(final HostMgmtPresenter presenter, final ServerGroupRecord group) {
        this.presenter = presenter;
        this.group = group;
    }

    public Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");

        NumberBoxItem timeout = new NumberBoxItem("timeout", "Timeout", true);
        timeout.setValue(0);

        final Form<Void> form = new Form<Void>(Void.class);
        form.setFields(timeout);

        DialogueOptions options = new DialogueOptions(
                "Suspend Group",
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        // merge base
                        FormValidation validation = form.validate();
                        if (validation.hasErrors())
                            return;

                        Map<String, Object> params = new HashMap<>();
                        params.put("timeout", timeout.getValue());
                        presenter.onGroupLifecycle(group.getName(), params, LifecycleOperation.SUSPEND);
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
        layout.add(new HTML("<h3> Suspend group " + SafeHtmlUtils.fromString(group.getName()).asString() + "?</h3>"));
        layout.add(
                new ContentDescription(
                        Console.CONSTANTS.suspendTimeoutDescription()
                )
        );
        layout.add(formWidget);
        return new WindowContentBuilder(layout, options).build();
    }

}
