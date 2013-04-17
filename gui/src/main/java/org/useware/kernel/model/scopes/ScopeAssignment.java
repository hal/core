package org.useware.kernel.model.scopes;

import org.useware.kernel.model.mapping.Node;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.builder.InteractionUnitVisitor;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import static org.useware.kernel.model.structure.TemporalOperator.Choice;
import static org.useware.kernel.model.structure.TemporalOperator.Deactivation;

/**
 * Assign scopes interaction units to scopes.Creates a shim tree of the structure model.
 * <p/>
 * Scopes are assigned by interpretation of {@link org.useware.kernel.model.structure.TemporalOperator}'s.
 * If an operator acts as a scope boundary then a new scope id wil be assigned.
 * Atomic units inherit the scoped of their parents.
 *
 * @param <S> the supported stereotypes
 *
 * @see org.useware.kernel.model.structure.TemporalOperator#isScopeBoundary()
 *
 * @author Heiko Braun
 */
public class ScopeAssignment<S extends Enum<S>> implements InteractionUnitVisitor<S> {

    private final InterfaceStructureShim<Integer> scopeShim;
    private final Set<Integer> contextIds = new HashSet<Integer>();
    private Stack<Scope> stack = new Stack<Scope>();

    private int scopeIdx = 0;

    public ScopeAssignment() {
        this.scopeShim = new InterfaceStructureShim<Integer>();
    }

    private Integer createContextId() {
        int contextId = ++scopeIdx;
        contextIds.add(contextId);
        return contextId;
    }

    public Set<Integer> getContextIds() {
        return contextIds;
    }

    @Override
    public void startVisit(Container container) {

        Node<Integer> containerNode = null;

        if(stack.isEmpty())
        {
            // top level: create new root node
            final Node<Integer> rootNode = new Node<Integer>(container.getId());
            rootNode.setData(createContextId());
            scopeShim.setRootElement(rootNode);
            stack.push(new Scope(rootNode) {
                @Override
                Integer getContextId() {
                    return rootNode.getData();
                }
            });

            containerNode = rootNode;
        }
        else
        {
            // child level: add new child & re-assign current container
            containerNode = stack.peek().getNode().addChild(container.getId());
        }

        if(container.getTemporalOperator().isScopeBoundary())
        {
            // scope boundary, assign new scope id
            stack.push(new Scope(containerNode, stack.peek().getContextId()) {
                @Override
                Integer getContextId() {
                    return createContextId();
                }
            });

        }
        else
        {
            // re-use parent context id
            final Integer sharedContextId = stack.peek().getContextId();
            stack.push(new Scope(containerNode) {

                @Override
                Integer getContextId() {
                    return sharedContextId;
                }
            });

        }

    }

    @Override
    public void visit(InteractionUnit<S> interactionUnit) {

        // atomic units inherit the scope from their parents

        Scope scope = stack.peek();
        Node<Integer> node = scope.getNode().addChild(interactionUnit.getId());
        node.setData(stack.peek().getContextId());
    }

    @Override
    public void endVisit(Container container) {

        Scope scope = stack.pop();

        if(scope.getPreviousContext()!=null)
            scope.getNode().setData(scope.getPreviousContext());
        else
            scope.getNode().setData(scope.getContextId());

    }

    public InterfaceStructureShim<Integer> getShim() {
        return scopeShim;
    }
}
