package org.jboss.as.console.mbui.behaviour;

import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.ModelNode;
import org.useware.kernel.gui.behaviour.InteractionCoordinator;
import org.useware.kernel.gui.behaviour.StatementContext;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.mapping.as7.AddressMapping;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;

import java.util.Map;

/**
 * @author Heiko Braun
 * @date 3/6/13
 */
public interface OperationContext {

    Dialog getDialog();
    InteractionUnit getUnit();
    AddressMapping getAddress();
    DispatchAsync getDispatcher();

    StatementContext getStatementContext();

    InteractionCoordinator getCoordinator();

    Map<QName, ModelNode> getOperationDescriptions();
}
