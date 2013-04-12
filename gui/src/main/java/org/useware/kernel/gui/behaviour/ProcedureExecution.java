package org.useware.kernel.gui.behaviour;

import org.useware.kernel.model.structure.QName;

import java.util.Map;
import java.util.Set;

/**
 * @author Heiko Braun
 * @date 2/21/13
 */
public interface ProcedureExecution {

    void addProcedure(Procedure procedure);

    Map<QName, Set<Procedure>> listProcedures();
}
