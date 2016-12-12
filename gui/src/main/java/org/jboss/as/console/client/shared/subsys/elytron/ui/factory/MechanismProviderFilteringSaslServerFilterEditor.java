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
package org.jboss.as.console.client.shared.subsys.elytron.ui.factory;

import java.util.ArrayList;
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
import org.jboss.as.console.client.shared.subsys.elytron.store.AddListAttribute;
import org.jboss.as.console.client.shared.subsys.elytron.store.ElytronStore;
import org.jboss.as.console.client.shared.subsys.elytron.store.RemoveListAttribute;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class MechanismProviderFilteringSaslServerFilterEditor implements IsWidget {


    private final ProvidesKey<ModelNode> nameProvider;

    private DefaultCellTable<ModelNode> table;
    private ListDataProvider<ModelNode> dataProvider;
    private final SingleSelectionModel<ModelNode> selectionModel;
    private Dispatcher circuit;
    private ResourceDescription resourceDescription;
    private SecurityContext securityContext;
    private String factoryName;

    MechanismProviderFilteringSaslServerFilterEditor(final Dispatcher circuit, ResourceDescription resourceDescription,
            SecurityContext securityContext) {
        this.circuit = circuit;
        this.securityContext = securityContext;
        this.nameProvider = modelNode -> modelNode.get("provider-name");
        selectionModel = new SingleSelectionModel<>(nameProvider);

        this.resourceDescription = new ResourceDescription(resourceDescription.clone());
        ModelNode reqPropsDescription = this.resourceDescription.get("operations").get("add").get("request-properties");
        ModelNode filtersDescription = reqPropsDescription.get("filters").get("value-type");
        reqPropsDescription.set(filtersDescription);
    }

    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        VerticalPanel panel = new VerticalPanel();
        panel.addStyleName("fill-layout-width");

        setupTable();
        dataProvider = new ListDataProvider<>(nameProvider);
        dataProvider.addDataDisplay(table);

        panel.add(setupTableButtons());

        panel.add(table);
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        panel.add(pager);
        return panel;
    }

    private void setupTable() {
        table = new DefaultCellTable<>(5, nameProvider);
        table.setSelectionModel(selectionModel);

        // columns
        Column<ModelNode, String> providerNameColumn = createColumn("provider-name");
        Column<ModelNode, String> mechanismNameColumn = createColumn("mechanism-name");
        Column<ModelNode, String> providerVersionColumn = createColumn("provider-version");
        Column<ModelNode, String> versionComparisonColumn = createColumn("version-comparison");
        providerNameColumn.setSortable(true);
        providerNameColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        mechanismNameColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        providerVersionColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        versionComparisonColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        table.addColumn(providerNameColumn, "Provider Name");
        table.addColumn(mechanismNameColumn, "Mechanism Name");
        table.addColumn(providerVersionColumn, "Provider Version");
        table.addColumn(versionComparisonColumn, "Version Comparison");
        table.setColumnWidth(providerNameColumn, 30, Style.Unit.PCT);
        table.setColumnWidth(mechanismNameColumn, 30, Style.Unit.PCT);
        table.setColumnWidth(providerVersionColumn, 20, Style.Unit.PCT);
        table.setColumnWidth(versionComparisonColumn, 20, Style.Unit.PCT);
    }

    private Column<ModelNode, String> createColumn(String attributeName) {
        return new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode node) {
                return node.hasDefined(attributeName) ? node.get(attributeName).asString() : "";
            }
        };
    }

    private ToolStrip setupTableButtons() {
        ToolStrip tools = new ToolStrip();
        ToolButton addButton = new ToolButton(Console.CONSTANTS.common_label_add(), event -> {

            ModelNodeFormBuilder.FormAssets addFormAssets = new ModelNodeFormBuilder()
                    .setResourceDescription(resourceDescription)
                    .setCreateMode(true)
                    .unsorted()
                    .setCreateNameAttribute(false)
                    .setSecurityContext(securityContext)
                    .build();
            addFormAssets.getForm().setEnabled(true);

            DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle("Filter"));
            AddResourceDialog.Callback callback = new AddResourceDialog.Callback() {
                @Override
                public void onAdd(ModelNode payload) {
                    circuit.dispatch(new AddListAttribute(ElytronStore.MECHANISM_PROVIDER_FILTERING_SASL_SERVER_FACTORY_ADDRESS,
                            "filters",
                            factoryName,
                            payload));
                    dialog.hide();
                }

                @Override
                public void onCancel() {
                    dialog.hide();
                }
            };
            AddResourceDialog addDialog = new AddResourceDialog(addFormAssets, resourceDescription, callback);
            dialog.setWidth(480);
            dialog.setHeight(360);
            dialog.setWidget(addDialog);
            dialog.setGlassEnabled(true);
            dialog.center();
        });
        ToolButton removeButton = new ToolButton(Console.CONSTANTS.common_label_delete(), event -> {
            final ModelNode selection = selectionModel.getSelectedObject();
            if (selection != null) {
                Feedback.confirm("Filter", Console.MESSAGES.deleteConfirm("Filter "  + selection.get("provider-name").asString()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                circuit.dispatch(new RemoveListAttribute(
                                        ElytronStore.MECHANISM_PROVIDER_FILTERING_SASL_SERVER_FACTORY_ADDRESS,
                                        factoryName,
                                        "filters",
                                        selection));
                            }
                        });
            }
        });
        tools.addToolButtonRight(addButton);
        tools.addToolButtonRight(removeButton);
        return tools;
    }

    public void update(Property prop) {
        factoryName = prop.getName();
        if (prop.getValue().hasDefined("filters")) {
            List<ModelNode> models = prop.getValue().get("filters").asList();
            table.setRowCount(models.size(), true);

            List<ModelNode> dataList = dataProvider.getList();
            dataList.clear(); // cannot call setList() as that breaks the sort handler
            dataList.addAll(models);
        } else {
            clearValues();
        }

        // Make sure the new values are properly sorted
        ColumnSortEvent.fire(table, table.getColumnSortList());
        selectionModel.clear();
    }

    public void clearValues() {
        dataProvider.setList(new ArrayList<>());
    }

}
