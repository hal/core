package org.jboss.as.console.client.widgets.nav.v3;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.Window;
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

    private static final int DEFAULT_COLUMN_WIDTH = 300;
    private static final int DEFAULT_PREVIEW_WIDTH = 500;
    private static final int CONTAINER_WIDTH = 1500;

    private boolean initialized;
    private Stack<Widget> visibleColumns = new Stack<>();
    private Widget activeSelectionWidget;
    private SplitLayoutPanel splitlayout;
    private final HasHandlers eventBus;
    private List<Widget> westWidgets = new LinkedList<>();

    static {
        Window.addResizeHandler(new ResizeHandler() {
            @Override
            public void onResize(ResizeEvent resizeEvent) {
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        scrollIfNecessary();
                    }
                });
            }
        });
    }

    /**
     * number of columns visible across all finders (and contributions)
     */
    private static int totalColumnsVisible = 0;
    //private Widget centerWidget;

    public ColumnManager(SplitLayoutPanel delegate) {
        this.splitlayout = delegate;
        this.eventBus = Console.MODULES.getPlaceManager();

        this.initialized = false;

        this.splitlayout.addAttachHandler(new AttachEvent.Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                if(!event.isAttached())
                {
                    System.out.println("Detach finder");
                    totalColumnsVisible -= visibleColumns.size();

                    assertVisibleColumns();
                }
                else
                {

                    // skip the first attach event

                    if(initialized) {

                        System.out.println("Attach finder");

                        if (visibleColumns.size() > 0) {
                            totalColumnsVisible += visibleColumns.size();
                            assertVisibleColumns();
                        }
                    }
                    else
                    {
                        initialized = true;
                    }

                }
            }
        });
    }

    private static void assertVisibleColumns() {
        System.out.println("Num Visible: "+ totalColumnsVisible);

        if(totalColumnsVisible<0)
            new RuntimeException("Assertion error").printStackTrace();
    }

    public void updateActiveSelection(Widget widget) {

        ClearFinderSelectionEvent.fire(eventBus);

        if(activeSelectionWidget!=null)
            activeSelectionWidget.getElement().removeClassName("active");
        widget.getElement().addClassName("active");
        activeSelectionWidget = widget;


    }

    public static int getTotalColumnsVisible() {
        return totalColumnsVisible;
    }

    public void appendColumn(final Widget columnWidget) {
        splitlayout.setWidgetHidden(columnWidget, false);
        visibleColumns.push(columnWidget);

        totalColumnsVisible++;

        scrollIfNecessary();
    }

    public void reduceColumnsTo(int level) {


        for(int i=visibleColumns.size()-1; i>=level; i--)
        {
            final Widget widget = visibleColumns.pop();
            splitlayout.setWidgetHidden(widget, true);
            totalColumnsVisible--;
        }

        scrollIfNecessary();

    }

    private static void scrollIfNecessary() {
        assertVisibleColumns();

        int widthConstraint = Window.getClientWidth()<CONTAINER_WIDTH ? Window.getClientWidth() : CONTAINER_WIDTH;
        int requiredWidth = totalColumnsVisible * DEFAULT_COLUMN_WIDTH + DEFAULT_PREVIEW_WIDTH;
        if(requiredWidth>widthConstraint)
        {
            FinderScrollEvent.fire(Console.getPlaceManager(), true, requiredWidth);
            System.out.println("Scrolling necessary!");
            System.out.println(widthConstraint+"/"+requiredWidth);

        }
        else
        {
            FinderScrollEvent.fire(Console.getPlaceManager(), false, 0);
        }
    }

    public void toogleScrolling(boolean enforceScrolling, int requiredSize) {
        if(enforceScrolling)
        {
            splitlayout.getElement().addClassName("scrolling");
            splitlayout.getElement().getParentElement().addClassName("force-overflow");
            splitlayout.getElement().getStyle().setWidth(requiredSize, Style.Unit.PX);

            // scroll into view
            /*Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                   centerWidget.getElement().scrollIntoView();
                }
            });
*/
        }
        else
        {
            splitlayout.getElement().getParentElement().removeClassName("force-overflow");
            splitlayout.getElement().removeClassName("scrolling");
            splitlayout.getElement().getStyle().clearWidth();
        }
    }

    public void addWest(Widget widget) {
        splitlayout.addWest(widget, DEFAULT_COLUMN_WIDTH);
        westWidgets.add(widget);

    }

    public void add(Widget widget) {
        splitlayout.add(widget);
        //this.centerWidget = widget;
    }

    public void setInitialVisible(int index) {
        int i=0;

        for (Widget widget : westWidgets)
        {
            if(i<index) {

                visibleColumns.push(widget);
                totalColumnsVisible++;
            }
            else {
                splitlayout.setWidgetHidden(widget, true);

                /*if(totalColumnsVisible>1)
                    totalColumnsVisible--;*/
            }

            i++;
        }

        assertVisibleColumns();

    }

}
