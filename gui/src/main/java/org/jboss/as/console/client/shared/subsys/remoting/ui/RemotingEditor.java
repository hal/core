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
package org.jboss.as.console.client.shared.subsys.remoting.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.PropertyEditor;
import org.jboss.as.console.client.v3.widgets.SubResourceAddPropertyDialog;
import org.jboss.as.console.client.v3.widgets.SubResourcePropertyManager;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.List;
import java.util.Map;

import static org.jboss.as.console.client.shared.subsys.remoting.ui.RemotingSelectionAwareContext.SELECTED_ENTITY;

/**
 * Generic editor for remoting sub resources.
 *
 * @author Harald Pehl
 */
abstract class RemotingEditor implements IsWidget {

    protected final SecurityContext securityContext;
    protected final ResourceDescription resourceDescription;

    private final DispatchAsync dispatcher;
    private final StatementContext statementContext;
    private final AddressTemplate addressTemplate;
    private final DefaultCellTable<Property> table;
    private final ListDataProvider<Property> dataProvider;
    private final SingleSelectionModel<Property> selectionModel;
    private ModelNodeFormBuilder.FormAssets formAssets;
    private PropertyEditor propertyEditor;

    RemotingEditor(DispatchAsync dispatcher, SecurityContext securityContext,
                   StatementContext statementContext, AddressTemplate addressTemplate,
                   ResourceDescription resourceDescription) {
        this.dispatcher = dispatcher;
        this.securityContext = securityContext;
        this.statementContext = statementContext;
        this.addressTemplate = addressTemplate;
        this.resourceDescription = resourceDescription;

        ProvidesKey<Property> providesKey = new ProvidesKey<Property>() {
            @Override
            public Object getKey(Property item) {
                return item.getName();
            }
        };
        table = new DefaultCellTable<>(5, providesKey);
        dataProvider = new ListDataProvider<>(providesKey);
        selectionModel = new SingleSelectionModel<>(providesKey);
        //noinspection unchecked
        dataProvider.addDataDisplay(table);
        table.setSelectionModel(selectionModel);
    }

    protected ToolStrip tools() {
        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onAdd();
            }
        }));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (selectionModel.getSelectedObject() != null) {
                    onRemove(selectionModel.getSelectedObject().getName());
                }
            }
        }));
        return tools;
    }

    @SuppressWarnings("unchecked")
    protected DefaultCellTable<Property> table() {
        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };
        table.addColumn(nameColumn, "Name");
        return table;
    }

    protected Widget formPanel() {
        formAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext).build();
        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeSet) {
                RemotingEditor.this.onModify(selectionModel.getSelectedObject().getName(), formAssets.getForm().getChangedValues());
            }

            @Override
            public void onCancel(Object entity) {
                formAssets.getForm().cancel();
            }
        });

        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Property property = selectionModel.getSelectedObject();
                if (property != null) {
                    updateDetail(property);
                } else {
                    clearDetail();
                }
            }
        });

        VerticalPanel formPanel = new VerticalPanel();
        formPanel.setStyleName("fill-layout-width");
        formPanel.add(formAssets.getHelp().asWidget());
        formPanel.add(formAssets.getForm().asWidget());
        return formPanel;
    }

    protected PropertyEditor propertyEditor() {
        RemotingSelectionAwareContext propertyContext = new RemotingSelectionAwareContext(statementContext, this);
        AddressTemplate propertyAddress = addressTemplate.replaceWildcards(SELECTED_ENTITY).append("property=*");
        ResourceDescription propertyDescription = resourceDescription.getChildDescription("property");

        SubResourcePropertyManager propertyManager = new SubResourcePropertyManager(propertyAddress, propertyContext,
                dispatcher);
        SubResourceAddPropertyDialog addDialog = new SubResourceAddPropertyDialog(propertyManager, securityContext,
                propertyDescription);
        propertyEditor = new PropertyEditor.Builder(propertyManager)
                // using propertyAddress would cause an exception when looking for
                // "{selected.profile}/subsystem=remoting/connector={selected.entity}/property=*"
                // so we have to use the original address and append "property=*" which results in
                // "{selected.profile}/subsystem=remoting/connector=*/property=*"
                .operationAddress(addressTemplate.append("property=*"))
                .addDialog(addDialog)
                .build();
        return propertyEditor;
    }


    // ------------------------------------------------------ abstract methods

    protected abstract void onAdd();

    protected abstract void onRemove(final String name);

    protected abstract void onModify(final String name, final Map<String, Object> changedValues);


    // ------------------------------------------------------ select & update

    void select(String key) {
        Property hit = null;
        for (Property property : dataProvider.getList()) {
            if (property.getName().equals(key)) {
                hit = property;
                break;
            }
        }
        if (hit != null) {
            selectionModel.setSelected(hit, true);
        } else {
            table.selectDefaultEntity();
        }
    }

    Property getSelection() {
        return selectionModel.getSelectedObject();
    }

    protected void updateMaster(final List<Property> models) {
        dataProvider.setList(models);
        if (models.isEmpty()) {
            selectionModel.clear();
        } else {
            table.selectDefaultEntity();
            updateDetail(getSelection());
        }
    }

    protected void updateDetail(Property property) {
        formAssets.getForm().edit(property.getValue());
        if (property.getValue().hasDefined("property")) {
            List<Property> properties = property.getValue().get("property").asPropertyList();
            propertyEditor.update(properties);
        } else {
            propertyEditor.clearValues();
        }
    }

    protected void clearDetail() {
        formAssets.getForm().clearValues();
        propertyEditor.clearValues();
    }
}
