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
import java.util.Collections;
import java.util.List;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.subsys.elytron.store.AddListAttribute;
import org.jboss.as.console.client.shared.subsys.elytron.store.ElytronStore;
import org.jboss.as.console.client.shared.subsys.elytron.store.RemoveListAttribute;
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
public class NewIdentityAttributesView implements IsWidget {


    private final ProvidesKey<ModelNode> nameProvider;

    private DefaultCellTable<ModelNode> table;
    private ListDataProvider<ModelNode> dataProvider;
    private final SingleSelectionModel<ModelNode> selectionModel;
    private Dispatcher circuit;
    private ResourceDescription resourceDescription;
    private SecurityContext securityContext;
    private String ldapRealmName;
    private ToolButton addButton;
    private ToolButton removeButton;

    NewIdentityAttributesView(final Dispatcher circuit, ResourceDescription resourceDescription,
            SecurityContext securityContext) {

        this.circuit = circuit;
        this.securityContext = securityContext;
        this.nameProvider = modelNode -> modelNode.get(NAME);
        selectionModel = new SingleSelectionModel<>(nameProvider);

        // tweak to use ModelNodeFormBuilder automatic form generation
        this.resourceDescription = new ResourceDescription(resourceDescription.clone());
        // adds the identity-mappings.new-identity-attributes to the request-properties of add operation
        ModelNode reqPropsDescription = this.resourceDescription.get(OPERATIONS).get(ADD).get(REQUEST_PROPERTIES);
        // adds the identity-mappings.new-identity-attributes to the attribute list
        ModelNode attrPropsDescription = this.resourceDescription.get(ATTRIBUTES);
        ModelNode newIdentityAttributesDescription = reqPropsDescription.get("identity-mapping").get(VALUE_TYPE).get("new-identity-attributes").get(VALUE_TYPE);
        reqPropsDescription.set(newIdentityAttributesDescription);
        attrPropsDescription.set(newIdentityAttributesDescription);

    }

    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        VerticalPanel panel = new VerticalPanel();
        panel.addStyleName("fill-layout-width");

        // table
        table = new DefaultCellTable<>(20, nameProvider);
        dataProvider = new ListDataProvider<>(nameProvider);
        dataProvider.addDataDisplay(table);
        table.setSelectionModel(selectionModel);

        // columns
        Column<ModelNode, String> nameColumn = new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode node) {
                return node.get(NAME).asString();
            }
        };
        nameColumn.setSortable(true);
        Column<ModelNode, String> valueColumn = new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode node) {
                return node.get(VALUE).asString().replaceAll("\\[|\"|\\]", "");
            }
        };
        valueColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        ColumnSortEvent.ListHandler<ModelNode> sortHandler = new ColumnSortEvent.ListHandler<>(dataProvider.getList());
        sortHandler.setComparator(nameColumn, (o1, o2) -> o1.get(NAME).asString().toLowerCase().compareTo(o2.get(NAME).asString().toLowerCase()));
        table.addColumn(nameColumn, "Name");
        table.addColumn(valueColumn, "Value");
        table.setColumnWidth(nameColumn, 30, Style.Unit.PCT);
        table.setColumnWidth(valueColumn, 70, Style.Unit.PCT);
        table.addColumnSortHandler(sortHandler);
        table.getColumnSortList().push(nameColumn);

        panel.add(mainTableTools());
        panel.add(table);

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        panel.add(pager);
        return panel;
    }

    private ToolStrip mainTableTools() {
        ToolStrip tools = new ToolStrip();

        addButton = new ToolButton(Console.CONSTANTS.common_label_add(), event -> {

            ModelNodeFormBuilder.FormAssets addFormAssets = new ModelNodeFormBuilder()
                    .setResourceDescription(resourceDescription)
                    .setCreateMode(true)
                    .unsorted()
                    .setSecurityContext(securityContext)
                    .build();
            addFormAssets.getForm().setEnabled(true);

            DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle("New Identity Attribute"));
            AddResourceDialog.Callback callback = new AddResourceDialog.Callback() {
                @Override
                public void onAdd(ModelNode payload) {
                    circuit.dispatch(new AddListAttribute(ElytronStore.LDAP_REALM_ADDRESS,
                            "identity-mapping.new-identity-attributes",
                            ldapRealmName,
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
            dialog.setHeight(350);
            dialog.setWidget(addDialog);
            dialog.setGlassEnabled(true);
            dialog.center();
        });

        removeButton = new ToolButton(Console.CONSTANTS.common_label_delete(), event -> {
            final ModelNode selection = selectionModel.getSelectedObject();
            if (selection != null) {
                Feedback.confirm("New Identity Attribute", Console.MESSAGES.deleteConfirm("New Identity Attribute "  + selection.asString()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                circuit.dispatch(new RemoveListAttribute(
                                        ElytronStore.LDAP_REALM_ADDRESS,
                                        ldapRealmName,
                                        "identity-mapping.new-identity-attributes",
                                        selection));
                            }
                        });
            }
        });
        tools.addToolButtonRight(addButton);
        tools.addToolButtonRight(removeButton);
        return tools;
    }

    public void update(Property ldapRealmProperty) {
        ldapRealmName = ldapRealmProperty.getName();

        if (ldapRealmProperty != null) {
            addButton.setEnabled(true);
            removeButton.setEnabled(true);
        } else {
            addButton.setEnabled(false);
            removeButton.setEnabled(false);
        }

        if (ldapRealmProperty.getValue().get("identity-mapping").hasDefined("new-identity-attributes")) {
            // wrap in a new list as later the list will change as a result of sort operation
            List<ModelNode> models = new ArrayList<>(ldapRealmProperty.getValue().get("identity-mapping").get("new-identity-attributes").asList());
            table.setRowCount(models.size(), true);

            Collections.sort(models,
                (o1, o2) -> o1.get(NAME).asString().toLowerCase().compareTo(o2.get(NAME).asString().toLowerCase()));

            List<ModelNode> dataList = dataProvider.getList();
            dataList.clear(); // cannot call setList() as that breaks the sort handler
            dataList.addAll(models);

            // Make sure the new values are properly sorted
            ColumnSortEvent.fire(table, table.getColumnSortList());
        } else {
            dataProvider.setList(new ArrayList<>());
        }
    }

    public void clearValues() {
        dataProvider.setList(new ArrayList<>());
        addButton.setEnabled(false);
        removeButton.setEnabled(false);
    }

}