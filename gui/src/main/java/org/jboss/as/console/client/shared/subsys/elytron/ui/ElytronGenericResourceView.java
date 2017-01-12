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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.jboss.as.console.client.shared.subsys.elytron.store.ModifyResourceGeneric;
import org.jboss.as.console.client.shared.subsys.elytron.store.RemoveResourceGeneric;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
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
public class ElytronGenericResourceView {

    protected final Dispatcher circuit;
    protected ResourceDescription resourceDescription;
    protected String title;
    protected AddressTemplate addressTemplate;
    protected final SecurityContext securityContext;
    private boolean onAddFormRequiredOnly = true;

    private DefaultCellTable<Property> table;
    private ListDataProvider<Property> dataProvider;
    protected SingleSelectionModel<Property> selectionModel;
    protected ModelNodeFormBuilder.FormAssets modelForm;
    private Set<String> excludes = new LinkedHashSet<>();

    public ElytronGenericResourceView(final Dispatcher circuit, final ResourceDescription resourceDescription,
            final SecurityContext securityContext, String title, AddressTemplate addressTemplate) {
        this.circuit = circuit;
        this.securityContext = securityContext;
        this.resourceDescription = resourceDescription;
        this.title = title;
        this.addressTemplate = addressTemplate;
    }

    public Widget asWidget() {

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), event -> onAdd()));
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
        selectionModel = new SingleSelectionModel<>(providesKey);
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
                .setConfigOnly()
                .setResourceDescription(resourceDescription)
                .exclude(excludes.toArray(new String[excludes.size()]))
                .setSecurityContext(securityContext);
        modelForm = formBuilder.build();
        modelForm.getForm().addFormValidator((formItemList, formValidation) -> {
            addFormValidatorOnAddDialog(formItemList, formValidation);
        });

        List<String> forItems = modelForm.getForm().getFormItemNames();
        int numberOfFormItems = forItems.size();

        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline(title)
                .setDescription(SafeHtmlUtils.fromString(resourceDescription.get(DESCRIPTION).asString()))
                .setMasterTools(tools)
                .setMaster(Console.MESSAGES.available(title), table);

        if (numberOfFormItems > 0) {
            modelForm.getForm().setToolsCallback(new FormCallback() {
                @Override
                @SuppressWarnings("unchecked")
                public void onSave(final Map changeset) {
                    circuit.dispatch(new ModifyResourceGeneric(addressTemplate, selectionModel.getSelectedObject().getName(), changeset));
                }

                @Override
                public void onCancel(final Object entity) {
                    modelForm.getForm().cancel();
                }
            });

            VerticalPanel formPanel = new VerticalPanel();
            formPanel.setStyleName("fill-layout-width");
            formPanel.add(modelForm.getHelp().asWidget());
            formPanel.add(modelForm.getForm().asWidget());

            layoutBuilder.addDetail(Console.CONSTANTS.common_label_attributes(), formPanel);
        }


        Map<String, Widget> tabDetails = additionalTabDetails();
        for (String detailName: tabDetails.keySet()) {
            layoutBuilder.addDetail(detailName, tabDetails.get(detailName));
        }

        selectionModel.addSelectionChangeHandler(event -> {
            Property selectedProperty = selectionModel.getSelectedObject();
            if (selectedProperty != null) {
                modelForm.getForm().edit(selectedProperty.getValue());
            } else {
                modelForm.getForm().clearValues();
            }
            selectTableItem(selectedProperty);
        });
        table.setSelectionModel(selectionModel);

        return layoutBuilder.build();
    }

    private void onAdd() {

        DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle(title));
        AddResourceDialog.Callback callback = new AddResourceDialog.Callback() {
            @Override
            public void onAdd(ModelNode payload) {
                // The instance name must be part of the model node!
                String name = payload.get(NAME).asString();
                onAddCallback(payload);
                circuit.dispatch(new AddResourceGeneric(addressTemplate, new Property(name, payload)));
                dialog.hide();
            }

            @Override
            public void onCancel() {
                dialog.hide();
            }
        };
        AddResourceDialog addDialog = null;
        if (excludes.isEmpty()) {
            addDialog = new AddResourceDialog(securityContext, resourceDescription, callback);
            addDialog.setRequiredOnly(onAddFormRequiredOnly);
        } else {
            ModelNodeFormBuilder.FormAssets addFormAssets = customFormOnAdd();
            addFormAssets.getForm().setEnabled(true);
            addDialog = new AddResourceDialog(addFormAssets, resourceDescription, callback);
        }

        Widget addDialogWidget = addDialog.asWidget();
        addDialog.getForm().addFormValidator((formItemList, formValidation) -> {
            addFormValidatorOnAddDialog(formItemList, formValidation);
        });
        dialog.setWidth(480);
        dialog.setHeight(360);
        dialog.setWidget(addDialogWidget);
        dialog.setGlassEnabled(true);
        dialog.center();
    }

    protected ModelNodeFormBuilder.FormAssets customFormOnAdd() {
        return new ModelNodeFormBuilder()
                .setResourceDescription(resourceDescription)
                .setCreateMode(true)
                .unsorted()
                .exclude(excludes.toArray(new String[excludes.size()]))
                .setSecurityContext(securityContext)
                .build();
    }

    public void update(final List<Property> models) {
        dataProvider.setList(models);
        table.selectDefaultEntity();
        if (models.isEmpty()) {
            modelForm.getForm().clearValues();
            selectionModel.clear();
        }
        SelectionChangeEvent.fire(selectionModel);
    }

    public void excludesFormAttributes(String... excludes) {
        this.excludes.addAll(Arrays.asList(excludes));
    }

    <T> FormItem<T> findFormItem(List<FormItem> formItems, String name) {
        FormItem selectedFormItem = null;
        for (FormItem formItem : formItems) {
            if (name.equals(formItem.getName())) {
                selectedFormItem = formItem;
                break;
            }
        }
        return selectedFormItem;
    }

    public void setOnAddFormRequiredOnly(final boolean onAddFormRequiredOnly) {
        this.onAddFormRequiredOnly = onAddFormRequiredOnly;
    }

    public Map<String, Widget> additionalTabDetails() {
        return Collections.emptyMap();
    }

    protected void onAddCallback(final ModelNode payload) { }

    protected void selectTableItem(final Property keyManagerProp) {  }

    protected void addFormValidatorOnAddDialog(List<FormItem> formItemList, FormValidation formValidation) {}

}
