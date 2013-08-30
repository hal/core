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

import static com.google.gwt.dom.client.Style.Unit.PX;
import static com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.RoleKey;
import org.jboss.as.console.client.rbac.Role;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;

/**
 * @author Harald Pehl
 */
public class RolesFormItem extends FormItem<List<Role>> {

    private final List<Role> value;
    private DefaultCellTable<Role> table;
    private ListDataProvider<Role> dataProvider;
    private MultiSelectionModel<Role> selectionModel;

    public RolesFormItem(final String name, final String title) {
        super(name, title);
        this.value = new ArrayList<Role>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget asWidget() {

        // table
        RoleKey<Role> keyProvider = new RoleKey<Role>();
        table = new DefaultCellTable<Role>(6, keyProvider);
        table.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.ENABLED);
        dataProvider = new ListDataProvider<Role>(keyProvider);
        dataProvider.addDataDisplay(table);
        selectionModel = new MultiSelectionModel<Role>(keyProvider);
        table.setSelectionModel(selectionModel, DefaultSelectionEventManager.<Role>createCheckboxManager());

        // columns
        Column<Role, Boolean> checkColumn = new Column<Role, Boolean>(new CheckboxCell(true, false)) {
            @Override
            public Boolean getValue(Role role) {
                // Get the value from the selection model.
                return selectionModel.isSelected(role);
            }
        };
        Column<Role, String> nameColumn = new Column<Role, String>(new TextCell()) {
            @Override
            public String getValue(Role role) {
                return role.getName();
            }
        };
        table.addColumn(checkColumn, SafeHtmlUtils.fromSafeConstant("<br/>"));
        table.setColumnWidth(checkColumn, 40, PX);
        table.addColumn(nameColumn, Console.CONSTANTS.common_label_name());

        // pager
        DefaultPager pager = new DefaultPager();
        pager.setWidth("auto");
        pager.setDisplay(table);

        // panels
        VerticalPanel content = new VerticalPanel();
        content.setStyleName("fill-layout-width");
        DOM.setStyleAttribute(content.getElement(), "width", "97%");
        content.add(table);
        content.add(pager);
        HorizontalPanel wrapper = new HorizontalPanel();
        wrapper.add(content);
        content.getElement().getParentElement().setAttribute("class", "form-input");
        return wrapper;
    }

    public void setRoles(List<Role> roles) {
        if (dataProvider != null) {
            dataProvider.setList(roles);
            selectionModel.clear();
            for (Role role : value) {
                selectionModel.setSelected(role, true);
            }
        }
    }

    @Override
    public void setEnabled(final boolean b) {
    }

    @Override
    public boolean validate(final List<Role> value) {
        return true;
    }

    @Override
    public void clearValue() {
        selectionModel.clear();
    }

    @Override
    public List<Role> getValue() {
        return value;
    }

    @Override
    public void setValue(final List<Role> value) {
        this.value.clear();
        this.value.addAll(value);
        selectionModel.clear();
        for (Role role : this.value) {
            selectionModel.setSelected(role, true);
        }
    }

    @Override
    public String asString() {
        StringBuilder builder = new StringBuilder("[");
        for (Iterator<Role> iterator = value.iterator(); iterator.hasNext(); ) {
            Role role = iterator.next();
            builder.append(role.getName());
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append("]");
        return builder.toString();
    }
}
