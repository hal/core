package org.useware.kernel.model.behaviour;

import org.useware.kernel.model.structure.QName;

/**
 * A represent the concept of behaviour.
 *
 * @author Heiko Braun
 * @date 10/31/12
 */
public interface Behaviour extends SupportsConsumption, SupportsProduction {

    /**
     * The ID corresponds to a Resource of the behaviour model
     * @return
     */
    QName getId();

    /**
     * The justification for the selection of a behaviour.
     * It further discriminates the behaviour apart from the ID.
     * @return
     */
    QName getJustification();
}
