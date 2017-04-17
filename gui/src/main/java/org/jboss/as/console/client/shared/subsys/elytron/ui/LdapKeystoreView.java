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
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.subsys.elytron.store.ElytronStore;
import org.jboss.as.console.client.shared.subsys.elytron.store.ModifyComplexAttribute;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ComplexAttributeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class LdapKeystoreView extends ElytronGenericResourceView {

    public static final String COMPLEX_ATTRIBUTE = "new-item-template";
    public static final String NEW_ITEM_ATTRIBUTES = "new-item-attributes";

    private ModelNodeFormBuilder.FormAssets newItemTemplateFormAsset;

    public LdapKeystoreView(final Dispatcher circuit,
            final ResourceDescription resourceDescription,
            final SecurityContext securityContext, final String title,
            final AddressTemplate addressTemplate) {
        super(circuit, resourceDescription, securityContext, title, addressTemplate);
        excludesFormAttributes(COMPLEX_ATTRIBUTE);

        // complement the help text of "new-item-attributes" to inform the input form of key=value1,value2
        // as the input form is tweaked about this complex attribute
        ModelNode newItemAttributesNode = resourceDescription.get(ATTRIBUTES).get(COMPLEX_ATTRIBUTE).get(VALUE_TYPE)
                .get(NEW_ITEM_ATTRIBUTES);
        String description = newItemAttributesNode.get(DESCRIPTION).asString() + " " + Console.CONSTANTS
                .subsys_elytron_ldap_keystore_newattribute_desc();
        newItemAttributesNode.get(DESCRIPTION).set(description);
    }

    @Override
    public Map<String, Widget> additionalTabDetails() {
        Map<String, Widget> additionalWidgets = new HashMap<>();
        ModelNodeFormBuilder builder = new ComplexAttributeForm(COMPLEX_ATTRIBUTE, securityContext, resourceDescription)
                .builder();
        builder.createValidators(true);

        newItemTemplateFormAsset = builder.build();

        newItemTemplateFormAsset.getForm().addFormValidator((formItems, formValidation) -> {
            FormItem newItemAttributesItem = findFormItem(formItems, NEW_ITEM_ATTRIBUTES);
            List<String> newItemAttributes = (List<String>) newItemAttributesItem.getValue();
            for (String temp : newItemAttributes) {
                if (temp.indexOf('=') < 1) {
                    formValidation.addError(NEW_ITEM_ATTRIBUTES);
                    newItemAttributesItem.setErrMessage("Each line must contain a key=value1,value2 pair.");
                    newItemAttributesItem.setErroneous(true);
                    break;
                }
            }
        });

        newItemTemplateFormAsset.getForm().setResetCallback(() -> circuit
                .dispatch(new ModifyComplexAttribute(ElytronStore.LDAP_KEY_STORE_ADDRESS, COMPLEX_ATTRIBUTE,
                        selectionModel.getSelectedObject().getName(), new ModelNode().setEmptyList())));


        newItemTemplateFormAsset.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {


                ModelNode newItemTemplateModel = newItemTemplateFormAsset.getForm().getUpdatedEntity();

                // the "new-item-attributes" type is LIST, but value-type is an OBJECT, and the form field is
                // ListItem, that returns a list of STRING, so we must convert the key=value1,value2 (the value is a list
                // separated by comma) to a list of OBJECT (key=STRING, value=LIST OF STRING)
                // this tweak is necessary as all attributes of this complex attribute sets the "requires" to each other

                if (newItemTemplateModel.hasDefined(NEW_ITEM_ATTRIBUTES)) {
                    List<ModelNode> newItemAttributes = newItemTemplateModel.get(NEW_ITEM_ATTRIBUTES).asList();
                    newItemTemplateModel.get(NEW_ITEM_ATTRIBUTES).setEmptyList();
                    for (ModelNode node : newItemAttributes) {
                        String line = node.asString();
                        String key = line.substring(0, line.indexOf('='));
                        String val = line.substring(line.indexOf('=') + 1);
                        String[] valueSplit = val.split(",");

                        ModelNode keyValPair = new ModelNode();
                        keyValPair.get(NAME).set(key);
                        keyValPair.get(VALUE).setEmptyList();
                        for (String v : valueSplit) {
                            keyValPair.get(VALUE).add(v);
                        }
                        newItemTemplateModel.get(NEW_ITEM_ATTRIBUTES).add(keyValPair);
                    }
                }

                circuit.dispatch(new ModifyComplexAttribute(ElytronStore.LDAP_KEY_STORE_ADDRESS, COMPLEX_ATTRIBUTE,
                        selectionModel.getSelectedObject().getName(), newItemTemplateModel));
            }

            @Override
            public void onCancel(final Object entity) {
                newItemTemplateFormAsset.getForm().cancel();
            }
        });

        additionalWidgets.put("New Item Template", newItemTemplateFormAsset.asWidget());
        return additionalWidgets;
    }

    @Override
    public void update(final List<Property> models) {
        super.update(models);
        if (models.isEmpty()) {
            newItemTemplateFormAsset.getForm().clearValues();
        }
    }

    @Override
    protected void selectTableItem(final Property prop) {
        if (prop != null) {

            // as the ModelNode must be modified, it is cloned.
            ModelNode bean = prop.getValue().get(COMPLEX_ATTRIBUTE).clone();
            if (bean.hasDefined(NEW_ITEM_ATTRIBUTES)) {
                List<ModelNode> attrs = bean.get(NEW_ITEM_ATTRIBUTES).asList();
                bean.get(NEW_ITEM_ATTRIBUTES).setEmptyList();
                for (ModelNode n : attrs) {
                    String key = n.get(NAME).asString();
                    List<ModelNode> vals = n.get(VALUE).asList();
                    String val = "";
                    for (int i = 0; i < vals.size(); i++) {
                        val += vals.get(i).asString();
                        if (i + 1 < vals.size()) {
                            val += ",";
                        }
                    }
                    String line = key + "=" + val;
                    bean.get(NEW_ITEM_ATTRIBUTES).add(line);
                }
            }

            newItemTemplateFormAsset.getForm().edit(bean);
        } else {
            newItemTemplateFormAsset.getForm().clearValues();
        }
    }

}
