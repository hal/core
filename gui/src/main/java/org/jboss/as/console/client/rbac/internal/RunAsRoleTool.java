package org.jboss.as.console.client.rbac.internal;

import static org.jboss.as.console.client.shared.Preferences.Key.RUN_AS_ROLE;

import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.jboss.as.console.client.Console;
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



        StaticHelpPanel help = new StaticHelpPanel(
                "<table style='vertical-align:top' cellpadding=3>\n" +
                        "<tr>\n" +
                        "<td>\n" +
                        "Monitor\n" +
                        "</td>\n" +
                        "<td>\n" +
                        "The monitor role has the fewest permissions and restricts the user to viewing the configuration and the current state. The monitor role does not have permission to view sensitive data.\n" +
                        "</td></tr>\n" +
                        "\n" +
                        "<tr>\n" +
                        "<td>\n" +
                        "Configurator\n" +
                        "</td>\n" +
                        "<td>\n" +
                        "The configurator role has the same permissions as the monitor role, and can change the persistent configuration. For example, the configurator can deploy an application. A configurator can change the application level security settings. The configurator role does not have permission to view sensitive data.\n" +
                        "</td></tr>\n" +
                        "\n" +
                        "<tr>\n" +
                        "<td>\n" +
                        "Operator\n" +
                        "</td>\n" +
                        "<td>\n" +
                        "The operator role has monitor permissions and can also change the runtime state but not the persistent configuration. For example, the operator can start or stop servers.The operator role does not have permission to view sensitive data.\n" +
                        "</td></tr>\n" +
                        "\n" +
                        "<tr>\n" +
                        "<td>\n" +
                        "Maintainer\n" +
                        "</td>\n" +
                        "<td>\n" +
                        "The Maintainer role has the same permissions as the Operator role, and additionally can change the persistent configuration. For example, the Maintainer can deploy an application. The maintainer role does not have permission to view or modify sensitive data.\n" +
                        "</td></tr>\n" +
                        "\n" +
                        "<tr>\n" +
                        "<td>\n" +
                        "Deployer\n" +
                        "</td>\n" +
                        "<td>\n" +
                        "The Deployer role has the permissions of the Maintainer, but with those permissions constrained to operating on application resources.\n" +
                        "</td></tr>\n" +
                        "\n" +
                        "<tr>\n" +
                        "<td>\n" +
                        "Administrator\n" +
                        "</td>\n" +
                        "<td>\n" +
                        "The Administrator role has the permissions of the Maintainer. This role also has permission to view and modify sensitive data, including passwords, but excluding the management security auditing system. The Administrator role can modify administrative users and roles. \n" +
                        "</td></tr>\n" +
                        "\n" +
                        "<tr>\n" +
                        "<td>\n" +
                        "Auditor\n" +
                        "</td>\n" +
                        "<td>\n" +
                        "The Auditor role can view and modify the configuration settings for the management security auditing system. The Auditor role includes the Monitor role, and also allows the Auditor to view but not change the rest of the security configuration.\n" +
                        "</td></tr>\n" +
                        "\n" +
                        "<tr>\n" +
                        "<td>\n" +
                        "SuperUser\n" +
                        "</td>\n" +
                        "<td>\n" +
                        "The SuperUser role has the combined permissions of the Administrator and Auditor roles. This role has all available permissions.\n" +
                        "</td></tr>\n" +
                        "</table>\n");

        panel.add(help.asWidget());


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
