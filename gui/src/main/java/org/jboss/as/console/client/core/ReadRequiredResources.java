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
package org.jboss.as.console.client.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.rbac.Constraints;
import org.jboss.as.console.client.shared.Preferences;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.useware.kernel.gui.behaviour.StatementContext;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 * @author Heiko Braun
 */
public class ReadRequiredResources implements Function<RequiredResourcesContext> {

    private static final String READ = "read";
    private static final String WRITE = "write";
    private static final String EXECUTE = "execute";
    private static final String EXCEPTIONS = "exceptions";
    private static final String ACCESS_CONTROL = "access-control";
    private static final String COMBINED_DESCRIPTIONS = "combined-descriptions";

    private final DispatchAsync dispatcher;
    private final StatementContext statementContext;
    private final List<Input> input;
    private String locale = Preferences.get(Preferences.Key.LOCALE, "en");

    public ReadRequiredResources(DispatchAsync dispatcher, StatementContext statementContext) {
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
        this.input = new ArrayList<>();
    }

    public void add(AddressTemplate addressTemplate, boolean recursive) {


        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(addressTemplate.resolve(statementContext));
        operation.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(OPERATIONS).set(true);
        operation.get(ACCESS_CONTROL).set(COMBINED_DESCRIPTIONS);
        operation.get(LOCALE).set(locale);
        if (recursive) {
            operation.get("recursive-depth").set(2); // Workaround: Some browsers choke on two big payload size
        }
        operation.get(INCLUDE_ALIASES).set(true); // TODO Test if this is still necessary once WFLY-2738 is fixed
        // TODO What about notifications?

        input.add(new Input(addressTemplate, operation));
    }

    @Override
    public void execute(Control<RequiredResourcesContext> control) {
        ModelNode comp = new ModelNode();
        comp.get(OP).set(COMPOSITE);
        comp.get(ADDRESS).setEmptyList();

        int index = 1;
        List<ModelNode> steps = new ArrayList<>();
        Map<String, Input> stepToInput = new HashMap<>();
        for (Iterator<Input> iterator = input.iterator(); iterator.hasNext(); index++) {
            Input in = iterator.next();
            steps.add(in.operation);
            stepToInput.put("step-" + index, in);
        }
        comp.get(STEPS).set(steps);
        dispatcher.execute(new DMRAction(comp), new Parser(control, steps, stepToInput));
    }


    // ------------------------------------------------------ inner classes

    private static class Input {
        final AddressTemplate addressTemplate;
        final ModelNode operation;

        public Input(AddressTemplate ref, ModelNode operation) {

            this.addressTemplate = ref;
            this.operation = operation;
        }
    }

    private class Parser implements AsyncCallback<DMRResponse> {

        private final Control<RequiredResourcesContext> control;
        private final List<ModelNode> steps;
        private final Map<String, Input> stepToInput;
        private final Set<AddressTemplate> references;

        private Parser(Control<RequiredResourcesContext> control, List<ModelNode> steps, Map<String, Input> stepToInput) {
            this.control = control;
            this.steps = steps;
            this.stepToInput = stepToInput;

            this.references = new HashSet<>();
            for (Input in : stepToInput.values()) {
                references.add(in.addressTemplate);
            }
        }

        @Override
        public void onFailure(Throwable caught) {
            control.getContext().setError(caught);
            caught.printStackTrace();
            control.abort();
        }

        @Override
        public void onSuccess(DMRResponse dmrResponse) {
            ModelNode response = dmrResponse.get();

            if (response.isFailure()) {
                Log.error("Failed to retrieve access control meta data, fallback to temporary read-only context: ",
                        response.getFailureDescription());

            } else {
                ModelNode compositeResult = response.get(RESULT);
                for (int i = 1; i <= steps.size(); i++) {
                    String step = "step-" + i;
                    if (compositeResult.hasDefined(step)) {
                        // The first part is identify the resource that has been requested.
                        // Depending on whether you've requested a wildcard address or a
                        // specific one we either get a ModelType.List or ModelType.Object
                        // response. The former requires parsing the response to access control
                        // meta data matching the inquiry.
                        Input in = stepToInput.get(step);
                        // TODO: why is this mapped to the input and not resolved from the template?

                        List<ModelNode> inquiryAddress = in.operation.hasDefined(ADDRESS) ? in.operation.get(ADDRESS).asList() : Collections.EMPTY_LIST;

                        // it's a List response when asking for '<resourceType>=*"
                        ModelNode payload = null;
                        ModelNode stepResult = compositeResult.get(step).get(RESULT);
                        if (stepResult.getType() == ModelType.LIST) {
                            List<ModelNode> nodes = stepResult.asList();

                            for (ModelNode node : nodes) {
                                // matching the wildcard response
                                String storage = null;
                                if (node.get(RESULT).hasDefined(STORAGE))
                                    storage = node.get(RESULT).get(STORAGE).asString();
                                
                                List<ModelNode> responseAddress = node.get(ADDRESS).asList();

                                // match the inquiry
                                // the runtime-only=storage check is performed only when the matchingAddress returns false
                                // this cover the corner case of r-r-d for /subsystem=datasources/data-source=*/statistics=pool
                                // because the fix of WFCORE-1737 resolves the datasource=* wildcard
                                if (matchingAddress(responseAddress, inquiryAddress) || RUNTIME_ONLY.equals(storage)) {
                                    payload = node.get(RESULT);
                                    break;
                                } else {
                                    // log error as they don't match
                                    Log.error("Addresses don't match. response address:  " + node.get(ADDRESS).asString() + ". inquiry address: " + in.operation.get(ADDRESS).asString());
                                }
                            }
                            if (payload == null) {

                                control.getContext().setError(
                                        new RuntimeException("Unexpected response format at address: "+in.addressTemplate.toString())
                                );
                                control.abort();
                            }
                        } else {
                            payload = stepResult;
                        }

                        // add the description to the registry
                       /* ResourceDescriptionRegistry resourceDescriptionRegistry = control.getContext().getResourceDescriptionRegistry();
                        resourceDescriptionRegistry.add(
                                in.addressTemplate,
                                new org.jboss.as.console.client.v3.dmr.ResourceDescription(payload)
                        );*/

                        control.getContext().addDescriptionResult(
                                in.addressTemplate,
                                new org.jboss.as.console.client.v3.dmr.ResourceDescription(payload)
                        );

                        // extract and register the access control meta data
                        parseAccessControlChildren(
                                in.addressTemplate,
                                control.getContext(),
                                references,
                                payload
                        );
                    }
                }
            }

            control.proceed();
        }

        private boolean matchingAddress(List<ModelNode> responseAddress, List<ModelNode> inquiryAddress) {
            int numMatchingTokens = 0;
            int offset = inquiryAddress.size() - responseAddress.size();

            for (int i = responseAddress.size() - 1; i >= 0; i--) {
                ModelNode token = responseAddress.get(i);
                if (inquiryAddress.get(i + offset).toString().equals(token.toString()))
                    numMatchingTokens++;
            }
            return numMatchingTokens == responseAddress.size();
        }

        private void parseAccessControlChildren(AddressTemplate ref, RequiredResourcesContext context, Set<AddressTemplate> references, ModelNode payload) {
            // parse the root resource itself
            parseAccessControlMetaData(ref, context, payload);

            // parse the child resources
            if (payload.hasDefined(CHILDREN)) {
                ModelNode childNodes = payload.get(CHILDREN);
                Set<String> children = childNodes.keys();
                for (String child : children) {
                    AddressTemplate childAddress = ref.append("/" + child + "=*");
                    if (!references.contains(childAddress)) // might already be parsed
                    {
                        ModelNode childModel = childNodes.get(child);
                        if (childModel.hasDefined(MODEL_DESCRIPTION)) // depends on 'recursive' true/false
                        {
                            ModelNode childPayload = childModel.get(MODEL_DESCRIPTION).asPropertyList().get(0).getValue();
                            references.add(childAddress); /// dynamically update the list of required resources
                            parseAccessControlChildren(childAddress, context, references, childPayload);
                        }
                    }
                }
            }
        }

        private void parseAccessControlMetaData(final AddressTemplate addressTemplate, RequiredResourcesContext context, ModelNode payload) {
            ModelNode accessControl = payload.get(ACCESS_CONTROL);
            if (accessControl.isDefined() && accessControl.hasDefined(DEFAULT)) {

                // default policy for requested resource type
                Constraints defaultConstraints = parseConstraints(addressTemplate, accessControl.get(DEFAULT));
                context.addConstraintResult(addressTemplate, defaultConstraints);

                // exceptions (instances) of requested resource type
                if (accessControl.hasDefined(EXCEPTIONS)) {
                    for (Property exception : accessControl.get(EXCEPTIONS).asPropertyList()) {
                        ModelNode addressNode = exception.getValue().get(ADDRESS);
                        AddressTemplate exceptionRef = AddressTemplate.of(normalize(addressNode));
                        if (exceptionRef != null) {
                            Constraints instanceConstraints = parseConstraints(exceptionRef, exception.getValue());
                            context.addConstraintResult(addressTemplate, exceptionRef.resolveAsKey(statementContext), instanceConstraints);
                        } else {
                            Log.error("Skip exception " + exception.getName() + ": No address found in " + exception
                                    .getValue());
                        }
                    }
                }
            }
        }

        private Constraints parseConstraints(final AddressTemplate ref, ModelNode policyModel) {
            final Constraints constraints = new Constraints(ref);

            // resource constraints
            if (policyModel.hasDefined(ADDRESS) && !policyModel.get(ADDRESS).asBoolean()) {
                constraints.setAddress(false);
            } else {
                constraints.setReadResource(policyModel.get(READ).asBoolean());
                constraints.setWriteResource(policyModel.get(WRITE).asBoolean());
            }

            // operation constraints
            if (policyModel.hasDefined(OPERATIONS)) {
                List<Property> operations = policyModel.get(OPERATIONS).asPropertyList();
                for (Property op : operations) {
                    ModelNode opConstraintModel = op.getValue();
                    constraints.setOperationExec(op.getName(), opConstraintModel.get(EXECUTE).asBoolean());
                }
            }

            // attribute constraints
            if (policyModel.hasDefined(ATTRIBUTES)) {
                List<Property> attributes = policyModel.get(ATTRIBUTES).asPropertyList();

                for (Property att : attributes) {
                    ModelNode attConstraintModel = att.getValue();
                    constraints.setAttributeRead(att.getName(), attConstraintModel.get(READ).asBoolean());
                    constraints.setAttributeWrite(att.getName(), attConstraintModel.get(WRITE).asBoolean());
                }
            }
            return constraints;
        }


        private String normalize(final ModelNode address) {
            if (address.isDefined()) {
                StringBuilder normalized = new StringBuilder();
                List<Property> properties = address.asPropertyList();
                for (Property property : properties) {
                    normalized.append("/").append(property.getName()).append("=").append(property.getValue().asString());
                }
                return normalized.toString();
            }
            return null;
        }
    }
}
