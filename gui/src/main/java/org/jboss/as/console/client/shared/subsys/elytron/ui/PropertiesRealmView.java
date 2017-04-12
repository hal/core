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
import java.util.Map;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.subsys.elytron.store.AddResourceGeneric;
import org.jboss.as.console.client.shared.subsys.elytron.store.ElytronStore;
import org.jboss.as.console.client.shared.subsys.elytron.store.ModifyComplexAttribute;
import org.jboss.as.console.client.shared.subsys.elytron.store.ModifyResourceGeneric;
import org.jboss.as.console.client.shared.subsys.elytron.store.RemoveResourceGeneric;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.mbui.widgets.ComplexAttributeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
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
public class PropertiesRealmView {

    private final Dispatcher circuit;
    private final ResourceDescription resourceDescription;
    private final SecurityContext securityContext;

    private DefaultCellTable<Property> table;
    private ListDataProvider<Property> dataProvider;
    private SingleSelectionModel<Property> selectionModel;
    private ModelNodeFormBuilder.FormAssets modelFormAsset;
    private ModelNodeFormBuilder.FormAssets groupsPropertiesFormAssets;
    private ModelNodeFormBuilder.FormAssets usersPropertiesFormAssets;

    public PropertiesRealmView(final Dispatcher circuit,
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
                        Feedback.confirm(Console.MESSAGES.deleteTitle("Properties Realm"),
                                Console.MESSAGES.deleteConfirm("Properties Realm" + " '" + name + "'"),
                                isConfirmed -> {
                                    if (isConfirmed) {
                                        circuit.dispatch(new RemoveResourceGeneric(ElytronStore.PROPERTIES_REALM_ADDRESS, name));
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

        ModelNodeFormBuilder formBuilder = new ModelNodeFormBuilder()
                .setResourceDescription(resourceDescription)
                .exclude("groups-properties", "users-properties")
                .setConfigOnly()
                .setSecurityContext(securityContext);
        modelFormAsset = formBuilder.build();

        modelFormAsset.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                circuit.dispatch(new ModifyResourceGeneric(ElytronStore.PROPERTIES_REALM_ADDRESS, selectionModel.getSelectedObject().getName(), changeset));
            }

            @Override
            public void onCancel(final Object entity) {
                modelFormAsset.getForm().cancel();
            }
        });

        groupsPropertiesFormAssets = new ComplexAttributeForm("groups-properties", securityContext, resourceDescription).build();
        usersPropertiesFormAssets = new ComplexAttributeForm("users-properties", securityContext, resourceDescription).build();

        groupsPropertiesFormAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                circuit.dispatch(new ModifyComplexAttribute(ElytronStore.PROPERTIES_REALM_ADDRESS, "groups-properties",
                        selectionModel.getSelectedObject().getName(), groupsPropertiesFormAssets.getForm().getUpdatedEntity()));
            }

            @Override
            public void onCancel(final Object entity) {
                groupsPropertiesFormAssets.getForm().cancel();
            }
        });

        usersPropertiesFormAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                circuit.dispatch(new ModifyComplexAttribute(ElytronStore.PROPERTIES_REALM_ADDRESS, "users-properties",
                        selectionModel.getSelectedObject().getName(), usersPropertiesFormAssets.getForm().getUpdatedEntity()));
            }

            @Override
            public void onCancel(final Object entity) {
                usersPropertiesFormAssets.getForm().cancel();
            }
        });


        VerticalPanel formPanel = new VerticalPanel();
        formPanel.setStyleName("fill-layout-width");
        formPanel.add(modelFormAsset.getHelp().asWidget());
        formPanel.add(modelFormAsset.getForm().asWidget());

        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline("Properties Realm")
                .setDescription(SafeHtmlUtils.fromString(resourceDescription.get(DESCRIPTION).asString()))
                .setMasterTools(tools)
                .setMaster(Console.MESSAGES.available("Properties Realm"), table)
                .addDetail(Console.CONSTANTS.common_label_attributes(), modelFormAsset.asWidget())
                .addDetail("Users Properties", usersPropertiesFormAssets.asWidget())
                .addDetail("Groups Properties", groupsPropertiesFormAssets.asWidget());

        selectionModel.addSelectionChangeHandler(event -> {
            Property currentProp = selectionModel.getSelectedObject();
            if (currentProp != null) {
                modelFormAsset.getForm().edit(currentProp.getValue());
                if (currentProp.getValue().hasDefined("groups-properties"))
                    groupsPropertiesFormAssets.getForm().edit(currentProp.getValue().get("groups-properties"));
                else
                    groupsPropertiesFormAssets.getForm().editTransient(new ModelNode());

                if (currentProp.getValue().hasDefined("users-properties"))
                    usersPropertiesFormAssets.getForm().edit(currentProp.getValue().get("users-properties"));

            } else {
                modelFormAsset.getForm().clearValues();
                groupsPropertiesFormAssets.getForm().clearValues();
                usersPropertiesFormAssets.getForm().clearValues();
            }
        });
        table.setSelectionModel(selectionModel);

        return layoutBuilder.build();
    }

    private void onAdd() {

        ModelNode addAttributes = resourceDescription.get(OPERATIONS).get(ADD).get(REQUEST_PROPERTIES);

        addAttributes.get("users-properties-path").set(addAttributes.get("users-properties").get(VALUE_TYPE).get(PATH));
        addAttributes.get("users-properties-relative-to").set(addAttributes.get("users-properties").get(VALUE_TYPE).get("relative-to"));

        ModelNodeFormBuilder.FormAssets addFormAssets = new ModelNodeFormBuilder()
                .setResourceDescription(resourceDescription)
                .setCreateMode(true)
                .createValidators(false)
                .exclude("users-properties")
                .include("users-properties-path", "users-properties-relative-to")
                .setSecurityContext(securityContext)
                .build();
        addFormAssets.getForm().setEnabled(true);

        DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle("Properties Realm"));
        AddResourceDialog addDialog = new AddResourceDialog(addFormAssets, resourceDescription,
                new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        // The instance name must be part of the model node!
                        String name = payload.remove(NAME).asString();

                        String path = payload.remove("users-properties-path").asString();
                        payload.get("users-properties").get("path").set(path);

                        ModelNode userPropertiesRelativeTo = payload.remove("users-properties-relative-to");
                        if (userPropertiesRelativeTo.isDefined()) {
                            String relativeto = userPropertiesRelativeTo.asString();
                            payload.get("users-properties").get("relative-to").set(relativeto);
                        }
                        circuit.dispatch(new AddResourceGeneric(ElytronStore.PROPERTIES_REALM_ADDRESS, new Property(name, payload)));
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
            modelFormAsset.getForm().clearValues();
            groupsPropertiesFormAssets.getForm().clearValues();
            usersPropertiesFormAssets.getForm().clearValues();
            selectionModel.clear();
        }
        SelectionChangeEvent.fire(selectionModel);
    }

}
