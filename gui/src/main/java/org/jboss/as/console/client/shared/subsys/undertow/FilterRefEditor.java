/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.shared.subsys.undertow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.Property;

/**
 * 
 * FilterRefEditor to maintain the list of Filter References for each Host in undertow subsystem.
 * 
 * @author Claudio Miranda <claudio@redhat.com>
 * @since 04/06/2016
 */
public class FilterRefEditor implements IsWidget {


    private final ProvidesKey<Property> nameProvider;
    private final AddressTemplate operationAddress;
    private AddressTemplate resolvedOperationAddress;
    private final HttpPresenter presenter;

    private DefaultCellTable<Property> table;
    private ListDataProvider<Property> dataProvider;
    private final SingleSelectionModel<Property> selectionModel;
    private String hostname;

    FilterRefEditor(HttpPresenter presenter, AddressTemplate operationAddress) {
        this.presenter = presenter;
        this.nameProvider = Property::getName;
        this.operationAddress = operationAddress;
        selectionModel = new SingleSelectionModel<>(nameProvider);
    }

    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        VerticalPanel panel = new VerticalPanel();
        panel.addStyleName("fill-layout-width");

        // table
        table = new DefaultCellTable<>(5, nameProvider);
        dataProvider = new ListDataProvider<>(nameProvider);
        dataProvider.addDataDisplay(table);
        table.setSelectionModel(selectionModel);

        // columns
        Column<Property, String> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property property) {
                return property.getName();
            }
        };
        nameColumn.setSortable(true);
        Column<Property, String> predicateColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property property) {
                return getClassValue(property, "predicate");
            }
        };
        Column<Property, String> priorityColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property property) {
                return getClassValue(property, "priority");
            }
        };
        priorityColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        predicateColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        ColumnSortEvent.ListHandler<Property> sortHandler = new ColumnSortEvent.ListHandler<>(dataProvider.getList());
        sortHandler.setComparator(nameColumn, (o1, o2) -> o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase()));
        table.addColumn(nameColumn, Console.CONSTANTS.common_label_name());
        table.addColumn(predicateColumn, "Predicate");
        table.addColumn(priorityColumn, "Priority");
        table.setColumnWidth(nameColumn, 30, Style.Unit.PCT);
        table.setColumnWidth(predicateColumn, 40, Style.Unit.PCT);
        table.setColumnWidth(priorityColumn, 20, Style.Unit.PCT);
        table.addColumnSortHandler(sortHandler);
        table.getColumnSortList().push(nameColumn);

        // tools
        ToolStrip tools = new ToolStrip();
        ToolButton addButton = new ToolButton(Console.CONSTANTS.common_label_add(), event -> {
            presenter.setSelectedHost(hostname);
            presenter.onLaunchAddFilterReferenceDialog(operationAddress, hostname);
        });
        ToolButton removeButton = new ToolButton(Console.CONSTANTS.common_label_delete(), event -> {
            final Property selection = selectionModel.getSelectedObject();
            if (selection != null) {
                Feedback.confirm("Filter reference", Console.MESSAGES.deleteConfirm("Filter reference "  + selection.getName()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                presenter.setSelectedHost(hostname);
                                presenter.onRemoveResource(resolvedOperationAddress, selection.getName());
                            }
                        });
            }
        });
        tools.addToolButtonRight(addButton);
        tools.addToolButtonRight(removeButton);
        panel.add(tools);

        panel.add(table);
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        panel.add(pager);
        return panel;
    }

    public void update(List<Property> handlerClasses) {
        table.setRowCount(handlerClasses.size(), true);

        Collections.sort(handlerClasses, (o1, o2) -> o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase()));

        List<Property> dataList = dataProvider.getList();
        dataList.clear(); // cannot call setList() as that breaks the sort handler
        dataList.addAll(handlerClasses);

        // Make sure the new values are properly sorted
        ColumnSortEvent.fire(table, table.getColumnSortList());
    }

    void updateOperationAddressNames(String hostname) {
        this.hostname = hostname;
        resolvedOperationAddress = operationAddress.replaceWildcards(hostname);
    }

    public void clearValues() {
        dataProvider.setList(new ArrayList<>());
    }

    private String getClassValue(Property property, String attrName) {
        String value = "";
        if (property.getValue().hasDefined(attrName)) {
            value = property.getValue().get(attrName).asString();
        }
        return value;
    }
}