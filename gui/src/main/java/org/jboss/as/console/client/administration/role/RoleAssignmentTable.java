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

import static org.jboss.as.console.client.administration.role.model.Principal.Type.GROUP;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.Principal;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.administration.role.model.ScopedRole;
import org.jboss.as.console.client.rbac.Role;
import org.jboss.as.console.client.rbac.StandardRole;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;

/**
 * @author Harald Pehl
 */
public class RoleAssignmentTable implements IsWidget {

    final static RoleAssignmentTemplates TEMPLATES = GWT.create(RoleAssignmentTemplates.class);
    private final Principal.Type type;
    private DefaultCellTable<RoleAssignment> table;
    private ListDataProvider<RoleAssignment> dataProvider;
    private SingleSelectionModel<RoleAssignment> selectionModel;

    public RoleAssignmentTable(final Principal.Type type) {this.type = type;}

    public Widget asWidget() {
        VerticalPanel content = new VerticalPanel();
        content.setStyleName("fill-layout-width");

        // table
        table = new DefaultCellTable<RoleAssignment>(5, new RoleAssignment.Key());
        dataProvider = new ListDataProvider<RoleAssignment>(new RoleAssignment.Key());
        dataProvider.addDataDisplay(table);
        selectionModel = new SingleSelectionModel<RoleAssignment>(new RoleAssignment.Key());
        table.setSelectionModel(selectionModel);

        // columns
        TextColumn<RoleAssignment> nameColumn = new TextColumn<RoleAssignment>() {
            @Override
            public String getValue(RoleAssignment assignment) {
                return assignment.getPrincipal().getName();
            }
        };
        Column<RoleAssignment, RoleAssignment> roleColumn = new

                Column<RoleAssignment, RoleAssignment>(new RoleCell()) {
                    @Override
                    public RoleAssignment getValue(final RoleAssignment assignment) {
                        return assignment;
                    }
                };
        table.addColumn(nameColumn,
                type == GROUP ? Console.CONSTANTS.common_label_group() : Console.CONSTANTS.common_label_user());
        table.addColumn(roleColumn, Console.CONSTANTS.common_label_roles());
        if (type == GROUP) {
            TextColumn<RoleAssignment> excludeColumn = new TextColumn<RoleAssignment>() {
                @Override
                public String getValue(final RoleAssignment assignment) {
                    StringBuilder excludes = new StringBuilder();
                    for (Iterator<Principal> iterator = assignment.getExcludes().iterator(); iterator.hasNext(); ) {
                        Principal principal = iterator.next();
                        excludes.append(principal.getName());
                        if (iterator.hasNext()) {
                            excludes.append(", ");
                        }
                    }
                    return excludes.toString();
                }
            };
            table.addColumn(excludeColumn, Console.CONSTANTS.common_label_exclude());
        }
        content.add(table);

        // pager
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        content.add(pager);

        return content;
    }

    public void setAssignments(final List<RoleAssignment> assignments) {
        dataProvider.setList(assignments);
        table.selectDefaultEntity();
    }

    interface RoleAssignmentTemplates extends SafeHtmlTemplates {

        @Template("<div>{0}</div>")
        SafeHtml role(SafeHtml role);

        @Template("<div>{0}<span class=\"role_scope\">Scoped to {1}</span></div>")
        SafeHtml scopedRole(SafeHtml role, SafeHtml scope);
    }

    static class RoleCell extends AbstractCell<RoleAssignment> {

        @Override
        public void render(final Context context, final RoleAssignment assignment, final SafeHtmlBuilder builder) {
            List<Role> roles = assignment.getRoles();
            for (Role role : roles) {
                if (role instanceof StandardRole) {
                    builder.append(TEMPLATES.role(SafeHtmlUtils.fromString(role.getName())));
                } else if (role instanceof ScopedRole) {
                    ScopedRole scopedRole = (ScopedRole) role;
                    StringBuilder scopes = new StringBuilder();
                    for (Iterator<String> iterator = scopedRole.getScope().iterator(); iterator.hasNext(); ) {
                        String scope = iterator.next();
                        scopes.append(scope);
                        if (iterator.hasNext()) {
                            scopes.append(", ");
                        }
                    }
                    builder.append(TEMPLATES.scopedRole(SafeHtmlUtils.fromString(role.getName()),
                            SafeHtmlUtils.fromString(scopes.toString())));
                }
            }
        }
    }
}
