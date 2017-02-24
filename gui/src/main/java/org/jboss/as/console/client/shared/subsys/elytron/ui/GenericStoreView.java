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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.elytron.CredentialReferenceFormValidation;
import org.jboss.as.console.client.shared.subsys.elytron.store.ElytronStore;
import org.jboss.as.console.client.shared.subsys.elytron.store.ModifyComplexAttribute;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ComplexAttributeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class GenericStoreView extends ElytronGenericResourceView {

    private ModelNodeFormBuilder.FormAssets credentialReferenceFormAsset;

    public GenericStoreView(final Dispatcher circuit,
            final ResourceDescription resourceDescription,
            final SecurityContext securityContext, final String title,
            final AddressTemplate addressTemplate) {
        super(circuit, resourceDescription, securityContext, title, addressTemplate);

        // repackage credential-reference inner attributes to show up in the ADD modal dialog
        ModelNode reqPropsDescription = resourceDescription.get(OPERATIONS).get(ADD).get(REQUEST_PROPERTIES);
        ModelNode credRefDescription = reqPropsDescription.get(CREDENTIAL_REFERENCE).get(VALUE_TYPE);
        reqPropsDescription.get("credential-reference-store").set(credRefDescription.get("store")).get("nillable")
                .set(true);
        reqPropsDescription.get("credential-reference-alias").set(credRefDescription.get("alias")).get("nillable")
                .set(true);
        reqPropsDescription.get("credential-reference-type").set(credRefDescription.get("type")).get("nillable")
                .set(true);
        reqPropsDescription.get("credential-reference-clear-text").set(credRefDescription.get("clear-text"))
                .get("nillable").set(true);

        excludesFormAttributes(CREDENTIAL_REFERENCE);
    }

    @Override
    public Map<String, Widget> additionalTabDetails() {
        Map<String, Widget> additionalWidgets = new HashMap<>();
        ModelNodeFormBuilder builder = new ComplexAttributeForm(CREDENTIAL_REFERENCE, securityContext, resourceDescription)
                .builder();

        if (resourceDescription.get(ATTRIBUTES).get(CREDENTIAL_REFERENCE).hasDefined(NILLABLE)) {
            boolean nillable = resourceDescription.get(ATTRIBUTES).get(CREDENTIAL_REFERENCE).get(NILLABLE).asBoolean();
            if (!nillable) {
                builder.createValidators(true);
                builder.requiresAtLeastOne("clear-text", "store");
            }
        }
        credentialReferenceFormAsset = builder.build();

        credentialReferenceFormAsset.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                circuit.dispatch(new ModifyComplexAttribute(addressTemplate, CREDENTIAL_REFERENCE,
                        selectionModel.getSelectedObject().getName(),
                        credentialReferenceFormAsset.getForm().getUpdatedEntity()));
            }

            @Override
            public void onCancel(final Object entity) {
                credentialReferenceFormAsset.getForm().cancel();
            }
        });
        credentialReferenceFormAsset.getForm().addFormValidator(new CredentialReferenceFormValidation());

        additionalWidgets.put("Credential Reference", credentialReferenceFormAsset.asWidget());
        return additionalWidgets;
    }

    @Override
    public void update(final List<Property> models) {
        super.update(models);
        if (models.isEmpty()) {
            credentialReferenceFormAsset.getForm().clearValues();
        }
    }

    @Override
    protected void selectTableItem(final Property prop) {
        if (prop != null) {
            credentialReferenceFormAsset.getForm().edit(prop.getValue().get(CREDENTIAL_REFERENCE));
        } else {
            credentialReferenceFormAsset.getForm().clearValues();
        }
    }

    protected ModelNodeFormBuilder.FormAssets customFormOnAdd() {
        ModelNodeFormBuilder formBuilder = new ModelNodeFormBuilder()
                .setResourceDescription(resourceDescription)
                .setCreateMode(true)
                .unsorted()
                .exclude(CREDENTIAL_REFERENCE)
                .createValidators(true)
                .requiresAtLeastOne("credential-reference-store", "credential-reference-clear-text")
                .setSecurityContext(securityContext);

        formBuilder.addFactory("credential-reference-store", attributeDescription -> {
            TextBoxItem textBoxItem = new TextBoxItem("credential-reference-store", "Credential Reference Store",
                    false);
            textBoxItem.setAllowWhiteSpace(true);
            return textBoxItem;
        });
        formBuilder.addFactory("credential-reference-alias", attributeDescription -> {
            TextBoxItem textBoxItem = new TextBoxItem("credential-reference-alias", "Credential Reference Alias",
                    false);
            textBoxItem.setAllowWhiteSpace(true);
            return textBoxItem;
        });
        formBuilder.addFactory("credential-reference-type", attributeDescription -> {
            TextBoxItem textBoxItem = new TextBoxItem("credential-reference-type", "Credential Reference Type", false);
            textBoxItem.setAllowWhiteSpace(true);
            return textBoxItem;
        });
        formBuilder.addFactory("credential-reference-clear-text", attributeDescription -> {
            TextBoxItem textBoxItem = new TextBoxItem("credential-reference-clear-text",
                    "Credential Reference Clear Text", false);
            textBoxItem.setAllowWhiteSpace(true);
            return textBoxItem;
        });

        ModelNodeFormBuilder.FormAssets formAssets = formBuilder.build();
        formAssets.getForm().addFormValidator(new CredentialReferenceFormValidation(true));
        return formAssets;
    }

    @Override
    protected void onAddCallbackBeforeSubmit(final ModelNode payload) {
        payload.get(CREDENTIAL_REFERENCE).setEmptyObject();
        safeGet(payload, "credential-reference-store", "store");
        safeGet(payload, "credential-reference-alias", "alias");
        safeGet(payload, "credential-reference-type", "type");
        safeGet(payload, "credential-reference-clear-text", "clear-text");

        // specific handling for key-store resource
        // the "required" attribute requires the "path" attribute
        // but if the user doesn't set the "required" attribute, it comes as false from the form
        // and as it is filled, the path is not filled, results in error.
        // so, if the "required" attribute is false AND "path" is undefined, remove the "required" from the payload
        if (addressTemplate.getTemplate().equals(ElytronStore.KEY_STORE_ADDRESS.getTemplate())) {
            if (!payload.hasDefined(PATH) && payload.hasDefined(REQUIRED) && !payload.get(REQUIRED).asBoolean()) {
                payload.remove(REQUIRED);
            }
        }
    }

    private void safeGet(ModelNode payload, String repackagedPropName, String propertyName) {
        ModelNode value = payload.get(repackagedPropName);
        if (payload.hasDefined(repackagedPropName) && value.asString().trim().length() > 0) {
            payload.get(CREDENTIAL_REFERENCE).get(propertyName).set(value);
        }
        payload.remove(repackagedPropName);
    }

}
