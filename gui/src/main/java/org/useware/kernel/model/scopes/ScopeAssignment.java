package org.useware.kernel.model.scopes;

import org.useware.kernel.model.mapping.Node;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;
import org.useware.kernel.model.structure.builder.InteractionUnitVisitor;

import java.util.Stack;

/**
 * Assign interaction units to scopes. Creates a shadow tree of the structural model.
 * <p/>
 * Scopes are assigned by interpretation of {@link org.useware.kernel.model.structure.TemporalOperator}'s.
 * If an operator acts as a scope boundary then a new scope id wil be assigned.
 * Atomic units inherit the scope of their parents.
 *
 * @param <S> the supported stereotypes
 *
 * @see org.useware.kernel.model.structure.TemporalOperator#isScopeBoundary()
 *
 * @author Heiko Braun
 */
public class ScopeAssignment<S extends Enum<S>> implements InteractionUnitVisitor<S> {

    private final ScopeModel scopeModel;
    private int scopeIdx = 0;
    private Stack<Directive> stack = new Stack<Directive>();

    public ScopeAssignment() {
        this.scopeModel = new ScopeModel();

        final Node<Scope> rootScope  = new Node<Scope>( new Scope(scopeIdx) );

        scopeModel.setRootElement(rootScope);

        // install root directive
        stack.push(
                new Directive(rootScope, true, QName.valueOf("org.useware:root")) { // any container below root has a distinct scope
                    @Override
                    Node<Scope> applyContainer(Container container) {

                        Integer scopeId = newScopeId();
                        container.setScopeId(scopeId);

                        return rootScope.addChild(
                                new Node(
                                        new Scope(scopeId)
                                )
                        );
                    }

                    @Override
                    void applyUnit(InteractionUnit unit) {
                        throw new IllegalArgumentException("Root scope doesn't support child units");
                    }
                }
        );
    }

    @Override
    public void startVisit(Container container) {

        // apply scope to container
        final Node<Scope> scope = stack.peek().applyContainer(container);

        boolean isBoundary = container.getTemporalOperator().isScopeBoundary();

        // push another child strategy
        stack.push(new Directive(scope, isBoundary, container.getId()) {
            @Override
            Node<Scope> applyContainer(Container childContainer) {

                if(isBoundary())
                {
                    // new scope
                    Integer scopeId = newScopeId();
                    childContainer.setScopeId(scopeId);

                    return getParentScope().addChild(
                            new Node(
                                    new Scope(scopeId)
                            )
                    );
                }
                else
                {
                    // scope remains the same
                    childContainer.setScopeId(
                            getParentScope().getData().getId()
                    );

                    return getParentScope();
                }

            }

            @Override
            void applyUnit(InteractionUnit unit) {

                int scopeId = getParentScope().getData().getId();
                unit.setScopeId(scopeId);

                getParentScope().addChild(
                        new Node(
                                new Scope(scopeId)
                        )
                );
            }
        });

    }

    @Override
    public void visit(InteractionUnit<S> unit) {
        stack.peek().applyUnit(unit);
    }

    @Override
    public void endVisit(Container container) {
        stack.pop();
    }

    public ScopeModel getScopeModel() {
        return scopeModel;
    }

    abstract class Directive {

        private final boolean boundary;
        private final QName parentId;
        private Node<Scope> parentScope;

        protected Directive(Node<Scope> parentScope, boolean boundary, QName parentId) {
            this.parentScope = parentScope;
            this.boundary = boundary;
            this.parentId = parentId; // debug utility
        }

        boolean isBoundary() {
            return boundary;
        }

        protected Node<Scope> getParentScope() {
            return parentScope;
        }

        abstract Node<Scope> applyContainer(Container childContainer);
        abstract void applyUnit(InteractionUnit childUnit);
    }

    private Integer newScopeId() {
        return ++scopeIdx;
    }
}
