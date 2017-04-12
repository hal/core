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

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.cellview.client.Column;
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

import static org.jboss.dmr.client.ModelDescriptionConstants.ADD;
import static org.jboss.dmr.client.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.dmr.client.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.dmr.client.ModelDescriptionConstants.VALUE_TYPE;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class JdbcRealmPrincipalsQueryView implements IsWidget {


    private final ProvidesKey<ModelNode> nameProvider;
    private DefaultCellTable<ModelNode> table;
    private ListDataProvider<ModelNode> dataProvider;
    private final SingleSelectionModel<ModelNode> selectionModel;
    private Dispatcher circuit;
    private ResourceDescription resourceDescription;
    private SecurityContext securityContext;
    private String jdbcRealmName;

    JdbcRealmPrincipalsQueryView(final Dispatcher circuit, ResourceDescription resourceDescription,
            SecurityContext securityContext) {
        this.circuit = circuit;
        this.securityContext = securityContext;
        this.nameProvider = modelNode -> modelNode.get("sql");
        selectionModel = new SingleSelectionModel<>(nameProvider);

        this.resourceDescription = new ResourceDescription(resourceDescription.clone());
        ModelNode reqPropsDescription = this.resourceDescription.get(OPERATIONS).get(ADD).get(REQUEST_PROPERTIES);
        ModelNode principalQueryDescription = reqPropsDescription.get("principal-query").get(VALUE_TYPE);
        reqPropsDescription.set(principalQueryDescription);
    }

    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        VerticalPanel panel = new VerticalPanel();
        panel.addStyleName("fill-layout-width");

        // table
        table = new DefaultCellTable<>(5, nameProvider);
        dataProvider = new ListDataProvider<>(nameProvider);
        dataProvider.addDataDisplay(table);
        table.setSelectionModel(selectionModel);

        // columns
        Column<ModelNode, String> sqlColumn = new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode node) {
                return node.get("sql").asString();
            }
        };
        sqlColumn.setSortable(true);
        Column<ModelNode, String> datasourceColumn = new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode node) {
                return node.get("data-source").asString();
            }
        };

        table.addColumn(sqlColumn, "SQL");
        table.addColumn(datasourceColumn, "Datasource");
        table.setColumnWidth(sqlColumn, 85, Style.Unit.PCT);
        table.setColumnWidth(datasourceColumn, 15, Style.Unit.PCT);
        datasourceColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        panel.add(setupTableButtons());
        panel.add(table);
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        panel.add(pager);
        return panel;
    }

    private ToolStrip setupTableButtons() {
        ToolStrip tools = new ToolStrip();
        ToolButton addButton = new ToolButton(Console.CONSTANTS.common_label_add(), event -> {

            ModelNodeFormBuilder.FormAssets addFormAssets = new ModelNodeFormBuilder()
                    .setResourceDescription(resourceDescription)
                    .setCreateMode(true)
                    .unsorted()
                    .exclude("clear-password-mapper", "bcrypt-mapper", "salted-simple-digest-mapper",
                            "simple-digest-mapper", "scram-mapper")
                    .setCreateNameAttribute(false)
                    .setSecurityContext(securityContext)
                    .build();
            addFormAssets.getForm().setEnabled(true);

            DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle("Principal Query"));
            AddResourceDialog.Callback callback = new AddResourceDialog.Callback() {
                @Override
                public void onAdd(ModelNode payload) {
                    circuit.dispatch(new AddListAttribute(ElytronStore.JDBC_REALM_ADDRESS,
                            "principal-query",
                            jdbcRealmName,
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
        ToolButton removeButton = new ToolButton(Console.CONSTANTS.common_label_delete(), event -> {
            final ModelNode selection = selectionModel.getSelectedObject();
            if (selection != null) {
                Feedback.confirm("Principal Query", Console.MESSAGES.deleteConfirm("Principal Query "  + selection.get("sql").asString()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                circuit.dispatch(new RemoveListAttribute(
                                        ElytronStore.JDBC_REALM_ADDRESS,
                                        jdbcRealmName,
                                        "principal-query",
                                        selection));
                            }
                        });
            }
        });
        tools.addToolButtonRight(addButton);
        tools.addToolButtonRight(removeButton);
        return tools;
    }


    public void update(Property prop) {
        jdbcRealmName = prop.getName();
        List<ModelNode> aliases = prop.getValue().get("principal-query").asList();
        table.setRowCount(aliases.size(), true);
        List<ModelNode> dataList = dataProvider.getList();
        dataList.clear(); // cannot call setList() as that breaks the sort handler
        dataList.addAll(aliases);
    }

    public void clearValues() {
        dataProvider.setList(new ArrayList<>());
    }

}