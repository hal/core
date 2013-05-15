package org.jboss.as.console.client.shared.subsys.undertow;

import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.SuspendableViewImpl;

/**
 * @author Heiko Braun
 * @date 5/15/13
 */
public class SimpleViewImpl extends SuspendableViewImpl {
    private LayoutPanel container;


    @Override
    public Widget asWidget() {
        return super.asWidget();    //To change body of overridden methods use File | Settings | File Templates.
    }

    public void show(Widget widget) {
        container.clear();
        container.add(widget);
        //container.setWidgetTopHeight(widget, 0, Style.Unit.PX, 100, Style.Unit.PCT);
    }

    @Override
    public Widget createWidget() {
        this.container = new LayoutPanel();
        this.container.setStyleName("fill-layout");
        return container;
    }
}
