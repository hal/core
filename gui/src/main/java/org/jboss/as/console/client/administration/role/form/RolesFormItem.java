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
package org.jboss.as.console.client.administration.role.form;

import com.google.common.collect.Lists;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.*;
import com.google.gwt.view.client.DefaultSelectionEventManager.SelectAction;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.accesscontrol.store.Role;
import org.jboss.as.console.client.administration.accesscontrol.store.Roles;
import org.jboss.as.console.client.administration.role.ui.UIHelper;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.google.gwt.dom.client.Style.Unit.PX;

/**
 * @author Harald Pehl
 */
public class RolesFormItem extends FormItem<List<Role>> {

    private final int pageSize;
    private final List<Role> value;
    private final ProvidesKey<Role> keyProvider;
    private final ListDataProvider<Role> dataProvider;
    private final MultiSelectionModel<Role> selectionModel;
    private FormItemPanelWrapper wrapper;

    public RolesFormItem(final String name, final String title) {
        this(name, title, 7);
    }

    public RolesFormItem(final String name, final String title, int pageSize) {
        super(name, title);

        this.pageSize = pageSize;
        this.value = new ArrayList<Role>();
        this.keyProvider = new Role.Key();
        this.dataProvider = new ListDataProvider<Role>(keyProvider);
        this.selectionModel = new MultiSelectionModel<Role>(keyProvider);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        // table
        DefaultCellTable<Role> table = new DefaultCellTable<Role>(pageSize, keyProvider);
        table.setSelectionModel(selectionModel, DefaultSelectionEventManager.createCustomManager(
                new DefaultSelectionEventManager.CheckboxEventTranslator<Role>() {
                    @Override
                    public SelectAction translateSelectionEvent(CellPreviewEvent<Role> event) {
                        SelectAction action = super.translateSelectionEvent(event);
                        if (action.equals(SelectAction.IGNORE)) {
                            Role role = event.getValue();
                            boolean selected = selectionModel.isSelected(role);
                            return selected ? SelectAction.DESELECT : SelectAction.SELECT;
                        }
                        return action;
                    }
                }));
        dataProvider.addDataDisplay(table);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(final SelectionChangeEvent event) {
                value.clear();
                value.addAll(selectionModel.getSelectedSet());
                setModified(true);
            }
        });

        // columns
        Column<Role, Boolean> checkColumn = new Column<Role, Boolean>(new CheckboxCell(true, false)) {
            @Override
            public Boolean getValue(Role role) {
                // Get the value from the selection model.
                return selectionModel.isSelected(role);
            }
        };
        TextColumn<Role> nameColumn = new TextColumn<Role>() {
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
        content.setWidth("95%");
        content.add(table);
        content.add(pager);
        wrapper = new FormItemPanelWrapper(content, this);
        return wrapper;
    }

    @Override
    public void setEnabled(final boolean b) {
    }

    @Override
    public boolean validate(final List<Role> value) {
        return !(isRequired && selectionModel.getSelectedSet().isEmpty());
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

    public String asString() {
        return "[" + UIHelper.csv(value) + "]";
    }

    @Override
    public void setErroneous(boolean b) {
        super.setErroneous(b);
        wrapper.setErroneous(b);
    }

    @Override
    public String getErrMessage() {
        return super.getErrMessage() + ": Select a role";
    }

    public void update(final Roles roles) {
        SortedSet<Role> all = new TreeSet<>();
//        all.addAll(roles.standardRoles);
//        all.addAll(roles.scopedRoles);
        dataProvider.setList(Lists.newArrayList(all));
        selectionModel.clear();
        for (Role role : value) {
            selectionModel.setSelected(role, true);
        }
    }
}
