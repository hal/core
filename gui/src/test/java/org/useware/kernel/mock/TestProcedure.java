package org.useware.kernel.mock;

import org.useware.kernel.gui.behaviour.Procedure;
import org.useware.kernel.model.structure.QName;

/**
 * @author Heiko Braun
 * @date 2/19/13
 */
public class TestProcedure extends Procedure {

    public TestProcedure(String ns, String name) {
        super(new QName(ns, name), null);
    }

    public TestProcedure(QName id, QName requiredOrigin) {
        super(id, requiredOrigin);
    }

    public TestProcedure(QName id) {
        super(id, null);
    }

    @Override
    public QName getJustification() {
        return null;
    }
}
