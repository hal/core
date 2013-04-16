package org.useware.kernel.gui.reification;

import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.builder.InteractionUnitVisitor;

import java.util.Stack;

import static org.useware.kernel.model.structure.TemporalOperator.Choice;

public class ActivationVisitor implements InteractionUnitVisitor {

    private Stack<Container> stack = new Stack<Container>();
    private InteractionUnit candidate = null;

    @Override
    public void startVisit(Container container) {

        stack.push(container);

        if(null==candidate && canBePromoted(container))
            candidate = container;
        else if(isDiscrimiator(container)) // discriminating types replace anything else
            candidate = null;

    }

    @Override
    public void visit(InteractionUnit unit) {

        if(null==candidate && canBePromoted(unit))   // units replace container
            candidate = unit;
    }

    @Override
    public void endVisit(Container container) {

        Container prev = stack.pop();
    }

    boolean canBePromoted(Container container)
    {
        return !isDiscrimiator(container); // only non-discriminating types
    }

    boolean canBePromoted(InteractionUnit unit)
    {
        return true; // any at the moment
    }

    boolean isDiscrimiator(Container container)
    {
        return container.getTemporalOperator() == Choice;
    }

    public InteractionUnit getCandidate() {
        return candidate;
    }
}

