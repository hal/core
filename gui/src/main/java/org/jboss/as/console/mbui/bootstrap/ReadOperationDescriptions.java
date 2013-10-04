/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.console.mbui.bootstrap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.mbui.model.StereoTypes;
import org.jboss.as.console.mbui.model.mapping.AddressMapping;
import org.jboss.as.console.mbui.model.mapping.DMRMapping;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.FilteringStatementContext;
import org.useware.kernel.gui.behaviour.InteractionCoordinator;
import org.useware.kernel.gui.behaviour.StatementContext;
import org.useware.kernel.gui.reification.Context;
import org.useware.kernel.gui.reification.ContextKey;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.behaviour.Resource;
import org.useware.kernel.model.behaviour.ResourceType;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;
import org.useware.kernel.model.structure.Trigger;
import org.useware.kernel.model.structure.builder.InteractionUnitVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;
import static org.useware.kernel.model.mapping.MappingType.DMR;

/**
 *
 * Reads the operation meta data associated to {@link Trigger} units.
 *
 * @see org.jboss.as.console.mbui.behaviour.CommandFactory#createGenericCommand(String, org.jboss.as.console.mbui.behaviour.OperationContext)
 *
 * @author Heiko Braun
 * @date 11/12/2012
 */
public class ReadOperationDescriptions extends ReificationBootstrap
{
    final DispatchAsync dispatcher;
    private static final QName RESOURCE_OP = QName.valueOf("org.jboss.as:resource-operation");

    public ReadOperationDescriptions(final DispatchAsync dispatcher)
    {
        super("read operation description");
        this.dispatcher = dispatcher;
    }

    @Override
    public void prepare(final Dialog dialog, final Context context)
    {
        throw new UnsupportedOperationException("Only async preparation is supported");
    }

    @Override
    public void prepareAsync(final Dialog dialog, final Context context, final Callback callback)
    {
        assert dialog != null && context != null && callback != null : "Interaction unit, context and callback must be present";

        final CollectOperationsVisitor visitor = new CollectOperationsVisitor(context);
        dialog.getInterfaceModel().accept(visitor);

        ModelNode compsite = new ModelNode();
        compsite.get(OP).set(COMPOSITE);
        compsite.get(ADDRESS).setEmptyList();
        compsite.get(STEPS).set(visitor.steps);

        dispatcher.execute(new DMRAction(compsite), new AsyncCallback<DMRResponse>()
        {
            @Override
            public void onFailure(final Throwable caught)
            {
                // In some case the retrieval fails due to lack of permissions to read the meta data (addressable=false)
                callback.onError(new RuntimeException("Failed to read operation descriptions",caught));
            }

            @Override
            public void onSuccess(final DMRResponse result)
            {
                ModelNode response = result.get();

                if(!response.isFailure())
                {
                    // evaluate step responses
                    for (String step : visitor.stepReference.keySet())
                    {
                        ModelNode stepResponse = response.get(RESULT).get(step);

                        if (!context.has(ContextKey.OPERATION_DESCRIPTIONS))
                        {
                            context.set(ContextKey.OPERATION_DESCRIPTIONS, new HashMap<QName, ModelNode>());
                        }

                        Resource<ResourceType> output = visitor.stepReference.get(step);
                        ModelNode operationMetaData = stepResponse.get(RESULT);

                        final QName operationRef = new QName(
                                        output.getSource().getNamespaceURI(),
                                        output.getSource().getLocalPart(),
                                        output.getId().getSuffix()
                        );

                        context.<Map>get(ContextKey.OPERATION_DESCRIPTIONS).put(operationRef, operationMetaData);

                    }
                    callback.onSuccess();
                }
                else
                {
                    callback.onError(new RuntimeException("ReadOperationDescriptions failed: "+response.getFailureDescription()));
                }
            }
        });

    }


    class CollectOperationsVisitor implements InteractionUnitVisitor<StereoTypes>
    {
        final Context context;
        List<ModelNode> steps = new ArrayList<ModelNode>();
        Set<QName> resolvedOperations = new HashSet<QName>();
        Map<String, Resource<ResourceType>> stepReference = new HashMap<String, Resource<ResourceType>>();

        public CollectOperationsVisitor(final Context context)
        {
            this.context = context;
        }

        @Override
        public void startVisit(final Container container)
        {
            // ignore
        }

        @Override
        public void visit(final InteractionUnit<StereoTypes> interactionUnit)
        {
            if(interactionUnit instanceof Trigger
                    && interactionUnit.doesProduce())
                addStep((Trigger) interactionUnit);
        }

        @Override
        public void endVisit(final Container container)
        {
            // noop
        }

        private void addStep(Trigger<StereoTypes> interactionUnit)
        {
            InteractionCoordinator coordinator = context.get(ContextKey.COORDINATOR);

            final StatementContext statementContext = coordinator.getDialogState().getContext(interactionUnit.getId());
            assert statementContext != null : "StatementContext not provided";
            assert interactionUnit.doesProduce();

            Resource<ResourceType> output = interactionUnit.getOutputs().iterator().next();

            // skip unqualified trigger that don't point to a resource operation
            if(!output.getId().equalsIgnoreSuffix(RESOURCE_OP))
                return;

            String operationName = output.getId().getSuffix();
            if(operationName==null)
                throw new IllegalArgumentException("Illegal operation name mapping: "+ output.getId()+ " (suffix required)");

            DMRMapping mapping = interactionUnit.findMapping(DMR);
            String address = mapping.getResolvedAddress();

            final QName operationRef = new QName(                // internal reference. See CommandFactory#createGenericCommand()
                    output.getSource().getNamespaceURI(),
                    output.getSource().getLocalPart(),
                    operationName);

            if (!resolvedOperations.contains(operationRef))
            {
                AddressMapping addressMapping = AddressMapping.fromString(address);
                ModelNode op = addressMapping.asResource(new FilteringStatementContext(
                        statementContext,
                        new FilteringStatementContext.Filter() {
                            @Override
                            public String filter(String key) {
                                if("selected.entity".equals(key))
                                    return "*";
                                else
                                    return null;
                            }

                            @Override
                            public String[] filterTuple(String key) {
                                return null;
                            }
                        }
                ) {

                });
                op.get(OP).set(READ_OPERATION_DESCRIPTION_OPERATION);
                op.get(NAME).set(operationName);

                steps.add(op);

                resolvedOperations.add(operationRef);
                stepReference.put("step-" + steps.size(), output);
            }

        }
    }
}
