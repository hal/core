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
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.forms.FormItem;
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
public class LdapRealmView {

    private class ComplexAttributeToolsCallback {

        private String complexAttributeName;
        private ModelNodeForm form;
        FormCallback complexAttributeToolsCallback = new FormCallback() {

            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                circuit.dispatch(new ModifyComplexAttribute(ElytronStore.LDAP_REALM_ADDRESS, complexAttributeName,
                        selectionModel.getSelectedObject().getName(), form.getUpdatedEntity()));
            }

            @Override
            public void onCancel(final Object entity) {
                form.cancel();
            }
        };

        ComplexAttributeToolsCallback(final String complexAttributeName,
                final ModelNodeForm form) {
            this.complexAttributeName = complexAttributeName;
            this.form = form;
        }

    }


    private final Dispatcher circuit;
    private final ResourceDescription resourceDescription;
    private final SecurityContext securityContext;
    protected ModelNodeFormBuilder.FormAssets modelForm;
    private DefaultCellTable<Property> table;
    private ListDataProvider<Property> dataProvider;
    private SingleSelectionModel<Property> selectionModel;
    private ModelNodeFormBuilder.FormAssets identityMappingFormAsset;
    private ModelNodeFormBuilder.FormAssets userPasswordMapperFormAsset;
    private ModelNodeFormBuilder.FormAssets otpCredentialMapperFormAsset;
    private IdentityAttributeMappingView identityAttributeMappingView;
    private NewIdentityAttributesView newIdentityAttributesView;

    public LdapRealmView(final Dispatcher circuit,
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
                        Feedback.confirm(Console.MESSAGES.deleteTitle("LDAP Realm"),
                                Console.MESSAGES.deleteConfirm("LDAP Realm" + " '" + name + "'"),
                                isConfirmed -> {
                                    if (isConfirmed) {
                                        circuit.dispatch(
                                                new RemoveResourceGeneric(ElytronStore.LDAP_REALM_ADDRESS, name));
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

        identityMappingFormAsset = new ComplexAttributeForm("identity-mapping", securityContext, resourceDescription)
                .exclude("attribute-mapping", "new-identity-attributes", "otp-credential-mapper",
                        "user-password-mapper", "x509-credential-mapper")
                .build();
        ModelNode nodeIdenMapping = resourceDescription.get(ATTRIBUTES).get("identity-mapping").get(VALUE_TYPE);

        ResourceDescription userPasswordMapperResource = new ResourceDescription(new ModelNode());
        userPasswordMapperResource.get(ATTRIBUTES).set(nodeIdenMapping);

        userPasswordMapperFormAsset = new ComplexAttributeForm("user-password-mapper", securityContext,
                userPasswordMapperResource).build();

        ResourceDescription otpCredentialMapperResource = new ResourceDescription(new ModelNode());
        otpCredentialMapperResource.get(ATTRIBUTES).set(nodeIdenMapping);

        otpCredentialMapperFormAsset = new ComplexAttributeForm("otp-credential-mapper", securityContext,
                otpCredentialMapperResource).build();

        ModelNodeFormBuilder formBuilder = new ModelNodeFormBuilder()
                .setConfigOnly()
                .exclude("identity-mapping")
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext);
        modelForm = formBuilder.build();

        modelForm.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                circuit.dispatch(new ModifyResourceGeneric(ElytronStore.LDAP_REALM_ADDRESS,
                        selectionModel.getSelectedObject().getName(), changeset));
            }

            @Override
            public void onCancel(final Object entity) {
                modelForm.getForm().cancel();
            }
        });

        identityAttributeMappingView = new IdentityAttributeMappingView();
        newIdentityAttributesView = new NewIdentityAttributesView(circuit, resourceDescription, securityContext);

        identityMappingFormAsset.getForm().setToolsCallback(new ComplexAttributeToolsCallback("identity-mapping",
                identityMappingFormAsset.getForm()).complexAttributeToolsCallback);

        userPasswordMapperFormAsset.getForm()
                .setToolsCallback(new ComplexAttributeToolsCallback("identity-mapping.user-password-mapper",
                        userPasswordMapperFormAsset.getForm()).complexAttributeToolsCallback);

        userPasswordMapperFormAsset.getForm().setResetCallback(() -> circuit
                .dispatch(new ModifyComplexAttribute(ElytronStore.LDAP_REALM_ADDRESS,
                        "identity-mapping.user-password-mapper",
                        selectionModel.getSelectedObject().getName(), new ModelNode().setEmptyList())));

        otpCredentialMapperFormAsset.getForm().setToolsCallback(
                new ComplexAttributeToolsCallback("identity-mapping.otp-credential-mapper",
                        otpCredentialMapperFormAsset.getForm()).complexAttributeToolsCallback);

        otpCredentialMapperFormAsset.getForm().setResetCallback(() -> circuit.dispatch(
                new ModifyComplexAttribute(ElytronStore.LDAP_REALM_ADDRESS, "identity-mapping.otp-credential-mapper",
                        selectionModel.getSelectedObject().getName(), new ModelNode().setEmptyList())));

        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline("LDAP Realm")
                .setDescription(SafeHtmlUtils.fromString(resourceDescription.get(DESCRIPTION).asString()))
                .setMasterTools(tools)
                .setMaster(Console.MESSAGES.available("LDAP Realm"), table)
                .addDetail(Console.CONSTANTS.common_label_attributes(), modelForm.asWidget())
                .addDetail("Identity Mapping", identityMappingFormAsset.asWidget())
                .addDetail("Identity Attribute Mapping", identityAttributeMappingView.asWidget())
                .addDetail("New Identity Attributes", newIdentityAttributesView.asWidget())
                .addDetail("User Password Mapper", userPasswordMapperFormAsset.asWidget())
                .addDetail("OTP Credential Mapper", otpCredentialMapperFormAsset.asWidget());

        selectionModel.addSelectionChangeHandler(event -> {
            Property ldapRealmProperty = selectionModel.getSelectedObject();
            if (ldapRealmProperty != null) {
                modelForm.getForm().edit(ldapRealmProperty.getValue());
                ModelNode identityMappingNode = ldapRealmProperty.getValue().get("identity-mapping");
                if (ldapRealmProperty.getValue().hasDefined("identity-mapping")) {
                    identityMappingFormAsset.getForm().edit(identityMappingNode);
                }

                if (identityMappingNode.hasDefined("attribute-mapping")) {
                    identityAttributeMappingView.update(identityMappingNode.get("attribute-mapping").asList());
                } else {
                    identityAttributeMappingView.clearValues();
                }

                newIdentityAttributesView.update(ldapRealmProperty);

                if (identityMappingNode.hasDefined("user-password-mapper")) {
                    userPasswordMapperFormAsset.getForm().edit(identityMappingNode.get("user-password-mapper"));
                } else {
                    userPasswordMapperFormAsset.getForm().editTransient(new ModelNode());
                }

                if (identityMappingNode.hasDefined("otp-credential-mapper")) {
                    otpCredentialMapperFormAsset.getForm().edit(identityMappingNode.get("otp-credential-mapper"));
                } else {
                    otpCredentialMapperFormAsset.getForm().editTransient(new ModelNode());
                }
            } else {
                modelForm.getForm().clearValues();
                identityMappingFormAsset.getForm().clearValues();
                identityAttributeMappingView.clearValues();
                newIdentityAttributesView.clearValues();
                userPasswordMapperFormAsset.getForm().clearValues();
                otpCredentialMapperFormAsset.getForm().clearValues();
            }
        });
        table.setSelectionModel(selectionModel);

        return layoutBuilder.build();
    }

    private void onAdd() {

        // manipulate the descriptions to allow the add UI operation be able to create the identity-mapping attribute
        ModelNode addDescription = resourceDescription.get(OPERATIONS).get(ADD).get(REQUEST_PROPERTIES);

        addDescription.get("identity-mapping-rdn-identifier")
                .set(addDescription.get("identity-mapping").get(VALUE_TYPE).get("rdn-identifier"));

        DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle("LDAP Realm"));
        AddResourceDialog addDialog = new AddResourceDialog(securityContext, resourceDescription,
                new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        // The name must be part of Property, not in the payload
                        String name = payload.remove(NAME).asString();

                        // re-construct the payload and add the rdn-identifier as child of identity-mapping
                        String rdnIdentifier = payload.remove("identity-mapping-rdn-identifier").asString();
                        ModelNode modelNode = payload.get("identity-mapping").setEmptyObject();
                        modelNode.get("rdn-identifier").set(rdnIdentifier);
                        circuit.dispatch(
                                new AddResourceGeneric(ElytronStore.LDAP_REALM_ADDRESS, new Property(name, payload)));
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
            modelForm.getForm().clearValues();
            selectionModel.clear();
            identityMappingFormAsset.getForm().clearValues();
            identityAttributeMappingView.clearValues();
            newIdentityAttributesView.clearValues();
            userPasswordMapperFormAsset.getForm().clearValues();
            otpCredentialMapperFormAsset.getForm().clearValues();
        }
        SelectionChangeEvent.fire(selectionModel);
    }

    private <T> FormItem<T> findFormItem(List<FormItem> formItems, String name) {
        FormItem selectedFormItem = null;
        for (FormItem formItem : formItems) {
            if (name.equals(formItem.getName())) {
                selectedFormItem = formItem;
                break;
            }
        }
        return selectedFormItem;
    }

}
