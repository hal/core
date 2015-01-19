package org.jboss.as.console.client.v3.stores.domain.actions;


import org.jboss.gwt.circuit.Action;

/**
 * @author Heiko Braun
 * @since 19/01/15
 */
public class FilterType implements Action {

    public final static String HOST = "host";
    public final static String GROUP = "group";

    private String filter = HOST;

    public FilterType(String filter) {
        this.filter = filter;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }
}
