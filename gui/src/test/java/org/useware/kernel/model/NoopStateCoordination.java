package org.useware.kernel.model;

import org.useware.kernel.gui.behaviour.StateCoordination;
import org.useware.kernel.model.structure.QName;

/**
 * @author Heiko Braun
 * @date 4/19/13
 */
public class NoopStateCoordination implements StateCoordination{
    @Override
    public void notifyActivation(QName unitId) {
        // noop
    }
}
