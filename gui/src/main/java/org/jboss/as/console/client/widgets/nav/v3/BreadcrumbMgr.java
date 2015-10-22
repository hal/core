package org.jboss.as.console.client.widgets.nav.v3;

import java.util.Stack;

/**
 * @author Heiko Braun
 * @since 10/03/15
 */
public class BreadcrumbMgr {
    private Stack<BreadcrumbEvent> navigationStack = new Stack<>();
    private int breadcrumbCursor = 0;

    public Stack<BreadcrumbEvent> getNavigationStack() {
        return navigationStack;
    }

    public int getBreadcrumbCursor() {
        return breadcrumbCursor;
    }

    public void setBreadcrumbCursor(int breadcrumbCursor) {
        if(breadcrumbCursor> navigationStack.size()-1)
            throw new IllegalArgumentException("Cursor ("+breadcrumbCursor+") exceeds stack size ("+navigationStack.size()+")");
        this.breadcrumbCursor = breadcrumbCursor;
    }
}
