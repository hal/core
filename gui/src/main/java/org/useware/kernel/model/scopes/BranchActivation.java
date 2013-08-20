package org.useware.kernel.model.scopes;

import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;
import org.useware.kernel.model.structure.builder.InteractionUnitVisitor;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Activate the first child of each container in the LHS branch
 *
 * @author Heiko Braun
 */
public class BranchActivation implements InteractionUnitVisitor {

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

        if(null==prev)
        {
            // the topmost element is always active
            activeItems.put(0, container.getId());
        }
        else if(prev.getTemporalOperator().isScopeBoundary()
                && !pastPivot)
        {
            // select first child and skip the remaining ones
            QName activeChild = activeItems.get(stack.size()-1);
            if(null==activeChild)
                activeItems.put(stack.size()-1, container.getId());
        }
    }

    @Override
    public void visit(InteractionUnit unit) {

        QName activeChild = activeItems.get(stack.size()-1);

        if(null==activeChild && !pastPivot)
        {
            activeItems.put(stack.size()-1, unit.getId());
        }

    }

    @Override
    public void endVisit(Container container) {


        stack.pop();

    }

    public Map<Integer, QName> getActiveItems() {
        return activeItems;
    }
}

