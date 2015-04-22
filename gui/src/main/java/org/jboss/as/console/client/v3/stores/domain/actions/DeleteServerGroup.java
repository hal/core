package org.jboss.as.console.client.v3.stores.domain.actions;

import org.jboss.gwt.circuit.Action;

/**
 * @author Heiko Braun
 * @since 21/04/15
 */
public class DeleteServerGroup implements Action {
    String name;

    public DeleteServerGroup(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
