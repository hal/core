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

import java.util.List;

import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.ScopedRole;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;

/**
 * @author Harald Pehl
 */
public class ScopedRoleTable implements IsWidget {

    private DefaultCellTable<ScopedRole> table;
    private ListDataProvider<ScopedRole> dataProvider;
    private SingleSelectionModel<ScopedRole> selectionModel;

    @Override
    public Widget asWidget() {
        VerticalPanel content = new VerticalPanel();
        content.setStyleName("fill-layout-width");

        // table
        table = new DefaultCellTable<ScopedRole>(5, new ScopedRole.Key());
        dataProvider = new ListDataProvider<ScopedRole>(new ScopedRole.Key());
        dataProvider.addDataDisplay(table);
        selectionModel = new SingleSelectionModel<ScopedRole>(new ScopedRole.Key());
        table.setSelectionModel(selectionModel);

        // columns
        TextColumn<ScopedRole> nameColumn = new TextColumn<ScopedRole>() {
            @Override
            public String getValue(ScopedRole role) {
                return role.getName();
            }
        };

        table.addColumn(nameColumn, Console.CONSTANTS.common_label_name());
        content.add(table);

        // pager
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        content.add(pager);

        return content;
    }

    public void setRoles(final List<ScopedRole> roles) {
        dataProvider.setList(roles);
        table.selectDefaultEntity();
    }
}
