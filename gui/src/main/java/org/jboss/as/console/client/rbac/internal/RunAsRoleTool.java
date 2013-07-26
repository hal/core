package org.jboss.as.console.client.rbac.internal;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.rbac.RolesHelpPanel;
import org.jboss.as.console.client.rbac.StandardRole;
import org.jboss.as.console.client.shared.Preferences;
import org.jboss.as.console.client.shared.help.StaticHelpPanel;
import org.jboss.as.console.client.tools.Tool;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.ListBoxItem;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import java.util.List;

import static org.jboss.as.console.client.shared.Preferences.Key.RUN_AS_ROLE;


/**
 * @author Harald Pehl
 * @date 07/02/2013
 */
public class RunAsRoleTool implements Tool {

    private DefaultWindow window;
    private ListBoxItem role;

    @Override
    public void launch() {
        if (window == null) {
            setupWindow();
        }
        String savedRole = Preferences.get(RUN_AS_ROLE);
        if (savedRole != null) {
            role.setValue(savedRole);
        }
        window.center();
    }

    @Override
    public void dispose() {
        if (window != null) {
            window.hide();
        }
    }

    private void setupWindow() {
        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("window-content");
        panel.add(new RolesHelpPanel().asWidget());
        panel.add(new ContentHeaderLabel("Select Role"));
        panel.add(new ContentDescription("Select the role you want to act on their behalf."));

        Form<Object> form = new Form<Object>(Object.class);
        role = new ListBoxItem("role", "Role");

        List<String> roleNames = StandardRole.getRoleNames();
        roleNames.add("No preselection");
        role.setChoices(roleNames, "No preselection");
        form.setFields(role);
        panel.add(form.asWidget());

        ClickHandler runAsHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                runAs(role.getValue());
            }
        };
        ClickHandler cancelHandler = new
                ClickHandler() {
                    @Override
                    public void onClick(ClickEvent clickEvent) {
                        window.hide();
                    }
                };
        DialogueOptions options = new DialogueOptions(
                "Run As", runAsHandler, "Cancel", cancelHandler);

        window = new DefaultWindow("Run as Role");
        window.setWidth(480);
        window.setHeight(300);
        window.trapWidget(new WindowContentBuilder(panel, options).build());
        window.setModal(true);
        window.setGlassEnabled(true);
    }

    private void runAs(final String role) {
        window.hide();

        String oldRole = Preferences.get(RUN_AS_ROLE);
        if ((oldRole == null && role.equals("No preselection")) || role.equals(oldRole)) {
            return;
        }

        if (role.equals("No preselection")) {
            Preferences.clear(RUN_AS_ROLE);
        } else {
            Preferences.set(RUN_AS_ROLE, role);
        }
        Feedback.confirm(Console.MESSAGES.restartRequired(), Console.MESSAGES.restartRequiredConfirm(),
                new Feedback.ConfirmationHandler() {
                    @Override
                    public void onConfirmation(boolean isConfirmed) {
                        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                            @Override
                            public void execute() {
                                Window.Location.reload();
                            }
                        });
                    }
                });
    }
}
