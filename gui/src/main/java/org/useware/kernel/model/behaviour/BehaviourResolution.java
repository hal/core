package org.useware.kernel.model.behaviour;

import org.useware.kernel.model.structure.QName;

/**
 * Provides means to resolve actual behaviour implementations.
 *
 * @author Heiko Braun
 * @date 2/19/13
 */
public interface BehaviourResolution {

    Behaviour resolve(QName id);
}
