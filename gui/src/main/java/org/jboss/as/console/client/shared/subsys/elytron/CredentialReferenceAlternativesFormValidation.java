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
package org.jboss.as.console.client.shared.subsys.elytron;

import java.util.List;

import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.FormValidator;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;

import static org.jboss.dmr.client.ModelDescriptionConstants.DEFAULT;
import static org.jboss.dmr.client.ModelDescriptionConstants.TYPE;

/**
 * Validator for credential-reference attribute alternatives metadata.
 * The credential-reference is an OBJECT type and there are other simple attribute as cluster-password of type STRING
 * Then this validator compares if any attribute (store, alias, type, clear-text) of the credential-reference is set
 * and the simple attribute, they are alternatives and only one of them should be set.
 *
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class CredentialReferenceAlternativesFormValidation implements FormValidator {

    private String attribute;
    private ModelNodeForm form;
    private String panelTitle;
    private boolean credentialReferenceFormAsParameter;

    /**
     *
     *
     *  @param attribute The attribute name of type STRING to compare the credential-reference OBJECT.
     *  @param form The ModelNodeForm form to compare "this" form.
     *  @param panelTitle   The panel title to show in the error validation message.
     *  @param credentialReferenceFormAsParameter  true if the form is the ComplexAttributeForm, otherwise false.
     */
    public CredentialReferenceAlternativesFormValidation(String attribute, ModelNodeForm form, String panelTitle, boolean credentialReferenceFormAsParameter) {
        this.attribute = attribute;
        this.form = form;
        this.panelTitle = panelTitle;
        this.credentialReferenceFormAsParameter = credentialReferenceFormAsParameter;
    }

    @Override
    public void validate(final List<FormItem> formItems, final FormValidation formValidation) {


        if (credentialReferenceFormAsParameter) {

            FormItem sourceItem = findFormItem(formItems, attribute);
            boolean sourceItemDefined = isFormItemDefined(sourceItem);

            for (ModelNode item: form.getUpdatedEntity().asList()) {
                if (sourceItemDefined && item.get(0).isDefined()) {
                    formValidation.addError(attribute);
                    sourceItem.setErrMessage("This field should not be used if the following fields are used " + label(attribute) + " in the panel " + panelTitle);
                    sourceItem.setErroneous(true);
                    break;
                }
            }
        } else {

            ModelNode editedEntity = form.getEditedEntity();
            boolean sourceItemDefined = editedEntity.hasDefined(attribute) && editedEntity.get(attribute).asString().trim().length() > 0;
            if (form.getAttributeMetaData(attribute).has(DEFAULT)) {
                String def = form.getAttributeMetaData(attribute).get(DEFAULT).asString();
                String value = form.getUpdatedEntity().get(attribute).asString().trim();
                sourceItemDefined = sourceItemDefined && !def.equals(value);
            }

            for (FormItem item: formItems) {
                boolean formItemDefined = isFormItemDefined(item);
                if (sourceItemDefined && formItemDefined) {
                    formValidation.addError(attribute);
                    item.setErrMessage("This field should not be used if the following fields are used: " + label(attribute) + " in the panel " + panelTitle);
                    item.setErroneous(true);
                    break;
                }
            }
        }

    }

    private boolean isFormItemDefined(FormItem item) {
        ModelNode sourceMetadata = (ModelNode) item.getMetadata();

        // the boolean type always comes as defined, so we must distinguish the boolean types
        // the string type always comes defined, the string length is evaluated if is set by the user
        boolean sourceBooleanType = ModelType.BOOLEAN.equals(sourceMetadata.get(TYPE).asType());
        boolean defined = (sourceBooleanType && Boolean.parseBoolean(item.getValue().toString()))
                || (!sourceBooleanType && !item.isUndefined()
                    && item.getValue().toString().trim().length() > 0);

        return defined;
    }

    private String label(String attr) {
        char[] attrName = attr.toCharArray();
        attrName[0] = Character.toUpperCase(attrName[0]);
        return new String(attrName).replace("-", " ");

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
