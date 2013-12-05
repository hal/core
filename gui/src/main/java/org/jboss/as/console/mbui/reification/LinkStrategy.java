package org.jboss.as.console.mbui.reification;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import org.jboss.ballroom.client.widgets.InlineLink;
import org.useware.kernel.gui.behaviour.NavigationEvent;
import org.useware.kernel.gui.behaviour.common.CommonQNames;
import org.useware.kernel.gui.reification.Context;
import org.useware.kernel.gui.reification.ContextKey;
import org.useware.kernel.gui.reification.strategy.ReificationStrategy;
import org.useware.kernel.gui.reification.strategy.ReificationWidget;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.Link;
import org.useware.kernel.model.structure.QName;
import org.jboss.as.console.mbui.model.StereoTypes;

/**
 * @author Heiko Braun
 * @date 2/26/13
 */
public class LinkStrategy implements ReificationStrategy<ReificationWidget, StereoTypes> {

    private EventBus eventBus;

    @Override
    public boolean prepare(InteractionUnit<StereoTypes> interactionUnit, Context context) {
        eventBus = context.get(ContextKey.EVENTBUS);
        //assert eventBus!=null : "Event bus is required to execute TriggerStrategy";

        return eventBus!=null;
    }

    @Override
    public ReificationWidget reify(InteractionUnit<StereoTypes> interactionUnit, Context context) {
        LinkAdapter adapter = new LinkAdapter(interactionUnit);
        return adapter;
    }

    @Override
    public boolean appliesTo(InteractionUnit<StereoTypes> interactionUnit) {
        return interactionUnit instanceof Link;
    }

    class LinkAdapter implements ReificationWidget
    {
        private final InteractionUnit unit;
        private final InlineLink widget;

        public LinkAdapter(final InteractionUnit interactionUnit) {
            this.unit = interactionUnit;

            this.widget = new InlineLink(interactionUnit.getLabel());

            this.widget.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {

                    Link link = (Link) interactionUnit;
                    QName target = link.getTarget();

                    NavigationEvent navigationEvent  = new NavigationEvent(
                            CommonQNames.NAVIGATION_ID, target,
                            NavigationEvent.Relation.fromString(link.getId().getLocalPart())  // TODO: local part?
                    );

                    eventBus.fireEventFromSource(
                            navigationEvent,
                            getInteractionUnit().getId()
                    );
                }
            });

            // NOTE: the output is declared within the constructor of a link unit

        }

        @Override
        public InteractionUnit<StereoTypes> getInteractionUnit() {
            return unit;
        }

        @Override
        public void add(ReificationWidget widget) {

           throw new RuntimeException("Should not be called on atomic unit");
        }

        @Override
        public Widget asWidget() {
            return widget;
        }
    }
}
