package org.jboss.as.console.client.shared.model;

import org.jboss.gwt.circuit.Action;

/**
 * @author Heiko Braun
 * @since 18/09/14
 */
public class SelectPerspective implements Action {
    private String parent;
    private String child;

    public SelectPerspective(String parent, String child) {
        this.parent = parent;
        this.child = child;
    }

    public String getParent() {
        return parent;
    }

    public String getChild() {
        return child;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SelectPerspective that = (SelectPerspective) o;

        if (!child.equals(that.child)) return false;
        if (!parent.equals(that.parent)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = parent.hashCode();
        result = 31 * result + child.hashCode();
        return result;
    }
}
