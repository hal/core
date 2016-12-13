/*
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.console.client.shared.subsys.elytron.ui;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Style;
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
import org.jboss.dmr.client.Property;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class ProviderLoaderEditor implements IsWidget {

    private DefaultCellTable<ModelNode> table;
    private ListDataProvider<ModelNode> dataProvider;
    private final SingleSelectionModel<ModelNode> selectionModel;

    ProviderLoaderEditor() {
        selectionModel = new SingleSelectionModel<>();
    }

    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        VerticalPanel panel = new VerticalPanel();
        panel.addStyleName("fill-layout-width");

        setupTable();
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(table);

        panel.add(table);
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        panel.add(pager);
        return panel;
    }

    private void setupTable() {
        table = new DefaultCellTable<>(5);
        table.setSelectionModel(selectionModel);

        // columns
        Column<ModelNode, String> indexCol = createColumn("index");
        Column<ModelNode, String> moduleCol = createColumn("module");
        Column<ModelNode, String> loadServicesCol = createColumn("load-services");
        Column<ModelNode, String> classnamesCol = createColumn("class-names");
        Column<ModelNode, String> pathCol= createColumn("path");
        Column<ModelNode, String> relativeToCol = createColumn("relative-to");
        indexCol.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        moduleCol.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        loadServicesCol.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        classnamesCol.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        pathCol.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        relativeToCol.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        table.addColumn(indexCol, "Index");
        table.addColumn(moduleCol, "Module");
        table.addColumn(loadServicesCol, "Load Services");
        table.addColumn(classnamesCol, "Class names");
        table.addColumn(pathCol, "Path");
        table.addColumn(relativeToCol, "Relative To");
        table.setColumnWidth(indexCol, 7, Style.Unit.PCT);
        table.setColumnWidth(moduleCol, 20, Style.Unit.PCT);
        table.setColumnWidth(loadServicesCol, 13, Style.Unit.PCT);
        table.setColumnWidth(classnamesCol, 30, Style.Unit.PCT);
        table.setColumnWidth(pathCol, 20, Style.Unit.PCT);
        table.setColumnWidth(relativeToCol, 10, Style.Unit.PCT);
    }

    private Column<ModelNode, String> createColumn(String attributeName) {
        return new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode node) {
                return node.hasDefined(attributeName) ? node.get(attributeName).asString() : "";
            }
        };
    }

    public void update(Property prop) {
        if (prop.getValue().hasDefined("providers")) {
            List<ModelNode> models = prop.getValue().get("providers").asList();
            table.setRowCount(models.size(), true);

            List<ModelNode> dataList = dataProvider.getList();
            dataList.clear();
            dataList.addAll(models);
        } else {
            clearValues();
        }
        selectionModel.clear();
    }

    public void clearValues() {
        dataProvider.setList(new ArrayList<>());
    }

}