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

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.administration.role.model.Role;
import org.jboss.as.console.client.core.EnumLabelLookup;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;

/**
 * @author Harald Pehl
 */
public class ScopedRoleTable extends RoleTable {

    public ScopedRoleTable() {
        super(5);
    }

    @Override
    public Widget asWidget() {
        VerticalPanel content = (VerticalPanel) super.asWidget();
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(getCellTable());
        content.add(pager);

        return content;
    }

    @Override
    protected void additionalColumns(final CellTable<Role> table) {
        TextColumn<Role> typeColumn = new TextColumn<Role>() {
            @Override
            public String getValue(Role role) {
                return EnumLabelLookup.labelFor("ScopeType", role.getType());
            }
        };
        TextColumn<Role> baseRoleColumn = new TextColumn<Role>() {
            @Override
            public String getValue(Role role) {
                return role.getBaseRole().getId();
            }
        };
        Column<Role, Collection<String>> scopeColumn = new Column<Role, Collection<String>>(new ScopeCell()) {
            @Override
            public Collection<String> getValue(final Role scopedRole) {
                return scopedRole.getScope();
            }
        };
        table.addColumn(baseRoleColumn, "Based On");
        table.addColumn(typeColumn, "Type");
        table.addColumn(scopeColumn, "Scope");
    }
}
