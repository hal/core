package org.jboss.as.console.client.widgets;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Heiko Braun
 * @date 7/29/13
 */
public class DefaultSplitLayoutPanel extends SplitLayoutPanel {

    public DefaultSplitLayoutPanel(int splitterSize) {
        super(splitterSize);
    }

    @Override
    public void addWest(Widget widget, double size) {
        widget.addStyleName("split-west");
        super.addWest(widget, size);
    }

    @Override
    public void addWest(IsWidget widget, double size) {
        Widget w = widget.asWidget();
        addWest(w, size);
    }

    @Override
    public void add(Widget widget) {
        widget.addStyleName("split-center");
        super.add(widget);
    }
}
