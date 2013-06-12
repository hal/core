package org.useware.kernel.gui.behaviour;

import org.useware.kernel.model.behaviour.Behaviour;
import org.useware.kernel.model.behaviour.Resource;
import org.useware.kernel.model.behaviour.ResourceType;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;
import org.useware.kernel.model.structure.builder.InteractionUnitVisitor;

import java.util.Map;
import java.util.Set;

/**
 *
 * @author Heiko Braun
 * @date 11/16/12
 */
public class Integrity {

    public static void check(InteractionUnit container, final Map<QName, Set<Procedure>> behaviours)
            throws IntegrityErrors {

        final IntegrityErrors err = new IntegrityErrors();

        container.accept(new InteractionUnitVisitor() {
            @Override
            public void startVisit(Container container) {
                if (container.doesProduce())
                    assertConsumer(container, behaviours, err);

                if (container.doesConsume())
                    assertProducer(container, behaviours, err);

            }

            @Override
            public void visit(InteractionUnit interactionUnit) {
                if (interactionUnit.doesProduce())
                    assertConsumer(interactionUnit, behaviours, err);

                if (interactionUnit.doesConsume())
                    assertProducer(interactionUnit, behaviours, err);

            }

            @Override
            public void endVisit(Container container) {

            }
        });

        if(err.needsToBeRaised())
            throw err;
    }

    /**
     * Assertion that a consumer exists for the produced resources of an interaction unit.
     *
     * @param unit
     * @param err
     */
    private static void assertConsumer(InteractionUnit unit, Map<QName, Set<Procedure>> behaviours, IntegrityErrors err) {

        Set<Resource<ResourceType>> producedTypes = unit.getOutputs();

        for (Resource<ResourceType> resource : producedTypes) {
            boolean match = false;

            for(QName id : behaviours.keySet())
            {
                for (Behaviour behaviour : behaviours.get(id)) {

                    if (behaviour.doesConsume(resource)) {
                        match = true;
                        break;
                    }
                }
            }
            if (!match)
                err.add(unit.getId(), "Missing consumer for <<" + resource + ">>");
        }

    }

    /**
     * Assertion that a producer exists for the consumed resources of an interaction unit.
     *
     * @param unit
     * @param behaviours
     * @param err
     */
    private static void assertProducer(InteractionUnit unit, Map<QName, Set<Procedure>> behaviours, IntegrityErrors err) {
        Set<Resource<ResourceType>> consumedTypes = unit.getInputs();

        for (Resource<ResourceType> resource : consumedTypes) {
            boolean match = false;

            for(QName id : behaviours.keySet())
            {
                for (Behaviour candidate : behaviours.get(id)) {
                    if (candidate.doesProduce(resource)) {
                        match = candidate.getJustification() == null || unit.getId().equals(candidate.getJustification());
                    }

                    if(match)break;
                }

                if(match)break;
            }

            if (!match)
                err.add(unit.getId(), "Missing producer for <<" + resource + ">>");
        }
    }
}
