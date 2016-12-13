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
package org.jboss.as.console.client.shared.subsys.elytron.ui.mapper;

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
public class ConstantPermissionMappingEditor implements IsWidget {

    private DefaultCellTable<ModelNode> table;
    private ListDataProvider<ModelNode> dataProvider;
    private final SingleSelectionModel<ModelNode> selectionModel;
    private Dispatcher circuit;
    private ResourceDescription resourceDescription;
    private SecurityContext securityContext;
    private String permissionMapping;

    ConstantPermissionMappingEditor(final Dispatcher circuit, ResourceDescription resourceDescription,
            SecurityContext securityContext) {
        this.circuit = circuit;
        this.securityContext = securityContext;
        selectionModel = new SingleSelectionModel<>();
        
        this.resourceDescription = new ResourceDescription(resourceDescription.clone());
        ModelNode reqPropsDescription = this.resourceDescription.get("operations").get("add").get("request-properties");
        ModelNode filtersDescription = reqPropsDescription.get("permissions").get("value-type");
        reqPropsDescription.set(filtersDescription);
    }

    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        VerticalPanel panel = new VerticalPanel();
        panel.addStyleName("fill-layout-width");

        setupTable();
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(table);

        panel.add(setupTableButtons());

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
        Column<ModelNode, String> classNameColumn = createColumn("class-name");
        Column<ModelNode, String> moduleColumn = createColumn("module");
        Column<ModelNode, String> targetNameColumn = createColumn("target-name");
        Column<ModelNode, String> actionColumn = createColumn("action");
        classNameColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        moduleColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        targetNameColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        actionColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        table.addColumn(classNameColumn, "Class name");
        table.addColumn(moduleColumn, "Module");
        table.addColumn(targetNameColumn, "Target name");
        table.addColumn(actionColumn, "Action");
        table.setColumnWidth(classNameColumn, 40, Style.Unit.PCT);
        table.setColumnWidth(moduleColumn, 20, Style.Unit.PCT);
        table.setColumnWidth(targetNameColumn, 25, Style.Unit.PCT);
        table.setColumnWidth(actionColumn, 15, Style.Unit.PCT);
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
                    .exclude("permissions")
                    .setCreateNameAttribute(false)
                    .setSecurityContext(securityContext)
                    .build();
            addFormAssets.getForm().setEnabled(true);

            DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle("Permission"));
            AddResourceDialog.Callback callback = new AddResourceDialog.Callback() {
                @Override
                public void onAdd(ModelNode payload) {
                    circuit.dispatch(new AddListAttribute(ElytronStore.CONSTANT_PERMISSION_MAPPER_ADDRESS,
                            "permissions",
                            permissionMapping,
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
                Feedback.confirm("Filter", Console.MESSAGES.deleteConfirm("Permission "  + selection.get("class-name").asString()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                circuit.dispatch(new RemoveListAttribute(
                                        ElytronStore.CONSTANT_PERMISSION_MAPPER_ADDRESS,
                                        permissionMapping,
                                        "permissions",
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
        permissionMapping = prop.getName();
        if (prop.getValue().hasDefined("permissions")) {
            List<ModelNode> models = prop.getValue().get("permissions").asList();
            table.setRowCount(models.size(), true);
    
            List<ModelNode> dataList = dataProvider.getList();
            dataList.clear(); // cannot call setList() as that breaks the sort handler
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