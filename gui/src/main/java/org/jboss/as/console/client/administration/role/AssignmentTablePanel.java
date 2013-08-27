package org.jboss.as.console.client.administration.role;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;

/**
 * @author Harald Pehl
 * @date 08/21/2013
 */
public class AssignmentTablePanel implements IsWidget {

    private final boolean standalone;

    public AssignmentTablePanel(final boolean standalone) {
        this.standalone = standalone;
    }

    @Override
    public Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width");

        RoleAssignment.Key roleAssignmentKey = new RoleAssignment.Key();
        DefaultCellTable<RoleAssignment> table = new DefaultCellTable<RoleAssignment>(10, roleAssignmentKey);
        ListDataProvider<RoleAssignment> dataProvider = new ListDataProvider<RoleAssignment>(roleAssignmentKey);
        dataProvider.addDataDisplay(table);
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        layout.add(pager);


        return null;
    }
}
