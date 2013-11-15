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
package org.jboss.as.console.mbui.reification;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import org.jboss.as.console.client.shared.properties.ModelNodePropertyEditor;
import org.jboss.as.console.client.shared.properties.ModelNodePropertyManagement;
import org.jboss.as.console.mbui.JBossQNames;
import org.jboss.as.console.mbui.model.StereoTypes;
import org.jboss.as.console.mbui.model.mapping.DMRMapping;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.useware.kernel.gui.behaviour.InteractionEvent;
import org.useware.kernel.gui.behaviour.PresentationEvent;
import org.useware.kernel.gui.behaviour.SystemEvent;
import org.useware.kernel.gui.behaviour.common.CommonQNames;
import org.useware.kernel.gui.reification.Context;
import org.useware.kernel.gui.reification.ContextKey;
import org.useware.kernel.gui.reification.strategy.ReificationStrategy;
import org.useware.kernel.gui.reification.strategy.ReificationWidget;
import org.useware.kernel.model.behaviour.Resource;
import org.useware.kernel.model.behaviour.ResourceType;
import org.useware.kernel.model.mapping.MappingType;
import org.useware.kernel.model.mapping.Predicate;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;

import java.util.List;
import java.util.Map;

import static org.useware.kernel.gui.behaviour.common.CommonQNames.RESET_ID;
import static org.useware.kernel.model.behaviour.ResourceType.*;
import static org.useware.kernel.model.behaviour.ResourceType.System;

/**
 * @author Harald Pehl
 * @author Heiko Braun
 * @date 11/01/2012
 */
public class PropertiesStrategy implements ReificationStrategy<ReificationWidget, StereoTypes>
{

    private ModelNode modelDescription;
    private EventBus eventBus;
    private static final Resource<ResourceType> SAVE_EVENT = new Resource<ResourceType>(JBossQNames.SAVE_ID, Interaction);
    private static final Resource<ResourceType> LOAD_EVENT = new Resource<ResourceType>(JBossQNames.LOAD_ID, Interaction);
    private static final Resource<ResourceType> RESET = new Resource<ResourceType>(RESET_ID, System);
    private SecurityContext securityContext;

    @Override
    public boolean prepare(InteractionUnit<StereoTypes> interactionUnit, Context context) {
        Map<QName, ModelNode> descriptions = context.get (ContextKey.MODEL_DESCRIPTIONS);

        // TODO (BUG): After the first reification the behaviour is modified,
        // so the predicate might apply to a different unit. As a result the correlation id is different!

        QName correlationId = interactionUnit.findMapping(MappingType.DMR, new Predicate<DMRMapping>() {
            @Override
            public boolean appliesTo(DMRMapping candidate) {
                return candidate.getAddress()!=null;
            }
        }).getCorrelationId();
        modelDescription = descriptions.get(correlationId);

        eventBus = context.get(ContextKey.EVENTBUS);
        securityContext = context.get(ContextKey.SECURITY_CONTEXT);

        return eventBus!=null
                && modelDescription!=null
                && securityContext!=null;
    }

    @Override
    public ReificationWidget reify(final InteractionUnit<StereoTypes> interactionUnit, final Context context)
    {
        return new PropertyEditorAdapter(interactionUnit, eventBus, modelDescription);
    }

    @Override
    public boolean appliesTo(final InteractionUnit<StereoTypes> interactionUnit)
    {
        return StereoTypes.Properties == interactionUnit.getStereotype();
    }

    class PropertyEditorAdapter implements ReificationWidget, ModelNodePropertyManagement
    {

        final InteractionUnit interactionUnit;
        private final ModelNodePropertyEditor editor;
        private String objectName = null;
        private SafeHtmlBuilder helpTexts;
        private EventBus coordinator;

        PropertyEditorAdapter(final InteractionUnit<StereoTypes> interactionUnit, EventBus coordinator, final ModelNode modelDescription)
        {
            this.interactionUnit = interactionUnit;
            this.coordinator = coordinator;

            DMRMapping dmrMapping = (DMRMapping)
                    this.interactionUnit.findMapping(MappingType.DMR);

            ModelNode attDesc = modelDescription.get("attributes").asObject();
            if(dmrMapping.getObjects().size()>0
                    && attDesc .hasDefined(dmrMapping.getObjects().get(0)))
            {
                // mapped to ModelType.Object
                objectName = dmrMapping.getObjects().get(0);
                ModelNode description = attDesc.get(objectName).asObject();
                Log.debug(description.toString());

            }
            else
            {
                Log.warn(interactionUnit.getId() + "is missing valid DMR mapping");
            }

            this.editor = new ModelNodePropertyEditor(PropertyEditorAdapter.this, true);

        }

        @Override
        public void onCreateProperty(String reference, Property prop) {

        }

        @Override
        public void onDeleteProperty(String reference, Property prop) {

        }

        @Override
        public void onChangeProperty(String reference, Property prop) {

        }

        @Override
        public void launchNewPropertyDialoge(String reference) {

        }

        @Override
        public void closePropertyDialoge() {

        }

        @Override
        public InteractionUnit getInteractionUnit() {
            return interactionUnit;
        }

        @Override
        public void add(final ReificationWidget widget)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Widget asWidget()
        {

            // handle resets within this scope
            coordinator.addHandler(SystemEvent.TYPE, new SystemEvent.Handler() {
                @Override
                public boolean accepts(SystemEvent event) {
                    return event.getId().equals(CommonQNames.RESET_ID) ;
                }

                @Override
                public void onSystemEvent(SystemEvent event) {


                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        @Override
                        public void execute() {
                            // request loading of data
                            InteractionEvent loadEvent = new InteractionEvent(JBossQNames.LOAD_ID);

                            // update interaction units
                            coordinator.fireEventFromSource(
                                    loadEvent,
                                    interactionUnit.getId()
                            );
                        }
                    });
                }
            });


            // handle the results of function calls
            coordinator.addHandler(PresentationEvent.TYPE, new PresentationEvent.PresentationHandler()
            {
                @Override
                public boolean accepts(PresentationEvent event) {
                    boolean matchingId = event.getTarget().equals(getInteractionUnit().getId());

                    // only single resources accepted (might be collection, see LoadResourceProcedure)
                    boolean payloadMatches = event.getPayload() instanceof ModelNode;

                    return matchingId && payloadMatches;
                }

                @Override
                public void onPresentationEvent(PresentationEvent event) {

                    assert (event.getPayload() instanceof ModelNode) : "Unexpected type "+event.getPayload().getClass();

                    ModelNode modelNode = (ModelNode)event.getPayload();
                    if(modelNode.hasDefined(objectName))
                    {
                        List<Property> properties = modelNode.get(objectName).asPropertyList();
                        editor.setProperties("self", properties);
                    }
                }
            });


            // Register inputs and outputs

            Resource<ResourceType> update = new Resource<ResourceType>(getInteractionUnit().getId(), Presentation);

            getInteractionUnit().setOutputs(SAVE_EVENT, LOAD_EVENT);
            getInteractionUnit().setInputs(RESET, update);

            return editor.asWidget();
        }
    }
}
