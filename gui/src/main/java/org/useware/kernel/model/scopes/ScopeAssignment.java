package org.useware.kernel.model.scopes;

import org.useware.kernel.model.mapping.Node;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.builder.InteractionUnitVisitor;

import java.util.Stack;

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

    private final InterfaceStructureShim<Scope> scopeShim;

    private Stack<ScopeRef> stack = new Stack<ScopeRef>();

    private int scopeIdx = 0;

    public ScopeAssignment() {
        this.scopeShim = new InterfaceStructureShim<Scope>();
    }

    private Integer createContextId() {
        int contextId = ++scopeIdx;
        return contextId;
    }

    @Override
    public void startVisit(Container container) {

        Node<Scope> containerNode = null;

        if(stack.isEmpty())  // root level: create parent
        {
            // top level: create new root node
            final Node<Scope> rootNode = new Node<Scope>(container.getId());
            rootNode.setData(new Scope(createContextId(), true));
            scopeShim.setRootElement(rootNode);
            stack.push(new ScopeRef(rootNode) {
                @Override
                Integer getContextId() {
                    return rootNode.getData().getScopeId();
                }

                @Override
                boolean isDemarcationUnit() {
                    return true;
                }
            });

            containerNode = rootNode;
        }
        else
        {
            // below root: add container as new child
            containerNode = stack.peek().getNode().addChild(container.getId());
        }

        if(container.getTemporalOperator().isScopeBoundary())
        {
            // scope boundary, assign new scope id
            stack.push(new ScopeRef(containerNode, stack.peek().getContextId()) {
                @Override
                Integer getContextId() {
                    return createContextId();
                }

                @Override
                boolean isDemarcationUnit() {
                    return true;
                }
            });

        }
        else
        {
            // re-use parent context id
            final Integer sharedContextId = stack.peek().getContextId();
            stack.push(new ScopeRef(containerNode) {

                @Override
                Integer getContextId() {
                    return sharedContextId;
                }

                @Override
                boolean isDemarcationUnit() {
                    return false;
                }
            });

        }

    }

    @Override
    public void visit(InteractionUnit<S> interactionUnit) {

        // atomic units inherit the scope from their parents

        ScopeRef scopeRef = stack.peek();
        Node<Scope> node = scopeRef.getNode().addChild(interactionUnit.getId());
        node.setData(new Scope(stack.peek().getContextId(), stack.peek().isDemarcationUnit()));
    }

    @Override
    public void endVisit(Container container) {

        ScopeRef scope = stack.pop();

        if(scope.getPreviousContext()!=null)
            scope.getNode().setData(new Scope(scope.getPreviousContext(), scope.isDemarcationUnit()));
        else
            scope.getNode().setData(new Scope(scope.getContextId(), scope.isDemarcationUnit()));

    }

    public InterfaceStructureShim<Scope> getShim() {
        return scopeShim;
    }


    abstract class ScopeRef {

        Node<Scope> node;
        Integer previousContext = null;

        protected ScopeRef(Node<Scope> container) {
            this.node = container;
        }

        public Integer getPreviousContext() {
            return previousContext;
        }

        protected ScopeRef(Node<Scope> container, Integer previousContext) {
            this.node = container;
            this.previousContext = previousContext;
        }

        public Node<Scope> getNode() {
            return node;
        }

        abstract Integer getContextId();

        abstract boolean isDemarcationUnit();
    }
}
