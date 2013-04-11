package org.jboss.as.console.mbui.behaviour;

import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.mbui.gui.behaviour.Procedure;

/**
 * Contract between {@link Procedure}'s and business logic components
 * @author Heiko Braun
 * @date 2/22/13
 */
public interface ProcedureContract {

    DispatchAsync getDispatcher();
}
