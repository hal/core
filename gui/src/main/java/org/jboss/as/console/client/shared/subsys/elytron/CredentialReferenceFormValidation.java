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

import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.FormValidator;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;

import static org.jboss.dmr.client.ModelDescriptionConstants.CREDENTIAL_REFERENCE;
import static org.jboss.dmr.client.ModelDescriptionConstants.TYPE;

/**
 * Custom validator for credential-reference attribute, as credential-reference doesn't set the "requires" and
 *   "alternatives" metadata, HAL should validate user input.
 *
 * the write combinations:
 * requires constraint:
 *    alias "requires" [store],
 *    store "requires" [alias]
 *
 * alternatives constraint:
 *    clear-text "alternatives" [store]
 *    store "alternatives" [clear-text]
 *  optional: type
 *
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class CredentialReferenceFormValidation implements FormValidator {

    private static final String STORE = "store";
    private static final String ALIAS = "alias";
    private static final String CLEAR_TEXT = "clear-text";

    // see constructor comment
    private boolean prefixCredentialNames;

    public CredentialReferenceFormValidation() { }

    /**
     *
     * Some credential-reference attribute is required=true, when used in the add modal dialog, the resource description
     *  attributes are rewritten with the credential-reference prefix, see GenericStoreView class
     *
     */
    public CredentialReferenceFormValidation(boolean prefixCredentialNames) {
        this.prefixCredentialNames = prefixCredentialNames;
    }

    @Override
    public void validate(final List<FormItem> formItems, final FormValidation formValidation) {

        String alias = ALIAS;
        String store = STORE;
        String clearText = CLEAR_TEXT;
        if (prefixCredentialNames) {
            alias = CREDENTIAL_REFERENCE + "-" + ALIAS;
            store = CREDENTIAL_REFERENCE + "-" + STORE;
            clearText = CREDENTIAL_REFERENCE + "-" + CLEAR_TEXT;
        }
        FormItem aliasFormItem = findFormItem(formItems, alias);
        FormItem storeFormItem = findFormItem(formItems, store);
        FormItem clearTextFormItem = findFormItem(formItems, clearText);

        boolean aliasDefined = isFormItemDefined(aliasFormItem);
        boolean storeDefined = isFormItemDefined(storeFormItem);
        boolean clearTextDefined = isFormItemDefined(clearTextFormItem);

        // validates the alias and store requires each other
        if (aliasDefined && !storeDefined) {
            formValidation.addError(store);
            storeFormItem.setErrMessage("This is a required attribute if " + label(alias) + " is used.");
            storeFormItem.focus();
            storeFormItem.setErroneous(true);
        }
        // validates the alias and store requires each other
        if (storeDefined && !aliasDefined) {
            formValidation.addError(alias);
            aliasFormItem.setErrMessage("This is a required attribute if " + label(store) + " is used.");
            aliasFormItem.focus();
            aliasFormItem.setErroneous(true);
        }

        // validates the alternatives between clear-text and store
        if (storeDefined && clearTextDefined) {
            formValidation.addError(clearText);
            clearTextFormItem.setErrMessage("This field should not be used if the following fields are used: " + label(store));
            clearTextFormItem.focus();
            clearTextFormItem.setErroneous(true);
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
