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
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.subsys.elytron.store.AddResourceGeneric;
import org.jboss.as.console.client.shared.subsys.elytron.store.ModifyComplexAttribute;
import org.jboss.as.console.client.shared.subsys.elytron.store.RemoveResourceGeneric;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

import static org.jboss.dmr.client.ModelDescriptionConstants.DESCRIPTION;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class PolicyView {

    private static final String CUSTOM_POLICY = "custom-policy";
    private static final String JACC_POLICY = "jacc-policy";

    private final Dispatcher circuit;
    private final ResourceDescription resourceDescription;
    private final SecurityContext securityContext;
    private final String title;
    private final AddressTemplate addressTemplate;

    private ToolButton addButton;
    private DefaultCellTable<Property> table;
    private ListDataProvider<Property> dataProvider;
    private SingleSelectionModel<Property> selectionModel;
    private DeckPanel forms;
    private ModelNodeFormBuilder.FormAssets customPolicyForm;
    private ModelNodeFormBuilder.FormAssets jaccPolicyForm;

    PolicyView(Dispatcher circuit,
            ResourceDescription resourceDescription,
            SecurityContext securityContext, String title,
            AddressTemplate addressTemplate) {
        this.circuit = circuit;
        this.resourceDescription = resourceDescription;
        this.securityContext = securityContext;
        this.title = title;
        this.addressTemplate = addressTemplate;
    }

    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        ToolStrip tools = new ToolStrip();
        addButton = new ToolButton(Console.CONSTANTS.common_label_add(), event -> onAdd());
        tools.addToolButtonRight(addButton);
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(),
                event -> {
                    if (selectionModel.getSelectedObject() != null) {
                        String name = selectionModel.getSelectedObject().getName();
                        Feedback.confirm(Console.MESSAGES.deleteTitle(title),
                                Console.MESSAGES.deleteConfirm(title + " '" + name + "'"),
                                isConfirmed -> {
                                    if (isConfirmed) {
                                        circuit.dispatch(new RemoveResourceGeneric(addressTemplate, name));
                                    }
                                });
                    }
                }));

        ProvidesKey<Property> providesKey = Property::getName;
        table = new DefaultCellTable<>(5, providesKey);
        dataProvider = new ListDataProvider<>(providesKey);
        dataProvider.addDataDisplay(table);
        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };
        TextColumn<Property> typeColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                if (node.getValue().hasDefined(CUSTOM_POLICY)) {
                    return "Custom Policy";
                } else if (node.getValue().hasDefined(JACC_POLICY)) {
                    return "JACC Policy";
                } else {
                    return "n/a";
                }
            }
        };
        table.addColumn(nameColumn, "Name");
        table.addColumn(typeColumn, "Type");

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);

        selectionModel = new SingleSelectionModel<>(providesKey);
        selectionModel.addSelectionChangeHandler(event -> {
            Property selectedProperty = selectionModel.getSelectedObject();
            if (selectedProperty != null) {
                if (selectedProperty.getValue().hasDefined(CUSTOM_POLICY)) {
                    forms.showWidget(1);
                } else if (selectedProperty.getValue().hasDefined(JACC_POLICY)) {
                    forms.showWidget(2);
                } else {
                    forms.showWidget(0);
                }
            } else {
                forms.showWidget(0);
            }
            selectTableItem(selectedProperty);

        });
        table.setSelectionModel(selectionModel);

        ResourceDescription customPolicyDescription = complexAttributeDescription(resourceDescription, CUSTOM_POLICY);
        ModelNodeFormBuilder customPolicyFormBuilder = new ModelNodeFormBuilder()
                .include("class-name", "module")
                .unsorted()
                .setResourceDescription(customPolicyDescription)
                .setSecurityContext(securityContext);
        customPolicyForm = customPolicyFormBuilder.build();
        customPolicyForm.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(Map changeset) {
                circuit.dispatch(new ModifyComplexAttribute(addressTemplate, CUSTOM_POLICY,
                        selectionModel.getSelectedObject().getName(), customPolicyForm.getForm().getUpdatedEntity()));
            }

            @Override
            public void onCancel(final Object entity) {
                customPolicyForm.getForm().cancel();
            }
        });

        ResourceDescription jaccPolicyDescription = complexAttributeDescription(resourceDescription, JACC_POLICY);
        ModelNodeFormBuilder jaccPolicyFormBuilder = new ModelNodeFormBuilder()
                .include("policy", "configuration-factory", "module")
                .unsorted()
                .setResourceDescription(jaccPolicyDescription)
                .setSecurityContext(securityContext);
        jaccPolicyForm = jaccPolicyFormBuilder.build();
        jaccPolicyForm.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(Map changeset) {
                circuit.dispatch(new ModifyComplexAttribute(addressTemplate, JACC_POLICY,
                        selectionModel.getSelectedObject().getName(), jaccPolicyForm.getForm().getUpdatedEntity()));
            }

            @Override
            public void onCancel(final Object entity) {
                jaccPolicyForm.getForm().cancel();
            }
        });

        forms = new DeckPanel();
        forms.add(new Label());
        forms.add(customPolicyForm.asWidget());
        forms.add(jaccPolicyForm.asWidget());
        forms.showWidget(0);

        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline(title)
                .setDescription(SafeHtmlUtils.fromString(resourceDescription.get(DESCRIPTION).asString()))
                .setMasterTools(tools)
                .setMaster(Console.MESSAGES.available(title), table)
                .addDetail(Console.CONSTANTS.common_label_attributes(), forms.asWidget());
        return layoutBuilder.build();
    }

    private void onAdd() {
        NewPolicyWizard wizard = new NewPolicyWizard(resourceDescription, securityContext, (customPolicy, payload) -> {
            String name = payload.get("name").asString();
            payload.remove("name");
            ModelNode modelNode = new ModelNode();
            if (customPolicy) {
                modelNode.get(CUSTOM_POLICY).set(payload);
            } else {
                modelNode.get(JACC_POLICY).set(payload);
            }
            Property property = new Property(name, modelNode);
            circuit.dispatch(new AddResourceGeneric(addressTemplate, property));
        });
        wizard.open(Console.MESSAGES.newTitle("Policy"));
    }

    public void update(List<Property> models) {
        dataProvider.setList(models);
        addButton.setEnabled(models.isEmpty());
        if (models.isEmpty()) {
            forms.showWidget(0);
            customPolicyForm.getForm().clearValues();
            jaccPolicyForm.getForm().clearValues();
            selectionModel.clear();
        }
        table.selectDefaultEntity();
        SelectionChangeEvent.fire(selectionModel);
    }

    private void selectTableItem(Property prop) {
        if (prop != null) {
            if (prop.getValue().hasDefined(CUSTOM_POLICY)) {
                customPolicyForm.getForm().edit(prop.getValue().get(CUSTOM_POLICY));
                jaccPolicyForm.getForm().clearValues();
            } else if (prop.getValue().hasDefined(JACC_POLICY)) {
                customPolicyForm.getForm().clearValues();
                jaccPolicyForm.getForm().edit(prop.getValue().get(JACC_POLICY));
            } else {
                customPolicyForm.getForm().clearValues();
                jaccPolicyForm.getForm().clearValues();
            }
        } else {
            customPolicyForm.getForm().clearValues();
            jaccPolicyForm.getForm().clearValues();
        }
    }

    private ResourceDescription complexAttributeDescription(ResourceDescription resourceDescription, String name) {
        ModelNode payload = new ModelNode();

        ModelNode attribute = resourceDescription.get("attributes").get(name);
        payload.get("description").set(attribute.get("description"));
        for (Property property : attribute.get("value-type").asPropertyList()) {
            payload.get("attributes").get(property.getName()).set(property.getValue());
        }
        return new ResourceDescription(payload);
    }
}
