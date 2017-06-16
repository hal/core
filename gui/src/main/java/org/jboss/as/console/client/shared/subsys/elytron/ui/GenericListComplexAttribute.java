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
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.subsys.elytron.store.AddListAttribute;
import org.jboss.as.console.client.shared.subsys.elytron.store.RemoveListAttribute;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
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

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class GenericListComplexAttribute implements IsWidget {


    private DefaultCellTable<ModelNode> table;
    private ListDataProvider<ModelNode> dataProvider;
    private final SingleSelectionModel<ModelNode> selectionModel;
    private Dispatcher circuit;
    private ResourceDescription resourceDescription;
    private SecurityContext securityContext;
    private AddressTemplate template;
    private String complexAttributeName;
    private String parentResourceName;
    private ToolButton removeButton;
    private ToolButton addButton;

    GenericListComplexAttribute(final Dispatcher circuit, ResourceDescription resourceDescription,
            SecurityContext securityContext, AddressTemplate template, String complexAttributeName) {
        this.circuit = circuit;
        this.securityContext = securityContext;
        this.template = template;
        this.complexAttributeName = complexAttributeName;
        selectionModel = new SingleSelectionModel<>();

        this.resourceDescription = new ResourceDescription(resourceDescription.clone());
        ModelNode reqPropsDescription = this.resourceDescription.get(OPERATIONS).get(ADD).get(REQUEST_PROPERTIES);
        ModelNode nestedAttributes = reqPropsDescription.get(complexAttributeName).get(VALUE_TYPE);
        reqPropsDescription.set(nestedAttributes);
        this.resourceDescription.get(ATTRIBUTES).set(resourceDescription.get(ATTRIBUTES).get(complexAttributeName).get(VALUE_TYPE));
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
        table = new DefaultCellTable<>(10);
        table.setSelectionModel(selectionModel);

        // columns
        for (Property propDescription: resourceDescription.get(ATTRIBUTES).asPropertyList()) {
            Column<ModelNode, String> column = createColumn(propDescription.getName());
            column.setSortable(true);
            column.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
            table.addColumn(column, label(propDescription.getName()));
        };
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
        String label = label(complexAttributeName);

        addButton = new ToolButton(Console.CONSTANTS.common_label_add(), event -> {

            ModelNodeFormBuilder.FormAssets addFormAssets = new ModelNodeFormBuilder()
                    .setResourceDescription(resourceDescription)
                    .setCreateMode(true)
                    .unsorted()
                    .setSecurityContext(securityContext)
                    .build();
            addFormAssets.getForm().setEnabled(true);

            DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle(label));
            AddResourceDialog.Callback callback = new AddResourceDialog.Callback() {
                @Override
                public void onAdd(ModelNode payload) {
                    circuit.dispatch(new AddListAttribute(template,
                            complexAttributeName,
                            parentResourceName,
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

        removeButton = new ToolButton(Console.CONSTANTS.common_label_delete(), event -> {
            final ModelNode selection = selectionModel.getSelectedObject();
            if (selection != null) {
                String _name  = selection.hasDefined(NAME) ? selection.get(NAME).asString() : selection.asString();
                Feedback.confirm(label, Console.MESSAGES.deleteConfirm(label + ": " + _name),
                        isConfirmed -> {
                            if (isConfirmed) {
                                circuit.dispatch(new RemoveListAttribute(
                                        template,
                                        parentResourceName,
                                        complexAttributeName,
                                        selection));
                            }
                        });
            }
        });
        removeButton.setEnabled(false);
        addButton.setEnabled(false);
        tools.addToolButtonRight(addButton);
        tools.addToolButtonRight(removeButton);
        return tools;
    }

    public void update(Property prop) {
        parentResourceName = prop.getName();
        if (prop.getValue().hasDefined(complexAttributeName)) {
            List<ModelNode> models = prop.getValue().get(complexAttributeName).asList();
            table.setRowCount(models.size(), true);

            List<ModelNode> dataList = dataProvider.getList();
            dataList.clear();
            dataList.addAll(models);
        } else {
            clearValues();
        }
        selectionModel.clear();
        removeButton.setEnabled(true);
        addButton.setEnabled(true);
    }

    public void clearValues() {
        dataProvider.setList(new ArrayList<>());
        removeButton.setEnabled(false);
        addButton.setEnabled(false);
    }

    private String label(String attr) {
        char[] attrName = attr.toCharArray();
        attrName[0] = Character.toUpperCase(attrName[0]);
        return new String(attrName).replace("-", " ");
    }

}