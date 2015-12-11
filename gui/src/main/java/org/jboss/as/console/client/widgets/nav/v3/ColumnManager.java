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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * @author Heiko Braun
 * @since 26/02/15
 */
public class ColumnManager {

    private static final int DEFAULT_COLUMN_WIDTH = 256;
    private static final int DEFAULT_PREVIEW_WIDTH = 256;
    private static final int CONTAINER_WIDTH = 1500;

    private boolean initialized;
    private Stack<Widget> visibleColumns = new Stack<>();
    private Widget activeSelectionWidget;
    private SplitLayoutPanel splitlayout;
    private final HasHandlers eventBus;
    private List<Widget> westWidgets = new LinkedList<>();
    private final FinderColumn.FinderId finderId;

    /**
     * number of columns visible across all finders (and contributions)
     */
    private static Map<FinderColumn.FinderId, Integer> totalColumnsVisible = new HashMap<>();
    private Widget centerWidget;


    public ColumnManager(SplitLayoutPanel delegate, FinderColumn.FinderId finderId) {
        this.splitlayout = delegate;
        this.finderId = finderId;
        this.eventBus = Console.MODULES.getPlaceManager();

        this.initialized = false;

        // default state
        if(null==totalColumnsVisible.get(finderId))
            totalColumnsVisible.put(finderId, 0);

        this.splitlayout.addAttachHandler(new AttachEvent.Handler() {
            @Override
            public void onAttachOrDetach(AttachEvent event) {
                if(!event.isAttached())
                {
                    //System.out.println("Detach finder");
                    decreaseTotalVisibleBy(ColumnManager.this.visibleColumns.size());

                    assertVisibleColumns();
                }
                else
                {

                    // skip the first attach event

                    if(initialized) {

                       // System.out.println("Attach finder");

                        if (visibleColumns.size() > 0) {
                            increaseTotalVisibleBy(ColumnManager.this.visibleColumns.size());

                            assertVisibleColumns();
                        }
                    }
                    else
                    {
                        initialized = true;
                    }

                    scrollIfNecessary();

                }
            }
        });


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

    private void increaseTotalVisibleBy(int amount) {
        Integer visibleColumns = totalColumnsVisible.get(finderId);
        totalColumnsVisible.put(finderId, (visibleColumns += amount));
    }

    private void decreaseTotalVisibleBy(int amount) {
        Integer visibleColumns = totalColumnsVisible.get(finderId);
        Integer target = visibleColumns -= amount;
        Integer value = target < 0 ? 0 : target;
        totalColumnsVisible.put(finderId, value);
    }

    private void assertVisibleColumns() {
        //System.out.println("Num Visible: "+ totalColumnsVisible);

        Integer visibleColumns = totalColumnsVisible.get(finderId);
        if(visibleColumns <0)
            new RuntimeException("Illegal number of visible columns in finder "+finderId+": "+visibleColumns).printStackTrace();
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

        increaseTotalVisibleBy(1);

        scrollIfNecessary();
    }

    public void reduceColumnsTo(int level) {

        for(int i=visibleColumns.size()-1; i>=level; i--)
        {
            final Widget widget = visibleColumns.pop();
            splitlayout.setWidgetHidden(widget, true);
            decreaseTotalVisibleBy(1);
        }

        scrollIfNecessary();

    }

    public int getVisibleComunsSize() {
        return visibleColumns.size();
    }

    private void scrollIfNecessary() {
        assertVisibleColumns();

        int widthConstraint = Window.getClientWidth()<CONTAINER_WIDTH ? Window.getClientWidth() : CONTAINER_WIDTH;

        Integer visibleColumns = totalColumnsVisible.get(finderId);
        int requiredWidth = visibleColumns * DEFAULT_COLUMN_WIDTH + DEFAULT_PREVIEW_WIDTH;

        if(requiredWidth>widthConstraint)
        {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    FinderScrollEvent.fire(Console.getPlaceManager(), true, requiredWidth);
                }
            });

            /*System.out.println("Scrolling necessary!");
            System.out.println(widthConstraint+"/"+requiredWidth);*/

        }
        else
        {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    FinderScrollEvent.fire(Console.getPlaceManager(), false, 0);
                }
            });
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
            });*/
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
        this.centerWidget = widget;
    }

    public void setInitialVisible(int index) {
        int i=0;

        for (Widget widget : westWidgets)
        {
            if(i<index) {

                visibleColumns.push(widget);
                increaseTotalVisibleBy(1);
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
