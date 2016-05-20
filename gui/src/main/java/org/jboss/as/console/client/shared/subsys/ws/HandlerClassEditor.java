/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.console.client.shared.subsys.ws;

import static org.jboss.dmr.client.ModelDescriptionConstants.NAME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.subsys.ws.store.CreateHandler;
import org.jboss.as.console.client.shared.subsys.ws.store.DeleteHandler;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelDescriptionConstants;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;

/**
 * Specific editor for the handler class of pre/post handler of endpoint/client configuration.
 * 
 * @author Claudio Miranda <claudio@redhat.com>
 * @date 3/31/2016
 */
public class HandlerClassEditor implements IsWidget {

    protected final SecurityContext securityContext;
    protected final ResourceDescription resourceDescription;
    private final Dispatcher circuit;

    private final ProvidesKey<Property> nameProvider;
    private final AddressTemplate operationAddress;
    private final int numRows = 5;
    private AddressTemplate resolvedOperationAddress;

    private DefaultCellTable<Property> table;
    private ListDataProvider<Property> dataProvider;

    HandlerClassEditor(AddressTemplate operationAddress, Dispatcher circuit, SecurityContext securityContext,
            ResourceDescription resourceDescription) {

        this.nameProvider = Property::getName;

        this.operationAddress = operationAddress;
        this.securityContext = securityContext;
        this.resourceDescription = resourceDescription;
        this.circuit = circuit;
    }

    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        VerticalPanel panel = new VerticalPanel();
        panel.addStyleName("fill-layout-width");

        // table
        table = new DefaultCellTable<>(numRows, nameProvider);
        dataProvider = new ListDataProvider<>(nameProvider);
        dataProvider.addDataDisplay(table);
        final SingleSelectionModel<Property> selectionModel = new SingleSelectionModel<>(nameProvider);
        table.setSelectionModel(selectionModel);

        // columns
        Column<Property, String> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property property) {
                return property.getName();
            }
        };
        nameColumn.setSortable(true);
        Column<Property, String> classColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property property) {
                return getClassValue(property).asString();
            }
        };
        ColumnSortEvent.ListHandler<Property> sortHandler = new ColumnSortEvent.ListHandler<>(dataProvider.getList());
        sortHandler.setComparator(nameColumn, (o1, o2) -> o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase()));
        table.addColumn(nameColumn, Console.CONSTANTS.common_label_name());
        table.addColumn(classColumn, "Handler class");
        table.setColumnWidth(nameColumn, 40, Style.Unit.PCT);
        table.setColumnWidth(classColumn, 60, Style.Unit.PCT);
        table.addColumnSortHandler(sortHandler);
        table.getColumnSortList().push(nameColumn);

        // tools
        ToolStrip tools = new ToolStrip();
        ToolButton addButton = new ToolButton(Console.CONSTANTS.common_label_add(), event -> onAdd());
        ToolButton removeButton = new ToolButton(Console.CONSTANTS.common_label_delete(), event -> {
            final Property selection = selectionModel.getSelectedObject();
            if (selection != null) {
                Feedback.confirm(Console.CONSTANTS.subsys_ws_remove_handler(), Console.MESSAGES.deleteConfirm("handler class "  + selection.getName()),
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    circuit.dispatch(new DeleteHandler(resolvedOperationAddress, selection.getName()));
                                }
                            }
                        });
            }
        });
        addButton.setOperationAddress(operationAddress.getTemplate(), ModelDescriptionConstants.ADD);
        removeButton.setOperationAddress(operationAddress.getTemplate(), ModelDescriptionConstants.REMOVE);
        tools.addToolButtonRight(addButton);
        tools.addToolButtonRight(removeButton);
        panel.add(tools);

        panel.add(table);
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        panel.add(pager);
        return panel;
    }

    public void update(List<Property> handlerClasses) {
        table.setRowCount(handlerClasses.size(), true);

        Collections.sort(handlerClasses, (o1, o2) -> o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase()));

        List<Property> dataList = dataProvider.getList();
        dataList.clear(); // cannot call setList() as that breaks the sort handler
        dataList.addAll(handlerClasses);

        // Make sure the new values are properly sorted
        ColumnSortEvent.fire(table, table.getColumnSortList());
    }

    void updateOperationAddressNames(String configName, String handlerName) {
        resolvedOperationAddress = operationAddress.replaceWildcards(configName, handlerName);
    }

    protected void onAdd() {
        final DefaultWindow dialog = new DefaultWindow("Add a handler class");
        AddResourceDialog addDialog = new AddResourceDialog(securityContext, resourceDescription,
                new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        // The instance name must be part of the model node!
                        String instanceName = payload.get(NAME).asString();
                        circuit.dispatch(new CreateHandler(resolvedOperationAddress, instanceName, payload));
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


    public void clearValues() {
        dataProvider.setList(new ArrayList<Property>());
    }

    private ModelNode getClassValue(Property property) {
        ModelNode value;
        if (property.getValue().hasDefined("class")) {
            value = property.getValue().get("class");
        } else {
            value = property.getValue();
        }
        return value;
    }
}
