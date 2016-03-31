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
import java.util.Comparator;
import java.util.List;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.subsys.ws.store.CreateConfig;
import org.jboss.as.console.client.shared.subsys.ws.store.DeleteConfig;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.client.v3.widgets.PropertyEditor;
import org.jboss.as.console.client.v3.widgets.SubResourceAddPropertyDialog;
import org.jboss.as.console.client.v3.widgets.SubResourcePropertyManager;
import org.jboss.as.console.client.widgets.tables.ViewLinkCell;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.circuit.Dispatcher;
import org.useware.kernel.gui.behaviour.StatementContext;

import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

class ConfigEditorWS implements IsWidget {

    protected final SecurityContext securityContext;
    protected final ResourceDescription resourceDescription;

    private final Dispatcher circuit;
    private final DispatchAsync dispatcher;
    private final StatementContext statementContext;
    private final DefaultCellTable<Property> table;
    private final ListDataProvider<Property> dataProvider;
    private final SingleSelectionModel<Property> selectionModel;
    private final AddressTemplate addressTemplate;
    private final WebServicePresenter presenter;
    private ModelNodeFormBuilder.FormAssets formAssets;
    private PropertyEditor propertyEditor;
    private final String title;

    public ConfigEditorWS(DispatchAsync dispatcher, Dispatcher circuit, SecurityContext securityContext,
            StatementContext statementContext, AddressTemplate addressTemplate,
            ResourceDescription resourceDescription, String title, WebServicePresenter presenter) {

        this.circuit = circuit;
        this.dispatcher = dispatcher;
        this.securityContext = securityContext;
        this.statementContext = statementContext;
        this.addressTemplate = addressTemplate;
        this.resourceDescription = resourceDescription;
        this.title = title;
        this.presenter = presenter;

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

    public Widget asWidget() {

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

        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };
        nameColumn.setSortable(true);

        Column<Property, String> option = new Column<Property, String>(
                new ViewLinkCell<String>("View handlers", new ActionCell.Delegate<String>() {
                    @Override
                    public void execute(String selection) {
                        presenter.setHandler(getSelection(), addressTemplate);
                    }
                })
        ) {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };

        ColumnSortEvent.ListHandler<Property> sortHandler = new ColumnSortEvent.ListHandler<>(dataProvider.getList());
        sortHandler.setComparator(nameColumn, new Comparator<Property>() {
            @Override
            public int compare(Property o1, Property o2) {
                return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
            }
        });

        table.addColumn(nameColumn, Console.CONSTANTS.common_label_name());
        table.addColumn(option, Console.CONSTANTS.common_label_option());

        table.addColumnSortHandler(sortHandler);
        table.getColumnSortList().push(nameColumn);

        formAssets = new ModelNodeFormBuilder()
            .setConfigOnly()
            .setResourceDescription(resourceDescription)
            .setSecurityContext(securityContext).build();

        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Property property = selectionModel.getSelectedObject();
                if (property != null) {
                    updateDetail(property.getValue());
                    presenter.setConfigName(getSelection().getName());
                } else {
                    clearDetail();
                }
            }
        });

        // begin - code to initialize the property editor
        ResourceDescription propDescription = resourceDescription.getChildDescription("property");

        EndpointSelectionAwareContext endpointContext = new EndpointSelectionAwareContext(statementContext, this);
        AddressTemplate propAddress = addressTemplate.replaceWildcards(EndpointSelectionAwareContext.SELECTED_ENTITY).append("property=*");
        SubResourcePropertyManager propertyManager = new SubResourcePropertyManager(propAddress, endpointContext, dispatcher);

        SubResourceAddPropertyDialog addDialog = new SubResourceAddPropertyDialog(propertyManager, securityContext, propDescription);
        propertyEditor = new PropertyEditor.Builder(propertyManager)
                .addDialog(addDialog)
                .operationAddress(addressTemplate.append("property=*"))
                .build();
        // end

        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
        .setPlain(true)
        .setHeadline(title)
        .setDescription(SafeHtmlUtils.fromString(resourceDescription.get(DESCRIPTION).asString()))
        .setMasterTools(tools)
        .setMaster(Console.MESSAGES.available(title), table)
        .addDetail(Console.CONSTANTS.common_label_properties(), propertyEditor.asWidget());

        return layoutBuilder.build();
    }

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
        Collections.sort(models, new Comparator<Property>() {
            @Override
            public int compare(Property o1, Property o2) {
                return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
            }
        });

        dataProvider.setList(models);
        if (models.isEmpty()) {
            selectionModel.clear();
        } else {
            table.selectDefaultEntity();
            updateDetail(getSelection().getValue());
        }
        ColumnSortEvent.fire(table, table.getColumnSortList());
    }

    protected void updateDetail(ModelNode currentConfig) {
        formAssets.getForm().edit(currentConfig);
        if (currentConfig.hasDefined("property")) {
            List<Property> properties = currentConfig.get("property").asPropertyList();
            propertyEditor.update(properties);
        } else {
            propertyEditor.clearValues();
        }
    }

    protected void clearDetail() {
        formAssets.getForm().clearValues();
        propertyEditor.clearValues();
    }

    /**
     *  Add dialog for the endpoint/client config.
     */
    private void onAdd() {
        final DefaultWindow dialog = new DefaultWindow(title);
        AddResourceDialog addDialog = new AddResourceDialog(securityContext, resourceDescription,
                new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        // The instance name must be part of the model node!
                        String instanceName = payload.get(NAME).asString();
                        circuit.dispatch(new CreateConfig(addressTemplate, instanceName, payload));
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

    /**
     *  Remove dialog for the endpoint/client config.
     */
    private void onRemove(final String name) {
        Feedback.confirm(Console.MESSAGES.deleteTitle(title),
                Console.MESSAGES.deleteConfirm(title + " '" + name + "'"),
                new Feedback.ConfirmationHandler() {
                    @Override
                    public void onConfirmation(boolean isConfirmed) {
                        if (isConfirmed) {
                            circuit.dispatch(new DeleteConfig(addressTemplate, name));
                        }
                    }
                });
    }

    public void notifyDefaultSelection() {
        presenter.setConfigName(getSelection().getName());
    }
}
