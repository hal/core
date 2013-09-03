package org.jboss.as.console.client.shared;

import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.SuspendableViewImpl;

/**
 * @author Harald Pehl
 * @date 10/30/2012
 */
public class DialogViewImpl extends SuspendableViewImpl implements DialogView
{
    private LayoutPanel container;

    @Override
    public Widget asWidget() {
        return super.asWidget();
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