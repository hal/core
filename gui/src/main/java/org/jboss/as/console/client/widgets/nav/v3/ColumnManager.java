package org.jboss.as.console.client.widgets.nav.v3;

import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * @author Heiko Braun
 * @since 26/02/15
 */
public class ColumnManager {

    private Stack<Widget> visibleColumns = new Stack<>();
    private Widget activeSelectionWidget;
    private SplitLayoutPanel splitlayout;
    private final HasHandlers eventBus;
    private List<Widget> westWidgets = new LinkedList<>();

    public ColumnManager(SplitLayoutPanel delegate) {
        this.splitlayout = delegate;
        this.eventBus = Console.MODULES.getPlaceManager();
    }

    public void updateActiveSelection(Widget widget) {

        ClearFinderSelectionEvent.fire(eventBus);

        if(activeSelectionWidget!=null)
            activeSelectionWidget.getElement().removeClassName("active");
        widget.getElement().addClassName("active");
        activeSelectionWidget = widget;


    }

    public void appendColumn(final Widget columnWidget) {
        splitlayout.setWidgetHidden(columnWidget, false);
        visibleColumns.push(columnWidget);
    }

    public void reduceColumnsTo(int level) {

        for(int i=visibleColumns.size()-1; i>=level; i--)
        {
            final Widget widget = visibleColumns.pop();
            splitlayout.setWidgetHidden(widget, true);
        }

    }

    public void addWest(Widget widget) {
        splitlayout.addWest(widget, 217);
        westWidgets.add(widget);
    }

    public void add(Widget widget) {
        splitlayout.add(widget);
    }

    public void setInitialVisible(int index) {
        int i=0;

        for (Widget widget : westWidgets)
        {
            if(i<index)
                visibleColumns.push(widget);
            else
                splitlayout.setWidgetHidden(widget, true);

            i++;
        }
    }

}
