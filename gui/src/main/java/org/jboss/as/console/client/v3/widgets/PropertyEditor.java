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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultEditTextCell;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import static org.jboss.dmr.client.ModelDescriptionConstants.VALUE;

/**
 * Generic editor for properties which are saved as {@code "key" => {value => "value"}}. The key and the value are both
 * saved as strings in the DMR resource.
 * <p>
 * The property editor relies on a set of dependencies:
 * <dl>
 * <dt>{@link PropertyManager}</dt>
 * <dd>The {@linkplain PropertyManager property manager} defines a set of callback methods which are invoked if a
 * property is added / modified or removed. Depending on the constructor, you can either use a
 * {@link SubResourcePropertyManager} or provide a custom one. Please note that if you use a custom implementation,
 * there won't be any ootb support for {@code PropertyXXXEvent}s.</dd>
 * <dt>{@link AddResourceDialog}</dt>
 * <dd>If not specified otherwise, this property editor uses a predefined {@link AddResourceDialog} which is
 * launched when a new property is added. This predefined dialog is wired to the property manager.</dd>
 * </dl>
 *
 * <b>Deprecated:</b> Use model driven widgets instead
 *
 * @author Heiko Braun
 * @author David Bosschaert
 * @author Harald Pehl
 * @date 4/20/11
 */
public class PropertyEditor implements IsWidget {

    // ------------------------------------------------------ builder


    public static class Builder {

        // mandatory parameter
        private final PropertyManager propertyManager;

        // optional parameter
        private AddressTemplate operationAddress;
        private boolean inlineEditing;
        private boolean hideTools;
        private int numRows;
        private String addLabel;
        private AddPropertyDialog addDialog;
        private String removeLabel;

        /**
         * Builder for a new {@link PropertyEditor}.
         */
        public Builder(PropertyManager propertyManager) {
            this.propertyManager = propertyManager;
            this.operationAddress = null;
            this.inlineEditing = false;
            this.hideTools = false;
            this.numRows = 5;
            this.addLabel = Console.CONSTANTS.common_label_add();
            this.removeLabel = Console.CONSTANTS.common_label_delete();
        }

        /**
         * Overwrite the address template for the add / remove operations. By default the address template of the
         * property manager is used for the add / remove operations.
         *
         * @param operationAddress the address
         *
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

        public Builder addDialog(AddPropertyDialog addDialog) {
            this.addDialog = addDialog;
            return this;
        }

        public Builder removeLabel(String removeLabel) {
            this.removeLabel = removeLabel;
            return this;
        }

        public PropertyEditor build() {
            if (addDialog == null && !inlineEditing) {
                throw new IllegalStateException("Unable to create property editor for " + propertyManager.getAddress() +
                        ": You have to specify an add dialog if inline editing is disabled");
            }
            return new PropertyEditor(this);
        }
    }


    // ------------------------------------------------------ property editor

    private final PropertyManager propertyManager;
    private final ProvidesKey<Property> keyProvider;
    private final AddressTemplate operationAddress;
    private final boolean inlineEditing;
    private final boolean hideTools;
    private final int numRows;
    private final String addLabel;
    private final AddPropertyDialog addDialog;
    private final String removeLabel;
    private ToolButton addButton;
    private ToolButton removeButton;

    private DefaultCellTable<Property> table;
    private ListDataProvider<Property> dataProvider;

    private PropertyEditor(Builder builder) {
        this.propertyManager = builder.propertyManager;
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
        selectionModel.addSelectionChangeHandler(event -> {
            Property selection = selectionModel.getSelectedObject();
            if (selection == null) {
                propertyManager.onDeselect();
            } else {
                propertyManager.onSelect(selection);
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
            valueColumn = new Column<Property, String>(new DefaultEditTextCell()) { {
                    setFieldUpdater((index, property, value) -> getPropertyValue(property).set(value));
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
                    return getPropertyValue(property).asString();
                }
            };
        }
        ColumnSortEvent.ListHandler<Property> sortHandler = new ColumnSortEvent.ListHandler<>(dataProvider.getList());
        sortHandler.setComparator(keyColumn, new Comparator<Property>() {
            @Override
            public int compare(Property o1, Property o2) {
                return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
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
            addButton = new ToolButton(addLabel, event -> {
                addDialog.clearValues();
                propertyManager.openAddDialog(addDialog);
            });
            removeButton = new ToolButton(removeLabel, event -> {
                final Property selection = selectionModel.getSelectedObject();
                if (selection != null) {
                    Feedback.confirm(Console.MESSAGES.removeProperty(),
                            Console.MESSAGES.removePropertyConfirm(selection.getName()),
                            new Feedback.ConfirmationHandler() {
                                @Override
                                public void onConfirmation(boolean isConfirmed) {
                                    if (isConfirmed) {
                                        propertyManager.onRemove(selection);
                                    }
                                }
                            });
                }
            });
            AddressTemplate effectiveAddress = operationAddress != null ? operationAddress : propertyManager
                    .getAddress();
            if (effectiveAddress != null) {
                addButton.setOperationAddress(effectiveAddress.getTemplate(), propertyManager.getAddOperationName());
                removeButton
                        .setOperationAddress(effectiveAddress.getTemplate(), propertyManager.getRemoveOperationName());
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

        Collections.sort(properties, new Comparator<Property>() {
            @Override
            public int compare(Property o1, Property o2) {
                return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
            }
        });
        
        List<Property> dataList = dataProvider.getList();
        dataList.clear(); // cannot call setList() as that breaks the sort handler
        dataList.addAll(properties);

        // Make sure the new values are properly sorted
        ColumnSortEvent.fire(table, table.getColumnSortList());
    }

    public void clearValues() {
        dataProvider.setList(new ArrayList<Property>());
    }

    private ModelNode getPropertyValue(Property property) {
        ModelNode value;
        if (property.getValue().hasDefined(VALUE)) {
            value = property.getValue().get(VALUE);
        } else {
            value = property.getValue();
        }
        return value;
    }
    
    public void enableToolButtons(boolean enable) {
        addButton.setEnabled(enable);
        removeButton.setEnabled(enable);
    }
}
