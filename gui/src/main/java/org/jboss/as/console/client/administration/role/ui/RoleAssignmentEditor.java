package org.jboss.as.console.client.administration.role.ui;

import static org.jboss.as.console.client.administration.role.model.Principal.Type.GROUP;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.RoleAssignmentPresenter;
import org.jboss.as.console.client.administration.role.model.Principal;
import org.jboss.as.console.client.administration.role.model.RoleAssignments;
import org.jboss.as.console.client.administration.role.model.Roles;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.ContentGroupLabel;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;

/**
 * @author Harald Pehl
 */
public class RoleAssignmentEditor implements IsWidget {

    private final Principal.Type type;
    private final RoleAssignmentPresenter presenter;
    private RoleAssignmentTable table;
    private RoleAssignmentDetails details;

    public RoleAssignmentEditor(final RoleAssignmentPresenter presenter, final Principal.Type type) {
        this.presenter = presenter;
        this.type = type;
    }

    @Override
    public Widget asWidget() {
        // container and panels
        LayoutPanel layout = new LayoutPanel();
        VerticalPanel content = new VerticalPanel();
        content.setStyleName("rhs-content-panel");
        ScrollPanel scroll = new ScrollPanel(content);
        layout.add(scroll);
        layout.setWidgetTopHeight(scroll, 0, Style.Unit.PX, 100, Style.Unit.PCT);

        // header and desc
        String header;
        String description;
        if (type == GROUP) {
            header = "Groups";
            description = Console.CONSTANTS.administration_group_assignment();
        } else {
            header = "Users";
            description = Console.CONSTANTS.administration_user_assignment();
        }
        content.add(new ContentHeaderLabel(header));
        content.add(new ContentDescription(description));

        // toolstrip
        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.launchAddRoleAssignmentWizard(type);
            }
        }));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Feedback.confirm(Console.MESSAGES.deleteTitle("Role Assignment"),
                        Console.MESSAGES.deleteConfirm("role assignment"),
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    presenter.removeRoleAssignment(table.getSelectedAssignment());
                                }
                            }
                        });
            }
        }));
        content.add(tools.asWidget());

        // table
        table = new RoleAssignmentTable(type);
        content.add(table);

        // details
        details = new RoleAssignmentDetails(presenter, type);
        details.bind(table.getCellTable());
        content.add(new ContentGroupLabel(Console.CONSTANTS.common_label_selection()));
        content.add(details);

        return layout;
    }

    public void update(final RoleAssignments assignments, final Roles roles) {
        if (table != null && details != null) {
            table.update(assignments);
            details.update(assignments, roles, table.getSelectedAssignment());
        }
    }
}
