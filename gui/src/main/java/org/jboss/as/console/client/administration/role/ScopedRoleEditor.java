package org.jboss.as.console.client.administration.role;

import java.util.List;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
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
public class ScopedRoleEditor implements IsWidget {

    private final RoleAssignmentPresenter presenter;
    private ScopedRoleTable table;
    private ScopedRoleDetails details;

    public ScopedRoleEditor(final RoleAssignmentPresenter presenter) {
        this.presenter = presenter;
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
        content.add(new ContentHeaderLabel(Console.CONSTANTS.administration_scoped_roles()));
        content.add(new ContentDescription(Console.CONSTANTS.administration_scoped_roles_desc()));

        // toolstrip
        ToolStrip tools = new ToolStrip();
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
        content.add(tools.asWidget());

        // table
        table = new ScopedRoleTable();
        content.add(table);

        // details
        details = new ScopedRoleDetails(presenter);
        details.bind(table.getCellTable());
        content.add(new ContentGroupLabel(Console.CONSTANTS.common_label_selection()));
        content.add(details);

        return layout;
    }

    public void update(final Roles roles, final List<String> hosts, final List<String> serverGroups) {
        if (!presenter.isStandalone()) {
            table.update(roles.getScopedRoles());
            details.update(roles.getScopedRoles(), hosts, serverGroups, table.getSelectedRole());
        }
    }
}
