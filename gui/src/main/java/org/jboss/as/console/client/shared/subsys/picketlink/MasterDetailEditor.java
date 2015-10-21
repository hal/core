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
package org.jboss.as.console.client.shared.subsys.picketlink;

import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.Property;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.List;
import java.util.Map;

/**
 * @author Harald Pehl
 */
abstract class MasterDetailEditor implements IsWidget {

    final SecurityContext securityContext;
    final StatementContext statementContext;
    final ResourceDescription resourceDescription;
    final String resourceName;

    private ListDataProvider<Property> dataProvider;
    private SingleSelectionModel<Property> selectionModel;
    private DefaultCellTable<Property> table;
    private ModelNodeFormBuilder.FormAssets formAssets;

    MasterDetailEditor(final SecurityContext securityContext,
            final StatementContext statementContext,
            final ResourceDescription resourceDescription,
            final String resourceName) {
        this.securityContext = securityContext;
        this.statementContext = statementContext;
        this.resourceDescription = resourceDescription;
        this.resourceName = resourceName;

        //noinspection Convert2MethodRef
        ProvidesKey<Property> providesKey = item -> item.getName();
        table = new DefaultCellTable<>(5, providesKey);
        dataProvider = new ListDataProvider<>(providesKey);
        selectionModel = new SingleSelectionModel<>(providesKey);
        //noinspection unchecked
        dataProvider.addDataDisplay(table);
        table.setSelectionModel(selectionModel);
    }

    ToolStrip tools() {
        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), event -> onAdd()));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(), event -> {
            Property selection = selectionModel.getSelectedObject();
            if (selection != null) {
                Feedback.confirm(Console.MESSAGES.deleteTitle(resourceName),
                        Console.MESSAGES.deleteConfirm(selection.getName()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                onRemove(selection);
                            }
                        });
            }
        }));
        return tools;
    }

    @SuppressWarnings("unchecked")
    DefaultCellTable<Property> table() {
        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };
        table.addColumn(nameColumn, "Name");
        return table;
    }

    Widget formPanel() {
        formAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext).build();
        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeSet) {
                onModify(selectionModel.getSelectedObject().getName(), formAssets.getForm().getChangedValues());
            }

            @Override
            public void onCancel(Object entity) {
                formAssets.getForm().cancel();
            }
        });

        selectionModel.addSelectionChangeHandler(event -> {
            Property property = selectionModel.getSelectedObject();
            if (property != null) {
                updateDetail(property);
            } else {
                clearDetail();
            }
        });

        VerticalPanel formPanel = new VerticalPanel();
        formPanel.setStyleName("fill-layout-width");
        formPanel.add(formAssets.getHelp().asWidget());
        formPanel.add(formAssets.getForm().asWidget());
        return formPanel;
    }


    // ------------------------------------------------------ abstract methods

    abstract void onAdd();

    abstract void onModify(final String name, final Map<String, Object> changedValues);

    abstract void onRemove(final Property item);


    // ------------------------------------------------------ update

    void updateMaster(final List<Property> models) {
        dataProvider.setList(models);
        if (models.isEmpty()) {
            selectionModel.clear();
        } else {
            table.selectDefaultEntity();
            updateDetail(selectionModel.getSelectedObject());
        }
    }

    void updateDetail(Property property) {
        if (formAssets != null) {
            formAssets.getForm().edit(property.getValue());
        }
    }

    void clearDetail() {
        formAssets.getForm().clearValues();
    }


    // ------------------------------------------------------ properties

    Property selection() {
        return selectionModel.getSelectedObject();
    }
}
