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
package org.jboss.as.console.client.shared.subsys.activemq;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.subsys.activemq.cluster.MsgClusteringPresenter;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
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

import static org.jboss.dmr.client.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.dmr.client.ModelDescriptionConstants.NAME;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class GenericResourceView {

    protected ResourceDescription resourceDescription;
    protected String title;
    private ContentHeaderLabel headerLabel;
    protected AddressTemplate addressTemplate;
    protected SingleSelectionModel<Property> selectionModel;
    protected ModelNodeFormBuilder.FormAssets modelForm;
    protected MsgClusteringPresenter presenter;
    private SecurityContext securityContext;
    private DefaultCellTable<Property> table;
    private ListDataProvider<Property> dataProvider;

    public GenericResourceView(final ResourceDescription resourceDescription, MsgClusteringPresenter presenter,
            String title, AddressTemplate addressTemplate) {
        this.presenter = presenter;
        this.resourceDescription = resourceDescription;
        this.title = title;
        this.addressTemplate = addressTemplate;
    }

    public Widget asWidget() {

        headerLabel = new ContentHeaderLabel();
        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), event -> launchAddDialog()));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(),
                event -> {
                    if (selectionModel.getSelectedObject() != null) {
                        String name = selectionModel.getSelectedObject().getName();
                        Feedback.confirm(Console.MESSAGES.deleteTitle(title),
                                Console.MESSAGES.deleteConfirm(title + " '" + name + "'"),
                                isConfirmed -> {
                                    if (isConfirmed) {
                                        presenter.onRemoveResource(addressTemplate, name);
                                        presenter.loadDetails();
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

        securityContext = presenter.getSecurityFramework()
                .getSecurityContext(NameTokens.ActivemqMsgClusteringPresenter);

        ModelNodeFormBuilder formBuilder = new ModelNodeFormBuilder()
                .setConfigOnly()
                .createValidators(true)
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext);

        customEditFormBuilder(formBuilder);

        modelForm = formBuilder.build();
        modelForm.getForm().addFormValidator((formItemList, formValidation) -> {
            addFormValidatorOnAddDialog(formItemList, formValidation);
        });

        List<String> forItems = modelForm.getForm().getFormItemNames();
        int numberOfFormItems = forItems.size();

        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadlineWidget(headerLabel)
                .setDescription(SafeHtmlUtils.fromString(resourceDescription.get(DESCRIPTION).asString()))
                .setMasterTools(tools)
                .setMaster(Console.MESSAGES.available(title), table);

        if (numberOfFormItems > 0) {
            modelForm.getForm().setToolsCallback(new FormCallback() {
                @Override
                @SuppressWarnings("unchecked")
                public void onSave(final Map changeset) {
                    presenter.onSaveResource(addressTemplate, selectionModel.getSelectedObject().getName(), changeset);
                    presenter.loadDetails();
                }

                @Override
                public void onCancel(final Object entity) {
                    modelForm.getForm().cancel();
                }
            });

            layoutBuilder.addDetail(Console.CONSTANTS.common_label_attributes(), modelForm.asWidget());
        }


        Map<String, Widget> tabDetails = additionalTabDetails();
        for (String detailName : tabDetails.keySet()) {
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

    private void launchAddDialog() {

        DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle(title));
        AddResourceDialog.Callback callback = new AddResourceDialog.Callback() {
            @Override
            public void onAdd(ModelNode payload) {
                // The instance name must be part of the model node!
                String name = payload.get(NAME).asString();
                onAddCallbackBeforeSubmit(payload);
                presenter.onAddSubResource(addressTemplate, payload);
                presenter.loadDetails();
                dialog.hide();
            }

            @Override
            public void onCancel() {
                dialog.hide();
            }
        };
        ModelNodeFormBuilder.FormAssets addFormAssets = customFormOnAdd();
        addFormAssets.getForm().setEnabled(true);
        AddResourceDialog addDialog = new AddResourceDialog(addFormAssets, resourceDescription, callback);

        Widget addDialogWidget = addDialog.asWidget();
        addDialog.getForm().addFormValidator((formItemList, formValidation) ->
            addFormValidatorOnAddDialog(formItemList, formValidation)
        );
        dialog.setWidth(480);
        dialog.setHeight(360);
        dialog.setWidget(addDialogWidget);
        dialog.setGlassEnabled(true);
        dialog.center();
    }

    protected ModelNodeFormBuilder.FormAssets customFormOnAdd() {
        ModelNodeFormBuilder builder = new ModelNodeFormBuilder()
                .setResourceDescription(resourceDescription)
                .setCreateMode(true)
                .unsorted()
                .setSecurityContext(securityContext)
                .createValidators(true);

        customAddFormBuilder(builder);
        return builder.build();
    }

    public void update(final List<Property> models) {
        headerLabel.setText(title + ": Provider " + presenter.getCurrentServer());
        dataProvider.setList(models);
        table.selectDefaultEntity();
        if (models.isEmpty()) {
            modelForm.getForm().clearValues();
            selectionModel.clear();
        }
        SelectionChangeEvent.fire(selectionModel);
    }

    protected final <T> FormItem<T> findFormItem(List<FormItem> formItems, String name) {
        FormItem selectedFormItem = null;
        for (FormItem formItem : formItems) {
            if (name.equals(formItem.getName())) {
                selectedFormItem = formItem;
                break;
            }
        }
        return selectedFormItem;
    }

    public Map<String, Widget> additionalTabDetails() {
        return Collections.emptyMap();
    }

    protected void customEditFormBuilder(final ModelNodeFormBuilder formBuilder) { }

    protected void customAddFormBuilder(final ModelNodeFormBuilder formBuilder) { }

    /**
     * Call this implementation if there is a need to customize the payload before submit it to persist the model.
     *
     * @param payload
     */
    protected void onAddCallbackBeforeSubmit(final ModelNode payload) {
    }

    protected void selectTableItem(final Property keyManagerProp) { }

    protected void addFormValidatorOnAddDialog(List<FormItem> formItemList, FormValidation formValidation) { }

}
