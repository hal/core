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
package org.jboss.as.console.client.shared.subsys.ws;

import static org.jboss.dmr.client.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.dmr.client.ModelDescriptionConstants.NAME;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.subsys.ws.store.CreateHandler;
import org.jboss.as.console.client.shared.subsys.ws.store.DeleteHandler;
import org.jboss.as.console.client.shared.subsys.ws.store.UpdateHandler;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 * @date 3/31/2016
 */
class HandlerEditor implements IsWidget {

    protected final SecurityContext securityContext;
    protected final ResourceDescription resourceDescription;
    private final Dispatcher circuit;
    private AddressTemplate addressTemplate;

    private final DefaultCellTable<Property> table;
    private final ListDataProvider<Property> dataProvider;
    private final SingleSelectionModel<Property> selectionModel;
    private ModelNodeFormBuilder.FormAssets formAssets;
    private ContentHeaderLabel headlineWidget = new ContentHeaderLabel();
    private HandlerClassEditor handlerClassEditor;
    private String configName;

    public HandlerEditor(Dispatcher circuit, SecurityContext securityContext,
                         AddressTemplate addressTemplate,
                         ResourceDescription resourceDescription) {

        this.securityContext = securityContext;
        this.resourceDescription = resourceDescription;
        this.circuit = circuit;
        this.addressTemplate = addressTemplate;

        ProvidesKey<Property> providesKey = Property::getName;

        table = new DefaultCellTable<>(5, providesKey);
        dataProvider = new ListDataProvider<>(providesKey);
        selectionModel = new SingleSelectionModel<>(providesKey);
        //noinspection unchecked
        dataProvider.addDataDisplay(table);
        table.setSelectionModel(selectionModel);
    }

    public Widget asWidget() {

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), event -> onAdd()));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(), event -> {
            if (selectionModel.getSelectedObject() != null) {
                onRemove(selectionModel.getSelectedObject().getName());
            }
        }));

        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };
        table.addColumn(nameColumn, Console.CONSTANTS.common_label_name());

        formAssets = new ModelNodeFormBuilder()
            .setConfigOnly()
            .setResourceDescription(resourceDescription)
            .setSecurityContext(securityContext).build();

        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeSet) {
                HandlerEditor.this.onModify(selectionModel.getSelectedObject().getName(), formAssets.getForm().getChangedValues());
            }

            @Override
            public void onCancel(Object entity) {
                formAssets.getForm().cancel();
            }
        });

        selectionModel.addSelectionChangeHandler(event -> {
            Property property = selectionModel.getSelectedObject();
            if (property != null) {
                updateHandlerList();
            } else {
                clearDetail();
            }
        });

        ResourceDescription handlerDescription = resourceDescription.getChildDescription("handler");
        handlerClassEditor = new HandlerClassEditor(addressTemplate.append("handler=*"), circuit, securityContext, handlerDescription);

        VerticalPanel formPanel = new VerticalPanel();
        formPanel.setStyleName("fill-layout-width");
        formPanel.add(formAssets.getHelp().asWidget());
        formPanel.add(formAssets.getForm().asWidget());

        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
        .setPlain(true)
        .setHeadlineWidget(headlineWidget)
        .setDescription(SafeHtmlUtils.fromString(resourceDescription.get(DESCRIPTION).asString()))
        .setMasterTools(tools)
        .setMaster(Console.MESSAGES.available(null), table)
        .addDetail(Console.CONSTANTS.common_label_attributes(), formPanel)
        .addDetail("Handler classes", handlerClassEditor.asWidget());

        return layoutBuilder.build();
    }

    // ------------------------------------------------------ select & update

    Property getSelection() {
        return selectionModel.getSelectedObject();
    }

    protected void updateMaster(String configName, final ModelNode currentConfig, String resourceType) {
        List<Property> models = currentConfig.get(resourceType).asPropertyList();

        Collections.sort(models, (o1, o2) -> o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase()));

        this.configName = configName;
        headlineWidget.setText(configName);
        dataProvider.setList(models);
        if (models.isEmpty()) {
            selectionModel.clear();
            handlerClassEditor.clearValues();
        } else {
            table.selectDefaultEntity();
            updateHandlerList();
        }
    }

    private void updateHandlerList() {
        ModelNode handlerItem = selectionModel.getSelectedObject().getValue();
        formAssets.getForm().edit(handlerItem);
        handlerClassEditor.updateOperationAddressNames(configName, getSelection().getName());
        if (handlerItem.hasDefined("handler")) {
            List<Property> handlers = handlerItem.get("handler").asPropertyList();
            handlerClassEditor.update(handlers);
        } else {
            handlerClassEditor.clearValues();
        }
    }

    protected void clearDetail() {
        formAssets.getForm().clearValues();
    }

    protected void onAdd() {
        final DefaultWindow dialog = new DefaultWindow(configName);
        AddResourceDialog addDialog = new AddResourceDialog(securityContext, resourceDescription,
                new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        // The instance name must be part of the model node!
                        String instanceName = payload.get(NAME).asString();
                        AddressTemplate preHandlerResource = addressTemplate.replaceWildcards(configName);
                        circuit.dispatch(new CreateHandler(preHandlerResource, instanceName, payload));
                        dialog.hide();
                    }

                    @Override
                    public void onCancel() {
                        dialog.hide();
                    }
                });
        dialog.setWidth(480);
        dialog.setHeight(360);
        dialog.setWidget(addDialog);
        dialog.setGlassEnabled(true);
        dialog.center();
    }

    protected void onRemove(final String name) {
        Feedback.confirm(Console.MESSAGES.deleteTitle(name),
                Console.MESSAGES.deleteConfirm("pre handler " + " '" + name + "'"),
                isConfirmed -> {
                    if (isConfirmed) {
                        AddressTemplate preHandlerResource = addressTemplate.replaceWildcards(configName);
                        circuit.dispatch(new DeleteHandler(preHandlerResource, name));
                    }
                });
    }

    protected void onModify(String name, Map<String, Object> changedValues) {
        AddressTemplate preHandlerResource = addressTemplate.replaceWildcards(configName);
        circuit.dispatch(new UpdateHandler(preHandlerResource, name, changedValues));
    }


}
