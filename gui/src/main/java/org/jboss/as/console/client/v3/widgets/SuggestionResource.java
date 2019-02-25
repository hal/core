/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.v3.widgets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.google.common.collect.Iterables;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.Composite;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.MultipleWordSuggest;
import org.jboss.ballroom.client.widgets.forms.SuggestBoxItem;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import static java.util.Collections.singleton;
import static org.jboss.dmr.client.ModelDescriptionConstants.ADDRESS;
import static org.jboss.dmr.client.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.dmr.client.ModelDescriptionConstants.RESULT;

/**
 * @author Claudio Miranda
 */
public class SuggestionResource {

    private MultipleWordSuggest oracle;
    private SuggestBoxItem suggestBoxItem;

    public SuggestionResource(String name, String label, boolean required, final AddressTemplate template) {
        this(name, label, required, singleton(template));
    }

    public SuggestionResource(String name, String label, boolean required, final Iterable<AddressTemplate> templates) {

        suggestBoxItem = new SuggestBoxItem(name, label, required) {
            @Override
            public void clearValue() {
                super.clearValue();
                // read resources, because as the widget is created before values are updated,
                // so, when the user edits the bean, read target resources to update its list.
                readResources(templates);
            }
        };
        if (Iterables.isEmpty(templates)) {
            Log.info("There are no resource templates to populate as suggestions for attribute: " + suggestBoxItem.getName());
        }
        readResources(templates);

        oracle = new MultipleWordSuggest();
        suggestBoxItem.setOracle(oracle);
    }

    public FormItem buildFormItem() {
        return suggestBoxItem;
    }

    private void readResources(final Iterable<AddressTemplate> templates) {

        // if there is no templates, there are no resources to read from, then just return.
        if (!templates.iterator().hasNext()) {
            return;
        }

        Iterator<AddressTemplate> iter = templates.iterator();
        List<Operation> ops = new ArrayList<>();
        while (iter.hasNext()) {
            AddressTemplate addressTemplate = iter.next();

            // the response must contains the full resource address to display to the user
            // that is why read-resource is used instead of read-children-names(child-type)
            ResourceAddress res = addressTemplate.resolve(Console.MODULES.getCoreGUIContext());
            Operation op = new Operation.Builder(READ_RESOURCE_OPERATION, res)
                .build();

            ops.add(op);
        }

        Composite composite = new Composite(ops);

        Console.MODULES.getDispatchAsync().execute(new DMRAction(composite), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                Console.error(Console.CONSTANTS.common_error_unknownError(), caught.getMessage());
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (result.isFailure()) {
                    Console.error(Console.CONSTANTS.common_error_unknownError(), result.getFailureDescription());
                } else {
                    ModelNode payload = result.get(RESULT);

                    int numberOfSteps = payload.asList().size();

                    List<SuggestOracle.Suggestion> resources = new ArrayList<>();
                    oracle.clear();
                    for (int nr = 1; nr <= numberOfSteps; nr++) {

                        ModelNode step = payload.get("step-" + nr);
                        if (step.hasDefined(RESULT)) {
                            ModelNode resourceResult = step.get(RESULT);

                            for (ModelNode p : resourceResult.asList()) {

                                if (p.hasDefined(ADDRESS)) {
                                    ModelNode address = p.get(ADDRESS);
                                    String formattedName = formatAddressName(address);

                                    // the dropdown should contain the replacement value, instead of display string.
                                    String replacementValue = formattedName.substring(formattedName.lastIndexOf(" = ") + 3);
                                    resources.add(new MultiWordSuggestOracle.MultiWordSuggestion(replacementValue, formattedName));
                                }
                            }
                        }
                    }
                    // the default suggestion list is used when the user doesn't type in the textbox, then all
                    // options are listed.
                    oracle.setDefaultSuggestions(resources);
                    // the list added here, is used when the user types letters in the textbox.
                    oracle.addAll(resources);

                }
            }

            // decompose the address into a formatted string to be displayed
            // in the drop-down list. This is important, so the users knows the full resource address.
            // example: profile = full / subsystem = batch-jberet / thread-pool = batch_pool1
            private String formatAddressName(ModelNode address) {
                StringBuilder buff = new StringBuilder();

                List<ModelNode> addressParts = address.asList();
                for (int i = 0; i < addressParts.size(); i++) {
                    ModelNode addressPart = addressParts.get(i);
                    Property property = addressPart.asProperty();
                    buff.append(property.getName()).append(" = ")
                            .append(property.getValue().asString());
                    if (i + 1 < addressParts.size())
                        buff.append(" / ");

                }
                String formattedName = buff.toString();
                return formattedName;
            }
        });
    }
}
