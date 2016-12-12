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
import org.jboss.ballroom.client.widgets.forms.FormItem;
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
public class ConfigurableSaslServerFactoryFilterEditor implements IsWidget {


    public static final String FILTERS = "filters";

    private DefaultCellTable<ModelNode> table;
    private ListDataProvider<ModelNode> dataProvider;
    private final SingleSelectionModel<ModelNode> selectionModel;
    private Dispatcher circuit;
    private ResourceDescription resourceDescription;
    private SecurityContext securityContext;
    private String factoryName;

    ConfigurableSaslServerFactoryFilterEditor(final Dispatcher circuit, ResourceDescription resourceDescription,
            SecurityContext securityContext) {
        this.circuit = circuit;
        this.securityContext = securityContext;
        selectionModel = new SingleSelectionModel<>();

        this.resourceDescription = new ResourceDescription(resourceDescription.clone());
        // rewrite the resource descriptor to allow the use of ModelNodeFormBuilder
        ModelNode reqPropsDescription = this.resourceDescription.get("operations").get("add").get("request-properties");
        ModelNode mechanismDescription = reqPropsDescription.get(FILTERS).get("value-type");
        reqPropsDescription.set(mechanismDescription);
    }

    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        VerticalPanel panel = new VerticalPanel();
        panel.addStyleName("fill-layout-width");

        // table
        table = new DefaultCellTable<>(5);
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(table);
        table.setSelectionModel(selectionModel);

        // columns
        Column<ModelNode, String> predefinedFilter = new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode node) {
                return node.hasDefined("predefined-filter") ? node.get("predefined-filter").asString() : "";
            }
        };
        Column<ModelNode, String> patternFilter = new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode node) {
                return node.hasDefined("pattern-filter") ? node.get("pattern-filter").asString() : "";
            }
        };
        Column<ModelNode, String> enablingColumn = new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode property) {
                return property.hasDefined("enabling") ? property.get("enabling").asString() : "";
            }
        };
        predefinedFilter.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        patternFilter.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        enablingColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        table.addColumn(predefinedFilter, "Pre-defined Filter");
        table.addColumn(patternFilter, "Pattern Filter");
        table.addColumn(enablingColumn, "Enabling");

        // tools
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
            addFormAssets.getForm().addFormValidator((list, formValidation) -> {
                // pattern-filter and predefined-filter are mutually exclusive
                FormItem<String> patternFilterFormItem = formItem(list, "pattern-filter");
                FormItem<String> predefinedFilterFormItem = formItem(list, "predefined-filter");
                if ((patternFilterFormItem.isUndefined() && predefinedFilterFormItem.isUndefined())
                        || (!patternFilterFormItem.isUndefined() && !predefinedFilterFormItem.isUndefined())) {
                    predefinedFilterFormItem.setErrMessage("Please set either predefined filter or pattern filter.");
                    predefinedFilterFormItem.setErroneous(true);
                    formValidation.addError("predefined-filter");
                }
            });


            DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle("Filter"));
            AddResourceDialog.Callback callback = new AddResourceDialog.Callback() {
                @Override
                public void onAdd(ModelNode payload) {
                    // pattern-filter and predefined-filter are mutually exclusive attributes
                    if (payload.hasDefined("predefined-filter"))
                        payload.remove("pattern-filter");
                    else if (payload.hasDefined("pattern-filter"))
                        payload.remove("predefined-filter");

                    circuit.dispatch(new AddListAttribute(ElytronStore.CONFIGURABLE_SASL_SERVER_FACTORY_ADDRESS,
                            FILTERS,
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
                Feedback.confirm("Filter", Console.MESSAGES.deleteConfirm("filter "
                        + (selection.hasDefined("predefined-filter") ? selection.get("predefined-filter").asString() : selection.get("pattern-filter").asString())),
                        isConfirmed -> {
                            if (isConfirmed) {
                                circuit.dispatch(new RemoveListAttribute(
                                        ElytronStore.CONFIGURABLE_SASL_SERVER_FACTORY_ADDRESS,
                                        factoryName,
                                        FILTERS,
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

    @SuppressWarnings("unchecked")
    private <T> FormItem<T> formItem(List<FormItem> formItems, String name) {
        for (FormItem formItem : formItems) {
            if (name.equals(formItem.getName())) {
                return formItem;
            }
        }
        return null;
    }


    public void update(Property prop) {
        factoryName = prop.getName();
        if (prop.getValue().hasDefined(FILTERS)) {
            List<ModelNode> models = prop.getValue().get(FILTERS).asList();
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
