/*
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.console.client.shared.subsys.elytron.ui;

import java.util.List;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.subsys.elytron.store.AddResourceGeneric;
import org.jboss.as.console.client.shared.subsys.elytron.store.ElytronStore;
import org.jboss.as.console.client.shared.subsys.elytron.store.RemoveResourceGeneric;
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

import static org.jboss.dmr.client.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.dmr.client.ModelDescriptionConstants.NAME;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class JdbcRealmView {

    private final Dispatcher circuit;
    private final ResourceDescription resourceDescription;
    private final SecurityContext securityContext;

    private DefaultCellTable<Property> table;
    private ListDataProvider<Property> dataProvider;
    private SingleSelectionModel<Property> selectionModel;
    private JdbcRealmPrincipalsQueryView principalsQueryViewView;

    public JdbcRealmView(final Dispatcher circuit,
            final ResourceDescription resourceDescription,
            final SecurityContext securityContext) {
        this.circuit = circuit;
        this.securityContext = securityContext;
        this.resourceDescription = resourceDescription;
    }

    public Widget asWidget() {

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), event -> onAdd()));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(),
                event -> {
                    if (selectionModel.getSelectedObject() != null) {
                        String name = selectionModel.getSelectedObject().getName();
                        Feedback.confirm(Console.MESSAGES.deleteTitle("JDBC Realm"),
                                Console.MESSAGES.deleteConfirm("JDBC Realm" + " '" + name + "'"),
                                isConfirmed -> {
                                    if (isConfirmed) {
                                        circuit.dispatch(new RemoveResourceGeneric(ElytronStore.JDBC_REALM_ADDRESS, name));
                                    }
                                });
                    }
                }));

        ProvidesKey<Property> providesKey = Property::getName;
        table = new DefaultCellTable<>(5, providesKey);
        dataProvider = new ListDataProvider<>(providesKey);
        dataProvider.addDataDisplay(table);
        selectionModel = new SingleSelectionModel<>(providesKey);
        table.setSelectionModel(selectionModel);
        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };
        table.addColumn(nameColumn, "Name");

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);

        principalsQueryViewView = new JdbcRealmPrincipalsQueryView();
        
        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline("JDBC Realm")
                .setDescription(SafeHtmlUtils.fromString(resourceDescription.get(DESCRIPTION).asString()))
                .setMasterTools(tools)
                .setMaster(Console.MESSAGES.available("JDBC Realm"), table)
                .addDetail("Principals Query", principalsQueryViewView.asWidget());

        selectionModel.addSelectionChangeHandler(event -> {
            Property keyStore = selectionModel.getSelectedObject();
            if (keyStore != null) {
                principalsQueryViewView.update(keyStore.getValue().get("principal-query").asList());
            } else {
                principalsQueryViewView.clearValues();
            }
        });
        table.setSelectionModel(selectionModel);

        return layoutBuilder.build();
    }

    private void onAdd() {

        // manipulate the descriptions to allow the add UI operation be able to create the jdbc-realm 
        // with sql and datasource at least
        // because the principal-query is a LIST of OBJECTS
        ModelNode principalQueryAttr = resourceDescription.get("operations").get("add").get("request-properties");

        principalQueryAttr.get("principal-query-sql").set(principalQueryAttr.get("principal-query").get("value-type").get("sql"));
        principalQueryAttr.get("principal-query-datasource").set(principalQueryAttr.get("principal-query").get("value-type").get("data-source"));
        
        ModelNodeFormBuilder.FormAssets addFormAssets = new ModelNodeFormBuilder()
                .setResourceDescription(resourceDescription)
                .setCreateMode(true)
                .exclude("principal-query")
                .include("principal-query-sql", "principal-query-datasource")
                .setSecurityContext(securityContext)
                .build();
        addFormAssets.getForm().setEnabled(true);
        
        DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle("JDBC Realm"));
        AddResourceDialog addDialog = new AddResourceDialog(addFormAssets, resourceDescription,
                new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        // The instance name must be part of the model node!
                        String name = payload.remove(NAME).asString();
                        String sql = payload.remove("principal-query-sql").asString();
                        String datasource = payload.remove("principal-query-datasource").asString();

                        // construct the payload as the principal-query attribute is a LIST of OBJECTS
                        ModelNode modelNode = payload.get("principal-query").addEmptyObject();
                        modelNode.get("sql").set(sql);
                        modelNode.get("data-source").set(datasource);
                        circuit.dispatch(new AddResourceGeneric(ElytronStore.JDBC_REALM_ADDRESS, new Property(name, payload)));
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

    protected void update(final List<Property> models) {
        dataProvider.setList(models);
        table.selectDefaultEntity();
        if (models.isEmpty()) {
            principalsQueryViewView.clearValues();
            selectionModel.clear();
        }
        SelectionChangeEvent.fire(selectionModel);
    }
    
}
