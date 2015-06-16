package org.jboss.as.console.client.v3.stores.domain.actions;

import org.jboss.gwt.circuit.Action;

/**
 * @author Heiko Braun
 * @since 16/06/15
 */
public class RemoveProfile implements Action {
    String name;

    public RemoveProfile(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
