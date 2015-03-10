package org.jboss.as.console.client.widgets.nav.v3;

import java.util.Stack;

/**
 * @author Heiko Braun
 * @since 10/03/15
 */
public class BreadcrumbMgr {
    private FinderColumn.FinderId lastFinderType;
    private Stack<BreadcrumbEvent> navigationStack = new Stack<>();
    private int breadcrumbCursor = 0;

    public FinderColumn.FinderId getLastFinderType() {
        return lastFinderType;
    }

    public void setLastFinderType(FinderColumn.FinderId lastFinderType) {
        this.lastFinderType = lastFinderType;
    }

    public Stack<BreadcrumbEvent> getNavigationStack() {
        return navigationStack;
    }

    public int getBreadcrumbCursor() {
        return breadcrumbCursor;
    }

    public void setBreadcrumbCursor(int breadcrumbCursor) {
        this.breadcrumbCursor = breadcrumbCursor;
    }
}
