/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.console.client.tools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.console.client.widgets.forms.MessageFormat;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.FormValidator;

import com.google.common.collect.Iterables;

public class ModelBrowserValidators {
    private static Map<String, FormValidator> validators = new HashMap<>();

    private static FormItem findItem(String name, List<FormItem> formItems) {
        return Iterables.find(formItems, formItem -> name.equals(formItem.getName()), null);
    }

    private static String removeProfile(String s) {
        if (!s.startsWith("profile=")) {
            return s;
        }

        return s.substring(s.indexOf("/") + 1);
    }

    public static FormValidator getValidatorFor(String resource) {
        resource = removeProfile(resource);

        return validators.get(resource);
    }

    static {
        validators.put("subsystem=naming/binding=*", new FormValidator() {

            @Override
            public void validate(List<FormItem> formItems, FormValidation outcome) {
                FormItem<String> name = findItem("name", formItems);
                FormItem<String> type = findItem("type", formItems);
                FormItem<String> bindingType = findItem("binding-type", formItems);
                FormItem<String> value = findItem("value", formItems);
                FormItem<Boolean> cache = findItem("cache", formItems);
                FormItem<String> module = findItem("module", formItems);
                FormItem<String> classItem = findItem("class", formItems);
                FormItem<String> lookup = findItem("lookup", formItems);

                // org.jboss.as.naming.subsystem.NamingBindingAdd.installRuntimeServices()
                String[] GLOBAL_NAMESPACES = { "java:global", "java:jboss", "java:/" };
                String invalidNamespaceForBinding = "Invalid binding name, name must start with one of " + Arrays.toString(GLOBAL_NAMESPACES);

                if (name != null) {
                    // name is checked only when adding a new binding
                    boolean allowed = false;
                    for (String ns : GLOBAL_NAMESPACES) {
                        if (name.getValue().startsWith(ns)) {
                            allowed = true;
                            break;
                        }
                    }
                    if (!allowed) {
                        name.setErrMessage(invalidNamespaceForBinding);
                        name.setErroneous(true);
                        outcome.addError("name");
                    }
                }

                // org.jboss.as.naming.subsystem.NamingBindingAdd.createSimpleBinding()
                List<String> allowedTypes = Arrays.asList("java.lang.String", "char", "java.lang.Character", "byte",
                        "java.lang.Byte", "short", "java.lang.Short", "int", "java.lang.Integer", "long", "java.lang.Long",
                        "float", "java.lang.Float", "double", "java.lang.Double", "boolean", "java.lang.Boolean",
                        "java.net.URL");
                String unsupportedSimpleBindingType = "Unsupported simple binding type";

                if (!type.isUndefined() && !allowedTypes.contains(type.getValue())) {
                    type.setErrMessage(unsupportedSimpleBindingType);
                    type.setErroneous(true);
                    outcome.addError("type");
                }

                // org.jboss.as.naming.subsystem.NamingBindingResourceDefinition.validateResourceModel()
                MessageFormat bindingTypeRequiresAttributeDefined = new MessageFormat(
                        "Binding type {0} requires attribute named {1} defined");
                MessageFormat cacheNotValidForBindingType = new MessageFormat(
                        "Binding type {0} can not take a 'cache' attribute");

                switch (bindingType.getValue()) {
                    case "simple":
                        if (value.isUndefined()) {
                            value.setErrMessage(bindingTypeRequiresAttributeDefined.format(new String[] { "simple", "value" }));
                            value.setErroneous(true);
                            outcome.addError("value");
                        }
                        if (cache.getValue()) {
                            cache.setErrMessage(cacheNotValidForBindingType.format("simple"));
                            cache.setErroneous(true);
                            outcome.addError("cache");
                        }
                        break;

                    case "object-factory":
                        if (module.isUndefined()) {
                            module.setErrMessage(
                                    bindingTypeRequiresAttributeDefined.format(new String[] { "object-factory", "module" }));
                            module.setErroneous(true);
                            outcome.addError("module");
                        }
                        if (classItem.isUndefined()) {
                            classItem.setErrMessage(
                                    bindingTypeRequiresAttributeDefined.format(new String[] { "object-factory", "class" }));
                            classItem.setErroneous(true);
                            outcome.addError("class");
                        }
                        if (cache.getValue()) {
                            cache.setErrMessage(cacheNotValidForBindingType.format("object-factory"));
                            cache.setErroneous(true);
                            outcome.addError("cache");
                        }
                        break;

                    case "external-context":
                        if (module.isUndefined()) {
                            module.setErrMessage(
                                    bindingTypeRequiresAttributeDefined.format(new String[] { "external-context", "module" }));
                            module.setErroneous(true);
                            outcome.addError("module");
                        }
                        if (classItem.isUndefined()) {
                            classItem.setErrMessage(
                                    bindingTypeRequiresAttributeDefined.format(new String[] { "external-context", "class" }));
                            classItem.setErroneous(true);
                            outcome.addError("class");
                        }
                        break;

                    case "lookup":
                        if (lookup.isUndefined()) {
                            lookup.setErrMessage(
                                    bindingTypeRequiresAttributeDefined.format(new String[] { "lookup", "lookup" }));
                            lookup.setErroneous(true);
                            outcome.addError("lookup");
                        }
                        if (cache.getValue()) {
                            cache.setErrMessage(cacheNotValidForBindingType.format("lookup"));
                            cache.setErroneous(true);
                            outcome.addError("cache");
                        }
                        break;
                }
            }
        });

    }
}
