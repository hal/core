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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.subsys.elytron.store.AddListAttribute;
import org.jboss.as.console.client.shared.subsys.elytron.store.ElytronStore;
import org.jboss.as.console.client.shared.subsys.elytron.store.RemoveListAttribute;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.client.widgets.tables.ViewLinkCell;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class SimplePermissionMappingEditor implements IsWidget {

    private DefaultCellTable<ModelNode> table;
    private ListDataProvider<ModelNode> dataProvider;
    private final SingleSelectionModel<ModelNode> selectionModel;
    private Dispatcher circuit;
    private ResourceDescription resourceDescription;
    private SecurityContext securityContext;
    private String permissionMapping;

    private DefaultCellTable<ModelNode> tablePermissions;
    private ListDataProvider<ModelNode> dataProviderPermissions;
    private VerticalPanel permissionPopupLayout = new VerticalPanel();
    private DefaultWindow permissionsWindow;

    // button to hide the match-rules detail window
    // the cancel button is not displayed
    private DialogueOptions popupDialogOptions = new DialogueOptions(Console.CONSTANTS.common_label_done(),

            // done
            new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    permissionsWindow.hide();
                }
            },

            Console.CONSTANTS.common_label_cancel(),
            // cancel
            new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    permissionsWindow.hide();
                }
            }
    );

    SimplePermissionMappingEditor(final Dispatcher circuit, ResourceDescription resourceDescription,
            SecurityContext securityContext) {
        this.circuit = circuit;
        this.securityContext = securityContext;
        selectionModel = new SingleSelectionModel<>();

        this.resourceDescription = new ResourceDescription(resourceDescription.clone());
        ModelNode reqPropsDescription = this.resourceDescription.get(OPERATIONS).get(ADD).get(REQUEST_PROPERTIES);
        ModelNode filtersDescription = reqPropsDescription.get("permission-mappings").get(VALUE_TYPE);
        reqPropsDescription.set(filtersDescription);
    }

    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        VerticalPanel panel = new VerticalPanel();
        panel.addStyleName("fill-layout-width");

        setupTable();
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(table);

        setupPermissionsTable();
        panel.add(setupTableButtons());


        panel.add(table);
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        panel.add(pager);
        return panel;
    }

    private void setupPermissionsTable() {
        tablePermissions = new DefaultCellTable<>(5);

        Column<ModelNode, String> className = createColumn ("class-name");
        Column<ModelNode, String> module = createColumn ("module");
        Column<ModelNode, String> targetName = createColumn ("target-name");
        Column<ModelNode, String> action = createColumn ("action");
        tablePermissions.addColumn(className, "Class name");
        tablePermissions.addColumn(module, "Module");
        tablePermissions.addColumn(targetName, "Target name");
        tablePermissions.addColumn(action, "Action");
        dataProviderPermissions = new ListDataProvider<>();
        dataProviderPermissions.addDataDisplay(tablePermissions);

        popupDialogOptions.showCancel(false);
        permissionPopupLayout.setStyleName("window-content");
        permissionPopupLayout.add(tablePermissions);
    }

    private void setupTable() {
        table = new DefaultCellTable<>(5);
        table.setSelectionModel(selectionModel);

        // columns
        Column<ModelNode, String> matchAll = createColumn("match-all");
        Column<ModelNode, String> principals = createColumn("principals");
        Column<ModelNode, String> roles = createColumn(ROLES);
        Column<ModelNode, ModelNode> linkOpenDetailsColumn = new Column<ModelNode, ModelNode>(
                new ViewLinkCell<>(Console.CONSTANTS.common_label_view(), this::showDetailModal)) {
            @Override
            public ModelNode getValue(ModelNode node) {
                return node;
            }
        };

        matchAll.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        principals.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        roles.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        linkOpenDetailsColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        table.addColumn(matchAll, "Match All");
        table.addColumn(principals, "Principals");
        table.addColumn(roles, "Roles");
        table.addColumn(linkOpenDetailsColumn, "Permissions");
        table.setColumnWidth(matchAll, 20, Style.Unit.PCT);
        table.setColumnWidth(principals, 30, Style.Unit.PCT);
        table.setColumnWidth(roles, 30, Style.Unit.PCT);
        table.setColumnWidth(linkOpenDetailsColumn, 20, Style.Unit.PCT);
    }

    private Column<ModelNode, String> createColumn(String attribute) {
        return new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode node) {
                return node.hasDefined(attribute) ? node.get(attribute).asString().replaceAll("\\[|\"|\\]", ""): "";
            }
        };
    }

    private void showDetailModal(final ModelNode selection) {

        if (selection.hasDefined("permissions")) {
            List<ModelNode> models = selection.get("permissions").asList();
            tablePermissions.setRowCount(models.size(), true);

            List<ModelNode> dataList = dataProviderPermissions.getList();
            dataList.clear();
            dataList.addAll(models);
        } else {
            dataProviderPermissions.setList(new ArrayList<>());
        }
        Widget windowContent = new WindowContentBuilder(permissionPopupLayout, popupDialogOptions).build();

        permissionsWindow = new DefaultWindow("Permissions");
        permissionsWindow.setWidth(800);
        permissionsWindow.setHeight(430);
        permissionsWindow.trapWidget(windowContent);
        permissionsWindow.setGlassEnabled(true);
        permissionsWindow.center();
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
                    .requiresAtLeastOne("principals", "roles", "match-all")
                    .build();
            addFormAssets.getForm().setEnabled(true);

            DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle("Permission Mapping"));
            AddResourceDialog.Callback callback = new AddResourceDialog.Callback() {
                @Override
                public void onAdd(ModelNode payload) {
                    circuit.dispatch(new AddListAttribute(ElytronStore.SIMPLE_PERMISSION_MAPPER_ADDRESS,
                            "permission-mappings",
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
                Feedback.confirm("Permission Mapping", Console.MESSAGES.deleteConfirm("Permission Mapping "  + selection.asString()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                circuit.dispatch(new RemoveListAttribute(
                                        ElytronStore.SIMPLE_PERMISSION_MAPPER_ADDRESS,
                                        permissionMapping,
                                        "permission-mappings",
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
        if (prop.getValue().hasDefined("permission-mappings")) {
            List<ModelNode> models = prop.getValue().get("permission-mappings").asList();
            table.setRowCount(models.size(), true);

            List<ModelNode> dataList = dataProvider.getList();
            dataList.clear(); // cannot call setList() as that breaks the sort handler
            dataList.addAll(models);
        } else {
            clearValues();
        }
        selectionModel.clear();
        SelectionChangeEvent.fire(selectionModel);
    }

    public void clearValues() {
        dataProvider.setList(new ArrayList<>());
    }

}