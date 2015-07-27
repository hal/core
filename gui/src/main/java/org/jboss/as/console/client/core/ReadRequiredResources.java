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

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.rbac.Constraints;
import org.jboss.as.console.client.rbac.ResourceRef;
import org.jboss.as.console.client.rbac.SecurityContextImpl;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.dmr.ResourceDefinition;
import org.jboss.as.console.mbui.widgets.ResourceDescription;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.*;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * TODO Replace deprecated with new API
 * @author Harald Pehl
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

    public ReadRequiredResources(DispatchAsync dispatcher, StatementContext statementContext) {
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
        this.input = new ArrayList<>();
    }

    public void add(String requiredResource, boolean recursive) {
        ResourceRef ref = new ResourceRef(requiredResource);
        ResourceAddress address = new ResourceAddress(ref.getAddress(), new ModelNode().setEmptyList(), statementContext);
        ResourceDescription description = new ResourceDescription(requiredResource, address);
        ModelNode operation = address.clone();
        operation.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(OPERATIONS).set(true);
        operation.get(ACCESS_CONTROL).set(COMBINED_DESCRIPTIONS);
        if (recursive) {
            operation.get("recursive-depth").set(2); // Workaround: Some browsers choke on two big payload size
        }
        operation.get(INCLUDE_ALIASES).set(true); // TODO Test if this is still necessary once WFLY-2738 is fixed
        // TODO What about notifications?

        input.add(new Input(requiredResource, ref, address, description, operation));
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
        final String requiredResource;
        final ResourceRef ref;
        final ResourceAddress address;
        final ResourceDescription description;
        final ModelNode operation;

        public Input(String requiredResource, ResourceRef ref, ResourceAddress address, ResourceDescription description,
                     ModelNode operation) {
            this.requiredResource = requiredResource;
            this.addressTemplate = AddressTemplate.of(requiredResource);
            this.ref = ref;
            this.address = address;
            this.description = description;
            this.operation = operation;
        }
    }

    private class Parser implements AsyncCallback<DMRResponse> {

        private final Control<RequiredResourcesContext> control;
        private final List<ModelNode> steps;
        private final Map<String, Input> stepToInput;
        private final Set<ResourceRef> references;

        private Parser(Control<RequiredResourcesContext> control, List<ModelNode> steps, Map<String, Input> stepToInput) {
            this.control = control;
            this.steps = steps;
            this.stepToInput = stepToInput;

            this.references = new HashSet<>();
            for (Input in : stepToInput.values()) {
                references.add(in.ref);
            }
        }

        @Override
        public void onFailure(Throwable caught) {
            control.getContext().makeReadonly();
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
                control.getContext().makeReadonly();

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
                        List<ModelNode> inquiryAddress = in.address.get(ADDRESS).asList();

                        // it's a List response when asking for '<resourceType>=*"
                        ModelNode payload = null;
                        ModelNode stepResult = compositeResult.get(step).get(RESULT);
                        if (stepResult.getType() == ModelType.LIST) {
                            List<ModelNode> nodes = stepResult.asList();

                            for (ModelNode node : nodes) {
                                // matching the wildcard response
                                List<ModelNode> responseAddress = node.get(ADDRESS).asList();

                                // match the inquiry
                                if (matchingAddress(responseAddress, inquiryAddress)) {
                                    payload = node.get(RESULT);
                                    break;
                                }
                            }
                            if (payload == null) {
                                Log.error("Failed to process response: "+stepResult.toString());

                                control.getContext().setError(
                                        new RuntimeException("Unexpected response format at address: "+in.requiredResource)
                                );
                                control.abort();
                            }
                        } else {
                            payload = stepResult;
                        }

                        // TODO extract the functions to parse and process the
                        // resource descriptions and the security related metadata

                        // update & store description
                        in.description.setDefinition(new ResourceDefinition(payload));
//                        control.getContext().getResourceDescriptionRegistry().add(in.description);
                        control.getContext().getResourceDescriptionRegistry().add(in.addressTemplate, new org.jboss.as.console.client.v3.dmr.ResourceDescription(payload));

                        // break down into root resource and children
                        parseAccessControlChildren(in.ref, references, control.getContext().getSecurityContextImpl(),
                                payload);
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

        private void parseAccessControlChildren(ResourceRef ref, Set<ResourceRef> references,
                                                SecurityContextImpl context, ModelNode payload) {
            // parse the root resource itself
            parseAccessControlMetaData(ref, context, payload);

            // parse the child resources
            if (payload.hasDefined(CHILDREN)) {
                ModelNode childNodes = payload.get(CHILDREN);
                Set<String> children = childNodes.keys();
                for (String child : children) {
                    ResourceRef childAddress = new ResourceRef(ref.getAddress() + "/" + child + "=*");
                    if (!references.contains(childAddress)) // might already be parsed
                    {
                        ModelNode childModel = childNodes.get(child);
                        if (childModel.hasDefined(MODEL_DESCRIPTION)) // depends on 'recursive' true/false
                        {
                            ModelNode childPayload = childModel.get(MODEL_DESCRIPTION).asPropertyList().get(0).getValue();
                            references.add(childAddress); /// dynamically update the list of required resources
                            parseAccessControlChildren(childAddress, references, context, childPayload);
                        }
                    }
                }
            }
        }

        private void parseAccessControlMetaData(final ResourceRef ref, SecurityContextImpl context, ModelNode payload) {
            ModelNode accessControl = payload.get(ACCESS_CONTROL);
            if (accessControl.isDefined() && accessControl.hasDefined(DEFAULT)) {

                // default policy for requested resource type
                Constraints defaultConstraints = parseConstraints(ref, accessControl.get(DEFAULT));
                if (ref.isOptional()) {
                    context.setOptionalConstraints(ref.getAddress(), defaultConstraints);
                } else {
                    context.setConstraints(ref.getAddress(), defaultConstraints);
                }

                // exceptions (instances) of requested resource type
                if (accessControl.hasDefined(EXCEPTIONS)) {
                    for (Property exception : accessControl.get(EXCEPTIONS).asPropertyList()) {
                        ModelNode addressNode = exception.getValue().get(ADDRESS);
                        String address = normalize(addressNode);
                        if (address != null) {
                            ResourceRef exceptionRef = new ResourceRef(address);
                            Constraints instanceConstraints = parseConstraints(exceptionRef, exception.getValue());
                            context.addChildContext(address, instanceConstraints);
                        } else {
                            Log.error("Skip exception " + exception.getName() + ": No address found in " + exception
                                    .getValue());
                        }
                    }
                }
            }
        }

        private Constraints parseConstraints(final ResourceRef ref, ModelNode policyModel) {
            Constraints constraints = new Constraints(ref.getAddress());

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
                    constraints.setOperationExec(ref.getAddress(), op.getName(), opConstraintModel.get(EXECUTE).asBoolean());
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
