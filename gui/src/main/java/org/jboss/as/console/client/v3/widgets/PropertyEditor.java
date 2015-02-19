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

package org.jboss.as.console.client.v3.widgets;

import com.google.gwt.cell.client.FieldUpdater;
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
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultEditTextCell;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * Generic editor for properties which are saved as {@code "key" => {value => "value"}}. The key and the value are both
 * saved as strings in the DMR resource.
 * <p/>
 * The property editor relies on a set of dependencies:
 * <dl>
 * <dt>{@link PropertyManager}</dt>
 * <dd>The {@linkplain PropertyManager property manager} defines a set of callback methods which are invoked if a
 * property is added / modified or removed. Depending on the constructor, you can either use a
 * {@link DefaultPropertyManager} or provide a custom one. Please note that if you use a custom implementation,
 * there won't be any ootb support for {@code PropertyXXXEvent}s.</dd>
 * <dt>{@link AddResourceDialog}</dt>
 * <dd>If not specified otherwise, this property editor uses a predefined {@link AddResourceDialog} which is
 * launched when a new property is added. This predefined dialog is wired to the property manager.</dd>
 * </dl>
 *
 * @author Heiko Braun
 * @author David Bosschaert
 * @date 4/20/11
 */
public class PropertyEditor implements IsWidget {

    // ------------------------------------------------------ builder

    @SuppressWarnings("unused")
    public static class Builder {
        // mandatory parameter
        private final PropertyManager propertyManager;
        private final SecurityContext securityContext;
        private final AddressTemplate addressTemplate;
        private final ResourceDescription resourceDescription;

        // optional parameter
        private AddressTemplate operationAddress;
        private boolean inlineEditing;
        private boolean hideTools;
        private int numRows;
        private String addLabel;
        private DefaultWindow addDialog;
        private String removeLabel;

        /**
         * Builder for a new {@link PropertyEditor} using the {@link DefaultPropertyManager}.
         */
        public Builder(DispatchAsync dispatcher, StatementContext statementContext, SecurityContext securityContext,
                       AddressTemplate addressTemplate, ResourceDescription resourceDescription) {
            this(new DefaultPropertyManager(dispatcher, statementContext),
                    securityContext, addressTemplate, resourceDescription);
        }

        /**
         * Builder for a new {@link PropertyEditor} using a custom {@link PropertyManager}.
         */
        public Builder(PropertyManager propertyManager, SecurityContext securityContext,
                       AddressTemplate addressTemplate, ResourceDescription resourceDescription) {
            this.propertyManager = propertyManager;
            this.securityContext = securityContext;
            this.addressTemplate = addressTemplate;
            this.resourceDescription = resourceDescription;
            this.operationAddress = null;
            this.inlineEditing = false;
            this.hideTools = false;
            this.numRows = 5;
            this.addLabel = Console.CONSTANTS.common_label_add();
            this.addDialog = new AddDialog(propertyManager, securityContext, addressTemplate, resourceDescription);
            this.removeLabel = Console.CONSTANTS.common_label_delete();
        }

        /**
         * Overwrite the address template for the add / remove operations. By default the address template given at
         * creation time is used for the add / remove operations.
         *
         * @param operationAddress the address
         * @return this builder
         */
        public Builder operationAddress(AddressTemplate operationAddress) {
            this.operationAddress = operationAddress;
            return this;
        }

        public Builder inlineEditing(boolean inlineEditing) {
            this.inlineEditing = inlineEditing;
            return this;
        }

        public Builder hideTools(boolean hideTools) {
            this.hideTools = hideTools;
            return this;
        }

        public Builder numRows(int numRows) {
            this.numRows = numRows;
            return this;
        }

        public Builder addLabel(String addLabel) {
            this.addLabel = addLabel;
            return this;
        }

        public Builder addDialog(DefaultWindow addDialog) {
            this.addDialog = addDialog;
            return this;
        }

        public Builder removeLabel(String removeLabel) {
            this.removeLabel = removeLabel;
            return this;
        }

        public PropertyEditor build() {
            return new PropertyEditor(this);
        }
    }


    // ------------------------------------------------------ add dialog

    public static class AddDialog extends DefaultWindow {

        private final AddResourceDialog dialog;

        public AddDialog(final PropertyManager propertyManager, final SecurityContext securityContext,
                         final AddressTemplate addressTemplate, final ResourceDescription resourceDescription) {
            super(Console.CONSTANTS.common_label_add());
            this.dialog = new AddResourceDialog(securityContext, resourceDescription,
                    new AddResourceDialog.Callback() {
                        @Override
                        public void onAdd(ModelNode payload) {
                            Property property = new Property(payload.get(NAME).asString(), payload);
                            propertyManager.onAdd(addressTemplate, property, AddDialog.this);
                        }

                        @Override
                        public void onCancel() {
                            hide();
                        }
                    });
            setWidget(dialog);
        }
    }


    // ------------------------------------------------------ property editor

    private final PropertyManager propertyManager;
    private final AddressTemplate addressTemplate;
    private final ProvidesKey<Property> keyProvider;
    private final AddressTemplate operationAddress;
    private final boolean inlineEditing;
    private final boolean hideTools;
    private final int numRows;
    private final String addLabel;
    private final DefaultWindow addDialog;
    private final String removeLabel;

    private DefaultCellTable<Property> table;
    private ListDataProvider<Property> dataProvider;

    private PropertyEditor(Builder builder) {
        this.propertyManager = builder.propertyManager;
        this.addressTemplate = builder.addressTemplate;
        this.keyProvider = new ProvidesKey<Property>() {
            @Override
            public Object getKey(Property property) {
                return property.getName();
            }
        };

        this.operationAddress = builder.operationAddress;
        this.inlineEditing = builder.inlineEditing;
        this.hideTools = builder.hideTools;
        this.numRows = builder.numRows;
        this.addLabel = builder.addLabel;
        this.addDialog = builder.addDialog;
        this.removeLabel = builder.removeLabel;
    }

    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        VerticalPanel panel = new VerticalPanel();
        panel.addStyleName("fill-layout-width");

        // table
        table = new DefaultCellTable<>(numRows, keyProvider);
        dataProvider = new ListDataProvider<>(keyProvider);
        dataProvider.addDataDisplay(table);
        final SingleSelectionModel<Property> selectionModel = new SingleSelectionModel<>(keyProvider);
        table.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Property selection = selectionModel.getSelectedObject();
                if (selection == null) {
                    propertyManager.onDeselect(addressTemplate);
                } else {
                    propertyManager.onSelect(addressTemplate, selection);
                }
            }
        });

        // columns
        Column<Property, String> keyColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property property) {
                return property.getName();
            }
        };
        keyColumn.setSortable(true);
        Column<Property, String> valueColumn;
        if (inlineEditing) {
            valueColumn = new Column<Property, String>(new DefaultEditTextCell()) {
                {
                    setFieldUpdater(new FieldUpdater<Property, String>() {
                        @Override
                        public void update(int index, Property property, String value) {
                            property.getValue().get(VALUE).set(value);
                        }
                    });
                }

                @Override
                public String getValue(Property property) {
                    return property.getValue().get(VALUE).asString();
                }
            };
        } else {
            valueColumn = new TextColumn<Property>() {
                @Override
                public String getValue(Property property) {
                    return property.getValue().get(VALUE).asString();
                }
            };
        }
        ColumnSortEvent.ListHandler<Property> sortHandler = new ColumnSortEvent.ListHandler<>(dataProvider.getList());
        sortHandler.setComparator(keyColumn, new Comparator<Property>() {
            @Override
            public int compare(Property o1, Property o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        table.addColumn(keyColumn, Console.CONSTANTS.common_label_key());
        table.addColumn(valueColumn, Console.CONSTANTS.common_label_value());
        table.setColumnWidth(keyColumn, 40, Style.Unit.PCT);
        table.setColumnWidth(valueColumn, 60, Style.Unit.PCT);
        table.addColumnSortHandler(sortHandler);
        table.getColumnSortList().push(keyColumn);

        // tools
        if (!hideTools) {
            ToolStrip tools = new ToolStrip();
            ToolButton addButton = new ToolButton(addLabel, new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    if (addDialog instanceof AddDialog) {
                        ((AddDialog) addDialog).dialog.clearValues();
                    }
                    propertyManager.openAddDialog(addressTemplate, addDialog);
                }
            });
            ToolButton removeButton = new ToolButton(removeLabel, new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    final Property selection = selectionModel.getSelectedObject();
                    if (selection != null) {
                        Feedback.confirm(Console.MESSAGES.removeProperty(),
                                Console.MESSAGES.removePropertyConfirm(selection.getName()),
                                new Feedback.ConfirmationHandler() {
                                    @Override
                                    public void onConfirmation(boolean isConfirmed) {
                                        if (isConfirmed) {
                                            propertyManager.onRemove(addressTemplate, selection);
                                        }
                                    }
                                });
                    }
                }
            });
            AddressTemplate effectiveAddress = operationAddress != null ? operationAddress : addressTemplate;
            if (effectiveAddress != null) {
                addButton.setOperationAddress(effectiveAddress.getTemplate(), ADD);
                removeButton.setOperationAddress(effectiveAddress.getTemplate(), REMOVE);
            }
            tools.addToolButtonRight(addButton);
            tools.addToolButtonRight(removeButton);
            panel.add(tools);
        }

        panel.add(table);
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        panel.add(pager);
        return panel;
    }

    public void update(List<Property> properties) {
        table.setRowCount(properties.size(), true);

        List<Property> dataList = dataProvider.getList();
        dataList.clear(); // cannot call setList() as that breaks the sort handler
        dataList.addAll(properties);

        // Make sure the new values are properly sorted
        ColumnSortEvent.fire(table, table.getColumnSortList());
    }

    public void clearValues() {
        dataProvider.setList(new ArrayList<Property>());
    }
}
