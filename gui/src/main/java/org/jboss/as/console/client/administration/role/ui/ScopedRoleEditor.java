package org.jboss.as.console.client.administration.role.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.RoleAssignmentPresenter;
import org.jboss.as.console.client.administration.accesscontrol.store.Roles;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.ContentGroupLabel;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;

import java.util.List;

/**
 * @author Harald Pehl
 */
public class ScopedRoleEditor implements IsWidget {

    private final RoleAssignmentPresenter presenter;
    private final RoleTable table;
    private final ScopedRoleDetails details;

    public ScopedRoleEditor(final RoleAssignmentPresenter presenter) {
        this.presenter = presenter;
        this.table = new ScopedRoleTable();
        this.details = new ScopedRoleDetails(presenter);
    }

    @Override
    public Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("fill-layout");

        // header and desc
        layout.add(new ContentDescription(Console.CONSTANTS.administration_scoped_roles_desc()));

        // toolstrip
        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.administration_members(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.showMembers(table.getSelectedRole());
            }
        }));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.launchAddScopedRoleWizard();
            }
        }));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Feedback.confirm(Console.MESSAGES.deleteTitle("Scoped Role"),
                        Console.MESSAGES.deleteConfirm("scoped role"),
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    presenter.removeScopedRole(table.getSelectedRole());
                                }
                            }
                        });
            }
        }));
        layout.add(tools.asWidget());

        // table
        layout.add(table);

        // details
        details.bind(table.getCellTable());
        layout.add(new ContentGroupLabel(Console.CONSTANTS.common_label_selection()));
        layout.add(details);

        return layout;
    }

    public void update(final Roles roles, final List<String> hosts, final List<String> serverGroups) {
        if (!presenter.isStandalone()) {
//            table.update(roles.getScopedRoles());
//            details.update(roles.getScopedRoles(), hosts, serverGroups, table.getSelectedRole());
        }
    }
}
