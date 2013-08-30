package org.jboss.as.console.client.administration.role;

import static org.jboss.as.console.client.administration.role.model.PrincipalType.GROUP;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.PrincipalStore;
import org.jboss.as.console.client.administration.role.model.PrincipalType;
import org.jboss.as.console.client.administration.role.model.RoleAssignmentStore;
import org.jboss.as.console.client.administration.role.model.RoleStore;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.ContentGroupLabel;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;

/**
 * @author Harald Pehl
 */
public class RoleAssignmentEditor implements IsWidget {

    private final PrincipalType type;
    private final RoleAssignmentPresenter presenter;
    private final BeanFactory beanFactory;
    private RoleAssignmentTable table;
    private RoleAssignmentDetails details;

    public RoleAssignmentEditor(final PrincipalType type, final RoleAssignmentPresenter presenter,
            final BeanFactory beanFactory) {
        this.presenter = presenter;
        this.type = type;
        this.beanFactory = beanFactory;
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
            header = Console.CONSTANTS.common_label_groups();
            description = Console.CONSTANTS.administration_group_assignment();
        } else {
            header = Console.CONSTANTS.common_label_users();
            description = Console.CONSTANTS.administration_user_assignment();
        }
        content.add(new ContentHeaderLabel(header));
        content.add(new ContentDescription(description));

        // toolstrip
        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.onAddRoleAssignment(type);
            }
        }));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.onRemoveRoleAssignment(table.getSelectedAssignment());
            }
        }));
        if (type == GROUP) {
            tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_exclude(), new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    presenter.onExcludeUsers(table.getSelectedAssignment());
                }
            }));
        }
        content.add(tools.asWidget());

        // table
        table = new RoleAssignmentTable(type);
        content.add(table);

        // details
        details = new RoleAssignmentDetails(type, presenter, beanFactory);
        details.bind(table.getCellTable());
        content.add(new ContentGroupLabel(Console.CONSTANTS.common_label_selection()));
        content.add(details);

        return layout;
    }

    public void update(final PrincipalStore principals, final RoleAssignmentStore assignments, final RoleStore roles) {
        if (table != null && details != null) {
            table.setAssignments(assignments);
            details.update(principals, assignments, roles);
        }
    }
}
