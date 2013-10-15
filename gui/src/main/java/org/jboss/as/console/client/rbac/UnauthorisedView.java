package org.jboss.as.console.client.rbac;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;

/**
 * @author Heiko Braun
 * @date 7/24/13
 */
public class UnauthorisedView extends SuspendableViewImpl implements UnauthorisedPresenter.MyView {

    @Override
    public Widget createWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("rhs-content-panel");
        layout.add(new ContentHeaderLabel(Console.CONSTANTS.unauthorized()));
        layout.add(new ContentDescription(Console.CONSTANTS.unauthorized_desc()));
        return layout;
    }
}
