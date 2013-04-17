package org.useware.kernel.gui.reification;

import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;
import org.useware.kernel.model.structure.TemporalOperator;
import org.useware.kernel.model.structure.builder.InteractionUnitVisitor;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Activate the first child of each container in the LHS branch
 *
 * @author Heiko Braun
 */
public class ActivationVisitor implements InteractionUnitVisitor {

    private Stack<Container> stack = new Stack<Container>();
    private Map<Integer, QName> activeItems = new HashMap<Integer,QName>();
    boolean pastPivot = false;
    int max = 0;

    @Override
    public void startVisit(Container container) {

        Container prev = null;
        if(!stack.isEmpty())
            prev = stack.peek();

        stack.push(container);

        if(prev!=null
                && prev.getTemporalOperator().isScopeBoundary()
                && !pastPivot)
        {
            QName activeChild = activeItems.get(stack.size()-1);
            if(null==activeChild)
                activeItems.put(stack.size()-1, container.getId());
        }
    }

    @Override
    public void visit(InteractionUnit unit) {


        /*  Ignore atomic units

        QName activeChild = activeItems.get(stack.size()-1);

        if(null==activeChild && !pastPivot)
        {
            activeItems.put(stack.size()-1, unit.getId());
        }
         */
    }

    @Override
    public void endVisit(Container container) {

        if(max<stack.size())
            max =  stack.size();
        else
            pastPivot = true;

        stack.pop();

    }

    public Map<Integer, QName> getActiveItems() {
        return activeItems;
    }
}

