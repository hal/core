package org.useware.kernel.gui.behaviour;

import org.useware.kernel.model.structure.QName;

/**
 * @author Heiko Braun
 * @date 4/11/13
 */
public interface ProcedureFactory {
    Procedure createProcedure(QName id);
}
