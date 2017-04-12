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
import java.util.List;

import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class IdentityAttributeMappingView implements IsWidget {

    private DefaultCellTable<ModelNode> table;
    private ListDataProvider<ModelNode> dataProvider;
    private final SingleSelectionModel<ModelNode> selectionModel;

    IdentityAttributeMappingView() {
        selectionModel = new SingleSelectionModel<>();
    }

    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        VerticalPanel panel = new VerticalPanel();
        panel.addStyleName("fill-layout-width");

        // table
        table = new DefaultCellTable<>(20);
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(table);
        table.setSelectionModel(selectionModel);

        // columns
        Column<ModelNode, String> fromColumn = createColumn("from");
        Column<ModelNode, String> toColumn = createColumn("to");
        Column<ModelNode, String> referenceColumn = createColumn("reference");
        Column<ModelNode, String> filterColumn = createColumn("filter");
        Column<ModelNode, String> filterBaseColumn = createColumn("filter-base-dn");
        Column<ModelNode, String> searchRecursiveColumn = createColumn("search-recursive");
        Column<ModelNode, String> roleRecursionColumn = createColumn("role-recursion");
        Column<ModelNode, String> roleRecursioNameColumn = createColumn("role-recursion-name");
        Column<ModelNode, String> extractDnColumn = createColumn("extract-rdn");
        table.addColumn(fromColumn, "From");
        table.addColumn(toColumn, "To");
        table.addColumn(referenceColumn, "Reference");
        table.addColumn(filterColumn, "Filter");
        table.addColumn(filterBaseColumn, "Filter Base DN");
        table.addColumn(searchRecursiveColumn, "Search Recursive");
        table.addColumn(roleRecursionColumn, "Role Recursion");
        table.addColumn(roleRecursioNameColumn, "Role Recursion Name");
        table.addColumn(extractDnColumn, "Extract RDN");

        panel.add(table);
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        panel.add(pager);
        return panel;
    }

    private Column<ModelNode, String> createColumn(String attributeName) {
        Column<ModelNode, String> column = new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode node) {
            return node.hasDefined(attributeName) ? node.get(attributeName).asString() : "";
            }
        };
        column.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        return column;
    }

    public void update(List<ModelNode> models) {
        table.setRowCount(models.size(), true);
        List<ModelNode> dataList = dataProvider.getList();
        dataList.clear();
        dataList.addAll(models);
    }

    public void clearValues() {
        dataProvider.setList(new ArrayList<>());
    }
}