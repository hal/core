/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.console.client.administration.role;

import static org.jboss.as.console.client.administration.role.model.PrincipalType.GROUP;
import static org.jboss.as.console.client.administration.role.model.PrincipalType.USER;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.Principal;
import org.jboss.as.console.client.administration.role.model.PrincipalType;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.administration.role.model.RoleAssignmentKey;
import org.jboss.as.console.client.administration.role.model.RoleAssignments;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;

/**
 * @author Harald Pehl
 */
public class RoleAssignmentTable implements IsWidget {

    private final PrincipalType type;
    private DefaultCellTable<RoleAssignment> table;
    private ListDataProvider<RoleAssignment> dataProvider;
    private SingleSelectionModel<RoleAssignment> selectionModel;

    public RoleAssignmentTable(final PrincipalType type) {this.type = type;}

    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        VerticalPanel content = new VerticalPanel();
        content.setStyleName("fill-layout-width");

        // table
        RoleAssignmentKey keyProvider = new RoleAssignmentKey();
        table = new DefaultCellTable<RoleAssignment>(5, keyProvider);
        dataProvider = new ListDataProvider<RoleAssignment>(keyProvider);
        dataProvider.addDataDisplay(table);
        selectionModel = new SingleSelectionModel<RoleAssignment>(keyProvider);
        table.setSelectionModel(selectionModel);

        // columns
        Column<RoleAssignment, Principal> principalColumn =
                new Column<RoleAssignment, Principal>(CellFactory.newPrincipalCell()) {
                    @Override
                    public Principal getValue(final RoleAssignment assignment) {
                        return assignment.getPrincipal();
                    }
                };
        Column<RoleAssignment, RoleAssignment> roleColumn =
                new Column<RoleAssignment, RoleAssignment>(CellFactory.newRolesCell()) {
                    @Override
                    public RoleAssignment getValue(final RoleAssignment assignment) {
                        return assignment;
                    }
                };
        table.addColumn(principalColumn,
                type == GROUP ? Console.CONSTANTS.common_label_group() : Console.CONSTANTS.common_label_user());
        table.addColumn(roleColumn, Console.CONSTANTS.common_label_roles());
        content.add(table);

        // pager
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        content.add(pager);

        return content;
    }

    public void update(final RoleAssignments assignments) {
        if (type == GROUP) {
            dataProvider.setList(assignments.getGroupAssignments());
        } else if (type == USER) {
            dataProvider.setList(assignments.getUserAssignments());
        }
        table.selectDefaultEntity();
    }

    public RoleAssignment getSelectedAssignment() {
        if (selectionModel != null) {
            return selectionModel.getSelectedObject();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    CellTable<RoleAssignment> getCellTable() {
        return table;
    }
}
