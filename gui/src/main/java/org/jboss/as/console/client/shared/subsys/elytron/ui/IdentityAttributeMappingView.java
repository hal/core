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
package org.jboss.as.console.client.shared.subsys.elytron.ui;

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
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class IdentityAttributeMappingView implements IsWidget {


    private final ProvidesKey<ModelNode> nameProvider;

    private DefaultCellTable<ModelNode> table;
    private ListDataProvider<ModelNode> dataProvider;
    private final SingleSelectionModel<ModelNode> selectionModel;

    IdentityAttributeMappingView() {
        this.nameProvider = modelNode -> modelNode.get("from");
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
        Column<ModelNode, String> fromColumn = new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode node) {
                return node.get("from").asString();
            }
        };
        fromColumn.setSortable(true);
        Column<ModelNode, String> toColumn = new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode node) {
                return node.get("to").asString();
            }
        };
        Column<ModelNode, String> filterColumn = new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode node) {
                return node.get("filter").asString();
            }
        };
        filterColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        toColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        ColumnSortEvent.ListHandler<ModelNode> sortHandler = new ColumnSortEvent.ListHandler<>(dataProvider.getList());
        sortHandler.setComparator(fromColumn,
                (o1, o2) -> o1.get("from").asString().toLowerCase().compareTo(o2.get("from").asString().toLowerCase()));
        table.addColumn(fromColumn, "From");
        table.addColumn(toColumn, "To");
        table.addColumn(filterColumn, "Filter");
        table.setColumnWidth(fromColumn, 30, Style.Unit.PCT);
        table.setColumnWidth(toColumn, 40, Style.Unit.PCT);
        table.setColumnWidth(filterColumn, 20, Style.Unit.PCT);
        table.addColumnSortHandler(sortHandler);
        table.getColumnSortList().push(fromColumn);

        panel.add(table);
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        panel.add(pager);
        return panel;
    }

    public void update(List<ModelNode> models) {
        table.setRowCount(models.size(), true);

        Collections.sort(models,
                (o1, o2) -> o1.get("from").asString().toLowerCase().compareTo(o2.get("from").asString().toLowerCase()));

        List<ModelNode> dataList = dataProvider.getList();
        dataList.clear(); // cannot call setList() as that breaks the sort handler
        dataList.addAll(models);

        // Make sure the new values are properly sorted
        ColumnSortEvent.fire(table, table.getColumnSortList());
    }

    public void clearValues() {
        dataProvider.setList(new ArrayList<>());
    }
}