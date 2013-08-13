package org.jboss.as.console.mbui.reification;

import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import org.jboss.as.console.mbui.model.mapping.DMRMapping;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.useware.kernel.gui.reification.Context;
import org.useware.kernel.gui.reification.ContextKey;
import org.useware.kernel.gui.reification.strategy.ReificationStrategy;
import org.useware.kernel.gui.reification.strategy.ReificationWidget;
import org.useware.kernel.model.behaviour.Resource;
import org.useware.kernel.model.behaviour.ResourceType;
import org.useware.kernel.model.mapping.MappingType;
import org.useware.kernel.model.structure.InteractionUnit;
import org.jboss.as.console.mbui.model.StereoTypes;

import java.util.Iterator;

/**
 * @author Heiko Braun
 * @date 2/26/13
 */
public class ToolStripStrategy implements ReificationStrategy<ReificationWidget, StereoTypes> {

    private SecurityContext securityContext;
    private EventBus eventBus;

    @Override
    public boolean prepare(InteractionUnit<StereoTypes> interactionUnit, Context context) {
        eventBus = context.get(ContextKey.EVENTBUS);
        securityContext = context.get(ContextKey.SECURITY_CONTEXT);
        return securityContext!=null & eventBus!=null;
    }


    @Override
    public ReificationWidget reify(InteractionUnit<StereoTypes> interactionUnit, Context context) {
        ToolStripAdapter adapter = null;
        if (interactionUnit != null)
        {
            adapter = new ToolStripAdapter(interactionUnit);
        }
        return adapter;
    }

    @Override
    public boolean appliesTo(InteractionUnit<StereoTypes> interactionUnit) {
        return StereoTypes.Toolstrip == interactionUnit.getStereotype();
    }

    class ToolStripAdapter implements ReificationWidget
    {
        private final InteractionUnit unit;
        private final org.jboss.ballroom.client.widgets.tools.ToolStrip tools;

        public ToolStripAdapter(InteractionUnit interactionUnit) {
            this.unit = interactionUnit;

            this.tools = new org.jboss.ballroom.client.widgets.tools.ToolStrip();
        }

        @Override
        public InteractionUnit getInteractionUnit() {
            return unit;
        }

        @Override
        public void add(ReificationWidget widget) {

            //System.out.println("Add "+ widget.getInteractionUnit() + " to " + unit);
            InteractionUnit unit = widget.getInteractionUnit();
            DMRMapping mapping = (DMRMapping) unit.findMapping(MappingType.DMR);

            if(unit.doesProduce())
            {
                Iterator<Resource<ResourceType>> iterator = unit.getOutputs().iterator();
                Resource<ResourceType> resource =  iterator.next();

                String resourceAddress = mapping.getAddress();
                String operationName = resource.getId().getSuffix();

                if(securityContext.getOperationPriviledge(resourceAddress, operationName).isGranted())
                    tools.addToolWidgetRight(widget.asWidget());
            }
            else
            {
                // TODO: Should this be an error? IMO any toolbar unit should be a producer
                tools.addToolWidgetRight(widget.asWidget());
            }

        }

        @Override
        public Widget asWidget() {
            return tools.asWidget();
        }
    }
}
