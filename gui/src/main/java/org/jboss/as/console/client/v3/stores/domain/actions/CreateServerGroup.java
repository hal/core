package org.jboss.as.console.client.v3.stores.domain.actions;

import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.gwt.circuit.Action;

/**
 * @author Heiko Braun
 * @since 21/04/15
 */
public class CreateServerGroup implements Action {

    ServerGroupRecord group;

    public CreateServerGroup(ServerGroupRecord group) {
        this.group = group;
    }

    public ServerGroupRecord getGroup() {
        return group;
    }
}
