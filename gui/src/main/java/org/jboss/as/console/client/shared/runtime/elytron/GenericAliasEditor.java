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
package org.jboss.as.console.client.shared.runtime.elytron;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;

import static org.jboss.as.console.client.shared.runtime.elytron.ElytronRuntimePresenter.CREDENTIAL_STORE_TEMPLATE;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class GenericAliasEditor implements IsWidget {

    private DefaultCellTable<ModelNode> table;
    private ListDataProvider<ModelNode> dataProvider;
    private final SingleSelectionModel<ModelNode> selectionModel;
    private ResourceDescription resourceDescription;
    private SecurityContext securityContext;
    private String credentialStoreResource;
    private ElytronRuntimePresenter presenter;
    private ToolButton addButton;
    private ToolButton removeButton;
    private ToolButton editButton;

    GenericAliasEditor(ResourceDescription resourceDescription, SecurityContext securityContext) {

        this.resourceDescription = new ResourceDescription();
        this.resourceDescription.get(OPERATIONS).get(ADD).set(resourceDescription.get(OPERATIONS).get(ADD_ALIAS_OPERATION));
        this.resourceDescription.get(ATTRIBUTES).set(resourceDescription.get(OPERATIONS).get(ADD_ALIAS_OPERATION).get(REQUEST_PROPERTIES));
        this.securityContext = securityContext;
        selectionModel = new SingleSelectionModel<>();
    }

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
        selectionModel.addSelectionChangeHandler(selectionChangeEvent -> {
            ModelNode selected = selectionModel.getSelectedObject();
            if (selected != null) {
                removeButton.setEnabled(true);
                editButton.setEnabled(true);
            } else {
                removeButton.setEnabled(false);
                editButton.setEnabled(false);

            }
        });
        return panel;
    }

    private void setupTable() {
        table = new DefaultCellTable<>(10);
        table.setSelectionModel(selectionModel);

        Column<ModelNode, String> realmColumn = new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode prop) {
                return prop.asString();
            }
        };
        realmColumn.setSortable(true);
        table.addColumn(realmColumn, "Alias");
    }

    private ToolStrip setupTableButtons() {
        ToolStrip tools = new ToolStrip();
        addButton = new ToolButton(Console.CONSTANTS.common_label_add(), event -> launchEditAliasDialog(false));
        removeButton = new ToolButton(Console.CONSTANTS.common_label_delete(), event -> {
            final ModelNode selection = selectionModel.getSelectedObject();
            if (selection != null) {
                Feedback.confirm("Remove Alias", Console.MESSAGES.deleteConfirm("Alias "  + selection),
                        isConfirmed -> {
                            if (isConfirmed) {
                                presenter.removeAlias(CREDENTIAL_STORE_TEMPLATE, credentialStoreResource, selection.asString());
                            }
                        });
            }
        });
        editButton = new ToolButton(Console.CONSTANTS.common_label_setSecret(), event -> launchEditAliasDialog(true));
        addButton.setEnabled(false);
        removeButton.setEnabled(false);
        editButton.setEnabled(false);
        tools.addToolButtonRight(addButton);
        tools.addToolButtonRight(removeButton);
        tools.addToolButtonRight(editButton);
        return tools;
    }

    private void launchEditAliasDialog(boolean isEditOperation) {
        ModelNodeFormBuilder addFormBuilder = new ModelNodeFormBuilder()
                .setResourceDescription(resourceDescription)
                .setCreateMode(true)
                .unsorted()
                .setCreateNameAttribute(false)
                .setSecurityContext(securityContext);

        if (isEditOperation) {
            addFormBuilder.addFactory("alias", attributeDescription -> {
                TextBoxItem tx = new TextBoxItem("alias", "Alias");
                tx.setEnabled(false);
                tx.setValue(selectionModel.getSelectedObject().asString());
                return tx;
            });
        }
        ModelNodeFormBuilder.FormAssets addFormAssets = addFormBuilder.build();

        addFormAssets.getForm().setEnabled(true);

        DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle("Alias"));
        AddResourceDialog.Callback callback = new AddResourceDialog.Callback() {
            @Override
            public void onAdd(ModelNode payload) {
                presenter.saveAlias(CREDENTIAL_STORE_TEMPLATE, credentialStoreResource, payload, isEditOperation);
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
    }

    public void update(List<ModelNode> models) {
        if (!models.isEmpty()) {
            table.setRowCount(models.size(), true);
            List<ModelNode> dataList = dataProvider.getList();
            dataList.clear();
            dataList.addAll(models);
        } else {
            removeButton.setEnabled(false);
            editButton.setEnabled(false);
            clearValues();
        }
        selectionModel.clear();
    }

    public void clearValues() {
        dataProvider.setList(new ArrayList<>());
    }

    public void setCredentialReferenceName(final String name) {
        credentialStoreResource = name;
        if (name != null) {
            addButton.setEnabled(true);
        } else {
            addButton.setEnabled(false);
            removeButton.setEnabled(false);
            editButton.setEnabled(false);
        }
    }

    public void setPresenter(final ElytronRuntimePresenter presenter) {
        this.presenter = presenter;
    }
}