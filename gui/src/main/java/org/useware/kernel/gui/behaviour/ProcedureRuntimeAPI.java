package org.useware.kernel.gui.behaviour;

import org.useware.kernel.model.structure.QName;

/**
 * @author Heiko Braun
 * @date 4/12/13
 */
public interface ProcedureRuntimeAPI {

    boolean isActive(QName interactionUnit);
    boolean canBeActivated(QName interactionUnit);
}
