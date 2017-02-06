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

import static org.jboss.dmr.client.ModelDescriptionConstants.ADD;
import static org.jboss.dmr.client.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.dmr.client.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.dmr.client.ModelDescriptionConstants.VALUE_TYPE;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class GenericStoreView extends ElytronGenericResourceView {

    public static final String COMPLEX_ATTRIBUTE = "credential-reference";
    private ModelNodeFormBuilder.FormAssets credentialReferenceFormAsset;

    public GenericStoreView(final Dispatcher circuit,
            final ResourceDescription resourceDescription,
            final SecurityContext securityContext, final String title,
            final AddressTemplate addressTemplate) {
        super(circuit, resourceDescription, securityContext, title, addressTemplate);

        // repackage credential-reference inner attributes to show up in the ADD modal dialog
        ModelNode reqPropsDescription = resourceDescription.get(OPERATIONS).get(ADD).get(REQUEST_PROPERTIES);
        ModelNode credRefDescription = reqPropsDescription.get(COMPLEX_ATTRIBUTE).get(VALUE_TYPE);
        reqPropsDescription.get("credential-reference-store").set(credRefDescription.get("store")).get("nillable")
                .set(false);
        reqPropsDescription.get("credential-reference-alias").set(credRefDescription.get("alias")).get("nillable")
                .set(false);
        reqPropsDescription.get("credential-reference-type").set(credRefDescription.get("type")).get("nillable")
                .set(false);
        reqPropsDescription.get("credential-reference-clear-text").set(credRefDescription.get("clear-text"))
                .get("nillable").set(false);

        excludesFormAttributes(COMPLEX_ATTRIBUTE);
    }

    @Override
    public Map<String, Widget> additionalTabDetails() {
        Map<String, Widget> additionalWidgets = new HashMap<>();
        credentialReferenceFormAsset = new ComplexAttributeForm(COMPLEX_ATTRIBUTE, securityContext, resourceDescription)
                .build();

        credentialReferenceFormAsset.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                circuit.dispatch(new ModifyComplexAttribute(addressTemplate, COMPLEX_ATTRIBUTE,
                        selectionModel.getSelectedObject().getName(),
                        credentialReferenceFormAsset.getForm().getUpdatedEntity()));
            }

            @Override
            public void onCancel(final Object entity) {
                credentialReferenceFormAsset.getForm().cancel();
            }
        });

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
            credentialReferenceFormAsset.getForm().edit(prop.getValue().get(COMPLEX_ATTRIBUTE));
        } else {
            credentialReferenceFormAsset.getForm().clearValues();
        }
    }

    protected ModelNodeFormBuilder.FormAssets customFormOnAdd() {
        ModelNodeFormBuilder formBuilder = new ModelNodeFormBuilder()
                .setResourceDescription(resourceDescription)
                .setCreateMode(true)
                .unsorted()
                .exclude(COMPLEX_ATTRIBUTE)
                .createValidators(true)
                .mutuallyExclusives("credential-reference-store", "credential-reference-alias",
                        "credential-reference-type", "credential-reference-clear-text")
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
        return formAssets;
    }

    @Override
    protected void onAddCallback(final ModelNode payload) {
        payload.get(COMPLEX_ATTRIBUTE).setEmptyObject();
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
            if (!payload.hasDefined("path") && payload.hasDefined("required") && !payload.get("required").asBoolean()) {
                payload.remove("required");
            }
        }
    }

    private void safeGet(ModelNode payload, String repackagedPropName, String propertyName) {
        ModelNode value = payload.get(repackagedPropName);
        if (payload.hasDefined(repackagedPropName) && value.asString().trim().length() > 0) {
            payload.get(COMPLEX_ATTRIBUTE).get(propertyName).set(value);
        }
        payload.remove(repackagedPropName);
    }

}
