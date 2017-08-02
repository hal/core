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
package org.jboss.as.console.client.shared.subsys.undertow;

import java.util.List;
import java.util.Map;

import com.google.gwt.dom.client.Style;
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
import org.jboss.as.console.client.shared.subsys.elytron.CredentialReferenceFormValidation;
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

import static org.jboss.as.console.client.shared.subsys.undertow.HttpPresenter.APPLICATION_SECURITY_DOMAIN;
import static org.jboss.as.console.client.shared.subsys.undertow.HttpPresenter.APPLICATION_SECURITY_DOMAIN_SSO;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class ApplicationSecurityDomainResourceView {

    protected final SecurityContext securityContext;
    protected SingleSelectionModel<Property> selectionModel;
    private ModelNodeFormBuilder.FormAssets formAttributesAssets;
    private ModelNodeFormBuilder.FormAssets ssoAddFormAssets;
    private DefaultCellTable<Property> table;
    private ListDataProvider<Property> dataProvider;
    private HttpPresenter presenter;

    public ApplicationSecurityDomainResourceView(final HttpPresenter presenter) {
        this.presenter = presenter;
        this.securityContext = presenter.getSecurityFramework().getSecurityContext(presenter.getProxy().getNameToken());
    }

    public Widget asWidget() {

        String title = Console.CONSTANTS.undertowApplicationSecurityDomain();
        ResourceDescription resourceDescription = presenter.getDescriptionRegistry().lookup(APPLICATION_SECURITY_DOMAIN);
        ResourceDescription ssoDescription = presenter.getDescriptionRegistry().lookup(APPLICATION_SECURITY_DOMAIN_SSO);

        // repackage credential-reference inner attributes to show up in the ADD modal dialog
        // as the credential-reference is a required=true
        ssoDescription.repackageComplexAttribute(CREDENTIAL_REFERENCE);

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), event -> onAdd(resourceDescription)));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(),
                event -> {
                    if (selectionModel.getSelectedObject() != null) {
                        String name = selectionModel.getSelectedObject().getName();
                        Feedback.confirm(Console.MESSAGES.deleteTitle(title),
                                Console.MESSAGES.deleteConfirm(title + " '" + name + "'"),
                                isConfirmed -> {
                                    if (isConfirmed) {
                                        presenter.onRemoveResource(APPLICATION_SECURITY_DOMAIN,
                                                selectionModel.getSelectedObject().getName());
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

        formAttributesAssets = new ModelNodeFormBuilder()
                .createValidators(true)
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext)
                .build();

        formAttributesAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                presenter.onSaveResource(APPLICATION_SECURITY_DOMAIN, selectionModel.getSelectedObject().getName(),
                        changeset);
            }

            @Override
            public void onCancel(final Object entity) {
                formAttributesAssets.getForm().cancel();
            }
        });

        ModelNodeFormBuilder.FormAssets formSsoAssets = new ModelNodeFormBuilder()
                .createValidators(true)
                .exclude(CREDENTIAL_REFERENCE)
                .setResourceDescription(ssoDescription)
                .setSecurityContext(securityContext)
                .createValidators(true)
                .build();

        formSsoAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                String name = selectionModel.getSelectedObject().getName();
                presenter.onSaveResource(APPLICATION_SECURITY_DOMAIN_SSO, name, changeset);
            }

            @Override
            public void onCancel(final Object entity) {
                formAttributesAssets.getForm().cancel();
            }
        });

        ModelNodeFormBuilder.FormAssets credentialReferenceFormAsset = new ComplexAttributeForm(CREDENTIAL_REFERENCE, securityContext, ssoDescription).build();
        credentialReferenceFormAsset.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(final Map changeset) {
                presenter.onSaveComplexAttribute(APPLICATION_SECURITY_DOMAIN_SSO,
                        selectionModel.getSelectedObject().getName(), CREDENTIAL_REFERENCE,
                        credentialReferenceFormAsset.getForm().getUpdatedEntity());
            }

            @Override
            public void onCancel(final Object entity) {
                credentialReferenceFormAsset.getForm().cancel();
            }
        });
        credentialReferenceFormAsset.getForm().addFormValidator(new CredentialReferenceFormValidation());

        // this button is only displayed when the setting=single-sign-on resource doesn't exist
        ToolButton btnAddSso = new ToolButton("Enable Single Sign On", clickEvent -> onAddSingleSignOn(ssoDescription));
        ToolButton btnRemoveSso = new ToolButton("Disable Single Sign On", clickEvent -> onRemoveSingleSignOn());
        btnRemoveSso.getElement().getStyle().setMarginBottom(20, Style.Unit.PX);

        VerticalPanel formPanel = new VerticalPanel();
        formPanel.setStyleName("fill-layout-width");

        Widget formSsoWidget = formSsoAssets.getForm().asWidget();

        selectionModel.addSelectionChangeHandler(event -> {
            Property selectedProperty = selectionModel.getSelectedObject();
            if (selectedProperty != null) {
                ModelNode appNode = selectedProperty.getValue();
                formAttributesAssets.getForm().edit(appNode);
                // if the resource setting=single-sign-on doesn't exist, show a button to add it
                // otherwise shows the sso form
                if (appNode.hasDefined("setting")) {
                    ModelNode ssoNode = appNode.get("setting").get("single-sign-on");
                    formSsoAssets.getForm().edit(ssoNode);
                    credentialReferenceFormAsset.getForm().edit(ssoNode.get(CREDENTIAL_REFERENCE));
                    formPanel.add(btnRemoveSso);
                    formPanel.add(formSsoWidget);
                    formPanel.remove(btnAddSso);
                } else {
                    formPanel.remove(formSsoWidget);
                    formPanel.remove(btnRemoveSso);
                    formPanel.add(btnAddSso);
                    btnAddSso.setEnabled(true);
                    credentialReferenceFormAsset.getForm().clearValues();
                    formSsoAssets.getForm().clearValues();
                }
            } else {
                formAttributesAssets.getForm().clearValues();
                formSsoAssets.getForm().clearValues();
                credentialReferenceFormAsset.getForm().clearValues();
                // if there are no resources (application-security-domain), there is no point displaying sso form,
                // sso enable button
                formPanel.remove(formSsoWidget);
                formPanel.remove(btnRemoveSso);
                formPanel.add(btnAddSso);
                btnAddSso.setEnabled(false);
            }
        });
        table.setSelectionModel(selectionModel);


        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline(title)
                .setDescription(SafeHtmlUtils.fromString(resourceDescription.get(DESCRIPTION).asString()))
                .setMasterTools(tools)
                .setMaster(Console.MESSAGES.available(title), table)
                .addDetail(Console.CONSTANTS.common_label_attributes(), formAttributesAssets.asWidget())
                .addDetail("Single Sign On", formPanel)
                .addDetail("SSO - Credential Reference", credentialReferenceFormAsset.asWidget());

        // creates the FormAsset used in the Add modal dialog for Single Sign-on form, to reuse it.
        ssoAddFormAssets = new ModelNodeFormBuilder()
                .setResourceDescription(ssoDescription)
                .setCreateMode(true)
                .unsorted()
                .exclude(CREDENTIAL_REFERENCE)
                .setCreateNameAttribute(false)
                .setSecurityContext(securityContext)
                .createValidators(true)
                .requiresAtLeastOne("credential-reference-store", "credential-reference-clear-text")
                .build();
        ssoAddFormAssets.getForm().setEnabled(true);
        ssoAddFormAssets.getForm().addFormValidator(new CredentialReferenceFormValidation(true));

        return layoutBuilder.build();
    }

    private void onAdd(ResourceDescription resourceDescription) {

        DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle(Console.CONSTANTS.undertowApplicationSecurityDomain()));
        AddResourceDialog.Callback callback = new AddResourceDialog.Callback() {
            @Override
            public void onAdd(ModelNode payload) {
                dialog.hide();
                presenter.onCreateResource(APPLICATION_SECURITY_DOMAIN, payload);
            }

            @Override
            public void onCancel() {
                dialog.hide();
            }
        };
        AddResourceDialog addDialog = new AddResourceDialog(securityContext, resourceDescription, callback);

        Widget addDialogWidget = addDialog.asWidget();
        dialog.setWidth(480);
        dialog.setHeight(360);
        dialog.setWidget(addDialogWidget);
        dialog.setGlassEnabled(true);
        dialog.center();
    }

    private void onAddSingleSignOn(ResourceDescription ssoDescription) {

        DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle("Single Sign On"));
        AddResourceDialog.Callback callback = new AddResourceDialog.Callback() {
            @Override
            public void onAdd(ModelNode payload) {
                dialog.hide();

                // as the credential-reference attribute is repackaged in the ADD form dialog, it should be
                // correctly assembled as a complex attribute to post it to the server
                payload.get(CREDENTIAL_REFERENCE).setEmptyObject();
                populateRepackagedToCredentialReference(payload, "credential-reference-store", "store");
                populateRepackagedToCredentialReference(payload, "credential-reference-alias", "alias");
                populateRepackagedToCredentialReference(payload, "credential-reference-type", "type");
                populateRepackagedToCredentialReference(payload, "credential-reference-clear-text", "clear-text");

                ssoAddFormAssets.getForm().clearValues();
                presenter.onAddSingleSignOn(selectionModel.getSelectedObject().getName(), payload);
            }

            @Override
            public void onCancel() {
                ssoAddFormAssets.getForm().clearValues();
                dialog.hide();
            }
        };
        AddResourceDialog addDialog = new AddResourceDialog(ssoAddFormAssets, ssoDescription, callback);

        Widget addDialogWidget = addDialog.asWidget();
        dialog.setWidth(480);
        dialog.setHeight(360);
        dialog.setWidget(addDialogWidget);
        dialog.setGlassEnabled(true);
        dialog.center();
    }

    private void onRemoveSingleSignOn() {
        String name = selectionModel.getSelectedObject().getName();
        Feedback.confirm(Console.MESSAGES.deleteTitle("setting"),
                Console.MESSAGES.disableSSOOnSecurityDomainConfirm(name),
                isConfirmed -> {
                    if (isConfirmed) {
                        presenter.onRemoveSingleSignOn(name);
                    }
                });
    }

    public void update(final List<Property> models) {
        dataProvider.setList(models);
        table.selectDefaultEntity();
        if (models.isEmpty()) {
            formAttributesAssets.getForm().clearValues();
            selectionModel.clear();
        }
        SelectionChangeEvent.fire(selectionModel);
    }

    // create a flat node, copying the credential-reference complex attribute as regular attributes of the payload
    private void populateRepackagedToCredentialReference(ModelNode payload, String repackagedPropName, String propertyName) {
        ModelNode value = payload.get(repackagedPropName);
        if (payload.hasDefined(repackagedPropName) && value.asString().trim().length() > 0) {
            payload.get(CREDENTIAL_REFERENCE).get(propertyName).set(value);
        }
        payload.remove(repackagedPropName);
    }

}
