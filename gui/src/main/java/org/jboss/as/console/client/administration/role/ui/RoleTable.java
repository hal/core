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
package org.jboss.as.console.client.administration.role.ui;

import java.util.Collection;
import java.util.List;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.Role;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;

/**
 * @author Harald Pehl
 */
public abstract class RoleTable implements IsWidget {

    private final int pageSize;
    private DefaultCellTable<Role> table;
    private ListDataProvider<Role> dataProvider;
    private SingleSelectionModel<Role> selectionModel;

    protected RoleTable(int pageSize) {this.pageSize = pageSize;}

    @Override
    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        ProvidesKey<Role> keyProvider = new Role.Key();
        table = new DefaultCellTable<Role>(pageSize, keyProvider);
        dataProvider = new ListDataProvider<Role>(keyProvider);
        dataProvider.addDataDisplay(table);
        selectionModel = new SingleSelectionModel<Role>(keyProvider);
        table.setSelectionModel(selectionModel);

        // first column: name
        TextColumn<Role> nameColumn = new TextColumn<Role>() {
            @Override
            public String getValue(Role role) {
                return role.getName();
            }
        };
        table.addColumn(nameColumn, Console.CONSTANTS.common_label_name());
        // additional columns: subclasses turn
        additionalColumns(table);
        // last column: include all
        Column<Role, Boolean> includeAllColumn = new Column<Role, Boolean>(new IncludeAllCell()) {
            @Override
            public Boolean getValue(final Role role) {
                return role.isIncludeAll();
            }
        };
        table.addColumn(includeAllColumn, "Include All");

        VerticalPanel content = new VerticalPanel();
        content.setStyleName("fill-layout-width");
        content.add(table);
        return content;
    }

    protected void additionalColumns(CellTable<Role> table) {

    }

    public void update(final List<Role> roles) {
        dataProvider.setList(roles);
        table.selectDefaultEntity();
    }

    public Role getSelectedRole() {
        return selectionModel.getSelectedObject();
    }

    @SuppressWarnings("unchecked")
    CellTable<Role> getCellTable() {
        return table;
    }

    protected static class ScopeCell extends AbstractCell<Collection<String>> {

        @Override
        public void render(final Context context, final Collection<String> scopes, final SafeHtmlBuilder builder) {
            builder.appendEscaped(UIHelper.csv(scopes));
        }
    }

    protected static class IncludeAllCell extends AbstractCell<Boolean> {

        @Override
        public void render(final Context context, final Boolean includeAll, final SafeHtmlBuilder builder) {
            if (includeAll) {
                builder.appendHtmlConstant("<i class=\"icon-ok\"></i>");
            } else {
                builder.appendHtmlConstant("&nbsp;");
            }
        }
    }
}
