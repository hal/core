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

import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import org.jboss.as.console.mbui.model.StereoTypes;
import org.useware.kernel.gui.behaviour.SystemEvent;
import org.useware.kernel.gui.behaviour.common.CommonQNames;
import org.useware.kernel.gui.reification.Context;
import org.useware.kernel.gui.reification.ContextKey;
import org.useware.kernel.gui.reification.strategy.ReificationStrategy;
import org.useware.kernel.gui.reification.strategy.ReificationWidget;
import org.useware.kernel.model.behaviour.Resource;
import org.useware.kernel.model.behaviour.ResourceType;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.useware.kernel.model.structure.TemporalOperator.SuspendResume;

/**
 * Strategy for a container with temporal operator == SuspendResume.
 *
 * @author Heiko Braun
 * @date 19/11/2013
 */
public class SuspendStrategy implements ReificationStrategy<ReificationWidget, StereoTypes>
{

    private EventBus eventBus;
    private static final Resource<ResourceType> ACTIVATION = new Resource<ResourceType>(CommonQNames.ACTIVATION_ID, ResourceType.System);
    private int numChildContainers = 0;

    @Override
    public boolean prepare(InteractionUnit<StereoTypes> interactionUnit, Context context) {

        eventBus = context.get(ContextKey.EVENTBUS);
        //assert eventBus!=null : "Coordinator bus is required to execute FormStrategy";
        return eventBus!=null;
    }

    @Override
    public ReificationWidget reify(final InteractionUnit<StereoTypes> interactionUnit, final Context context)
    {
        return new MyAdapter(interactionUnit);
    }

    @Override
    public boolean appliesTo(final InteractionUnit<StereoTypes> interactionUnit)
    {
        return (interactionUnit instanceof Container) && (((Container) interactionUnit)
                .getTemporalOperator() == SuspendResume);
    }


    class MyAdapter  implements ReificationWidget
    {
        final InteractionUnit interactionUnit;
        private DeckPanel deckPanel;
        private Map<Integer, QName> index2child = new HashMap<Integer, QName>();

        MyAdapter(final InteractionUnit<StereoTypes> interactionUnit)
        {

            this.interactionUnit = interactionUnit;

            this.deckPanel = new DeckPanel();

            // activation listener
            eventBus.addHandler(SystemEvent.TYPE,
                    new SystemEvent.Handler() {
                        @Override
                        public boolean accepts(SystemEvent event) {

                            QName id = (QName)event.getPayload();
                            if(id!=null)
                            {
                                boolean childTarget = index2child.containsValue(id);
                                boolean relativeNav = id.getSuffix()!=null;

                                return event.getId().equals(CommonQNames.ACTIVATION_ID)
                                        && (childTarget||relativeNav);
                            }
                            else
                            {
                                return false;   // TODO: how can this happen?
                            }
                        }


                        @Override
                        public void onSystemEvent(SystemEvent event) {
                            QName id = (QName)event.getPayload();
                            String suffix = id.getSuffix();

                            if(suffix!=null)
                            {
                                // relative nav
                                if("next".equals(suffix))
                                {
                                    if(deckPanel.getVisibleWidget()==0)
                                        deckPanel.showWidget(1);
                                    //else  TODO
                                    //    throw new IllegalStateException("Illegal call 'next' on visible widget "+deckPanel.getVisibleWidget());
                                }
                                else if("prev".equals(suffix))
                                {
                                    if(deckPanel.getVisibleWidget()==1)
                                        deckPanel.showWidget(0);


                                    //else TODO
                                    //    throw new IllegalStateException("Illegal call 'prev' on visible widget "+deckPanel.getVisibleWidget());
                                }
                            }
                            else
                            {

                                // absolute nav
                                Set<Integer> keys = index2child.keySet();
                                for(Integer key : keys)
                                {
                                    if(index2child.get(key).equals(id))
                                    {
                                        deckPanel.showWidget(key);
                                        break;
                                    }
                                }
                            }

                        }
                    }
            );

            // complement model
            getInteractionUnit().setInputs(ACTIVATION);

        }

        @Override
        public InteractionUnit getInteractionUnit() {
            return interactionUnit;
        }

        @Override
        public void add(final ReificationWidget widget)
        {
            boolean isContainer = widget.getInteractionUnit() instanceof Container;
            if(isContainer) numChildContainers++;

            if(numChildContainers>2)
                throw new IllegalStateException("Operator.SuspendResume only supports a single child container");

            deckPanel.add(widget.asWidget());
            index2child.put(deckPanel.getWidgetCount()-1, widget.getInteractionUnit().getId());
        }

        @Override
        public Widget asWidget()
        {
            //deckPanel.showWidget(0);
            return deckPanel;
        }
    }
}
