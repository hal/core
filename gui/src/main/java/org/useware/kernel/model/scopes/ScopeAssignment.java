package org.useware.kernel.model.scopes;

import org.useware.kernel.model.mapping.Node;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.builder.InteractionUnitVisitor;

import java.util.Stack;

/**
 * Assign scopes interaction units to scopes. Creates a shadow tree of the structure model.
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

    private final InterfaceStructureShadow<Scope> shadowTree;

    private Stack<ChildStrategy> stack = new Stack<ChildStrategy>();

    private int scopeIdx = 0;

    public ScopeAssignment() {
        this.shadowTree = new InterfaceStructureShadow<Scope>();
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
            shadowTree.setRootElement(rootNode);

            stack.push(new ChildStrategy(rootNode) {
                @Override
                Integer getOrCreateId() {
                    return rootNode.getData().getScopeId();
                }

                @Override
                boolean childStartsNewScope() {
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
            // children of boundary units will assign new scopes
            stack.push(new ChildStrategy(containerNode, stack.peek().getOrCreateId()) {
                @Override
                Integer getOrCreateId() {
                    return createContextId();
                }

                @Override
                boolean childStartsNewScope() {
                    return true;
                }
            });

        }
        else
        {
            // children of regular units will the the parent scope
            final Integer sharedContextId = stack.peek().getOrCreateId();
            stack.push(new ChildStrategy(containerNode) {

                @Override
                Integer getOrCreateId() {
                    return sharedContextId;
                }

                @Override
                boolean childStartsNewScope() {
                    return false;
                }
            });

        }

    }

    @Override
    public void visit(InteractionUnit<S> interactionUnit) {

        ChildStrategy strategy = stack.peek();
        Node<Scope> node = strategy.getNode().addChild(interactionUnit.getId());
        node.setData(new Scope(
                strategy.getOrCreateId(),
                strategy.childStartsNewScope())
        );

    }

    @Override
    public void endVisit(Container container) {

        ChildStrategy strategy = stack.pop();

        // the demarcation containers inherit the previous scope
        // only their children may create new scopes (if necessary)
        if(strategy.childStartsNewScope())
        {
            strategy.getNode().setData(
                    new Scope(
                            strategy.getPreviousScope(),
                            strategy.childStartsNewScope()
                    )
            );
        }
        else
        {
            strategy.getNode().setData(
                    new Scope(
                            strategy.getOrCreateId(),
                            strategy.childStartsNewScope()
                    )
            );
        }

    }

    public InterfaceStructureShadow<Scope> getScopeModel() {
        return shadowTree;
    }


    abstract class ChildStrategy {

        Node<Scope> node;
        Integer previousScope = null;

        protected ChildStrategy(Node<Scope> container) {
            this.node = container;
        }

        public Integer getPreviousScope() {
            return previousScope;
        }

        protected ChildStrategy(Node<Scope> container, Integer previousScope) {
            this.node = container;
            this.previousScope = previousScope;
        }

        public Node<Scope> getNode() {
            return node;
        }

        abstract Integer getOrCreateId();

        abstract boolean childStartsNewScope();
    }
}
