package org.jboss.as.console.client.administration.role;

import static org.jboss.as.console.client.administration.role.model.PrincipalType.GROUP;

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
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;

/**
 * @author Harald Pehl
 */
public class GroupEditor implements IsWidget {

    private final RoleAssignmentPresenter presenter;
    private RoleAssignmentTable table;

    public GroupEditor(final RoleAssignmentPresenter presenter) {
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
        content.add(new ContentHeaderLabel(Console.CONSTANTS.common_label_groups()));
        content.add(new ContentDescription(Console.CONSTANTS.administration_group_assignment()));

        // toolstrip
        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
            }
        }));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
            }
        }));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_exclude(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
            }
        }));
        content.add(tools.asWidget());

        // table
        table = new RoleAssignmentTable(GROUP);
        content.add(table);

        return layout;
    }

    public void setAssignments(final List<RoleAssignment> assignments) {
        table.setAssignments(assignments);
    }
}
