package org.jboss.mbui.gui.behaviour;

import org.jboss.mbui.model.structure.QName;

/**
 * @author Heiko Braun
 * @date 4/11/13
 */
public interface ProcedureFactory {
    Procedure createProcedure(QName id);
}
