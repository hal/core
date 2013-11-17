package org.jboss.as.console.mbui.reification.pipeline;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.mbui.model.StereoTypes;
import org.useware.kernel.gui.reification.Context;
import org.useware.kernel.gui.reification.strategy.ReificationStrategy;
import org.useware.kernel.gui.reification.strategy.ReificationWidget;
import org.useware.kernel.model.structure.InteractionUnit;

/**
 * @author Heiko Braun
 * @date 17/11/13
 */
public class TodoStrategy implements ReificationStrategy<ReificationWidget, StereoTypes> {

    @Override
    public boolean prepare(InteractionUnit<StereoTypes> interactionUnit, Context context) {
        return true;
    }

    @Override
    public ReificationWidget reify(final InteractionUnit<StereoTypes> interactionUnit, Context context) {
        final HTML placeholder = new HTML();
        placeholder.getElement().setAttribute("style", "border:1px solid red; padding:20px;font-size:16px");
        placeholder.setText("TODO:"+interactionUnit.getLabel());

        return new ReificationWidget() {
            @Override
            public InteractionUnit getInteractionUnit() {
                return interactionUnit;
            }

            @Override
            public void add(ReificationWidget widget) {
                throw new RuntimeException("Not supported");
            }

            @Override
            public Widget asWidget() {
                return placeholder;
            }
        };
    }

    @Override
    public boolean appliesTo(InteractionUnit<StereoTypes> interactionUnit) {
        return interactionUnit.getStereotype() == StereoTypes.Todo;
    }
}
