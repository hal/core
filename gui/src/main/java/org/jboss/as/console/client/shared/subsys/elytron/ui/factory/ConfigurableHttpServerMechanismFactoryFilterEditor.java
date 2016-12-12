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
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
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
public class ConfigurableHttpServerMechanismFactoryFilterEditor implements IsWidget {


    private final ProvidesKey<ModelNode> nameProvider;

    private DefaultCellTable<ModelNode> table;
    private ListDataProvider<ModelNode> dataProvider;
    private final SingleSelectionModel<ModelNode> selectionModel;
    private Dispatcher circuit;
    private ResourceDescription resourceDescription;
    private SecurityContext securityContext;
    private String factoryName;

    ConfigurableHttpServerMechanismFactoryFilterEditor(final Dispatcher circuit, ResourceDescription resourceDescription,
            SecurityContext securityContext) {
        this.circuit = circuit;
        this.securityContext = securityContext;
        this.nameProvider = modelNode -> modelNode.get("pattern-filter");
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

        // table
        table = new DefaultCellTable<>(5, nameProvider);
        dataProvider = new ListDataProvider<>(nameProvider);
        dataProvider.addDataDisplay(table);
        table.setSelectionModel(selectionModel);

        // columns
        Column<ModelNode, String> patternColumn = new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode property) {
                return property.get("pattern-filter").asString();
            }
        };
        patternColumn.setSortable(true);
        Column<ModelNode, String> enablingColumn = new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode property) {
                return property.get("enabling").asString();
            }
        };
        enablingColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        table.addColumn(patternColumn, "Pattern Filter");
        table.addColumn(enablingColumn, "Enabling");
        table.setColumnWidth(patternColumn, 70, Style.Unit.PCT);
        table.setColumnWidth(enablingColumn, 30, Style.Unit.PCT);

        // tools
        ToolStrip tools = new ToolStrip();
        ToolButton addButton = new ToolButton(Console.CONSTANTS.common_label_add(), event -> {

            FormItem patternFilter = new TextBoxItem("pattern-filter", "Pattern Filter", true);
            ((TextBoxItem)patternFilter).setAllowWhiteSpace(true);

            FormItem enabling = new CheckBoxItem("enabling", "Enabling");
            enabling.setRequired(true);

            ModelNodeFormBuilder.FormAssets addFormAssets = new ModelNodeFormBuilder()
                    .setResourceDescription(resourceDescription)
                    .setCreateMode(true)
                    .setSecurityContext(securityContext)
                    .build();
            addFormAssets.getForm().setFields(patternFilter, enabling);
            addFormAssets.getForm().setEnabled(true);

            DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle("Pattern filter"));
            AddResourceDialog.Callback callback = new AddResourceDialog.Callback() {
                @Override
                public void onAdd(ModelNode payload) {
                    circuit.dispatch(new AddListAttribute(ElytronStore.CONFIGURABLE_HTTP_SERVER_MECHANISM_FACTORY_ADDRESS,
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
                Feedback.confirm("Filter", Console.MESSAGES.deleteConfirm("Filter "  + selection.get("pattern-filter").asString()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                circuit.dispatch(new RemoveListAttribute(
                                        ElytronStore.CONFIGURABLE_HTTP_SERVER_MECHANISM_FACTORY_ADDRESS,
                                        factoryName,
                                        "filters",
                                        selection));
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
