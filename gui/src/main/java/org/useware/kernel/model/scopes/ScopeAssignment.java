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

    private final static QName ROOT = QName.valueOf("org.useware.scope:root");

    private final ScopeModel<Scope> scopeModel;
    private int scopeIdx = 0;
    private Stack<ScopeApplication> stack = new Stack<ScopeApplication>();

    public ScopeAssignment() {
        this.scopeModel = new ScopeModel<Scope>();

        final Node<Scope> rootScope  = new Node<Scope>( ROOT, new Scope(0, true) );

        scopeModel.setRootElement(rootScope);

        // install root reference
        stack.push(
                new ScopeApplication(rootScope) {
                    @Override
                    Node<Scope> applyToContainer(Container container) {

                        // any container below root has a distinct scope
                        return rootScope.addChild(
                                new Node(
                                        container.getId(),
                                        new Scope(createContextId(), container.getTemporalOperator().isScopeBoundary())
                                )
                        );
                    }

                    @Override
                    void applyToUnit(InteractionUnit unit) {
                        throw new IllegalArgumentException("Root scope doesn't support child units");
                    }
                }
        );
    }

    @Override
    public void startVisit(final Container container) {

        // apply scope to container
        final Node<Scope> scope = stack.peek().applyToContainer(container);

        // push another child strategy
        stack.push(new ScopeApplication(scope) {
            @Override
            Node<Scope> applyToContainer(Container childContainer) {

                Scope parentScope = scope.getData();
                Integer scopeId = parentScope.isDemarcationType() ? createContextId() : parentScope.getScopeId();

                // container added as child, inherits parent scope
                return scope.addChild(
                        new Node(
                                childContainer.getId(),
                                new Scope(scopeId, childContainer.getTemporalOperator().isScopeBoundary())
                        )
                );
            }

            @Override
            void applyToUnit(InteractionUnit unit) {

                Scope parentScope = scope.getData();
                Integer scopeId = parentScope.isDemarcationType() ? createContextId() : parentScope.getScopeId();

                scope.addChild(
                        new Node(
                                unit.getId(),
                                new Scope(scopeId, false)
                        )
                );
            }
        });

    }

    @Override
    public void visit(InteractionUnit<S> unit) {
        stack.peek().applyToUnit(unit);
    }

    @Override
    public void endVisit(Container container) {
        stack.pop();
    }

    public ScopeModel<Scope> getScopeModel() {
        return scopeModel;
    }

    abstract class ScopeApplication {

        private Node<Scope> parentScope;

        protected ScopeApplication(Node<Scope> parentScope) {
            this.parentScope = parentScope;
        }

        Node<Scope> getParentScope() {
            return parentScope;
        }

        abstract Node<Scope> applyToContainer(Container childContainer);
        abstract void applyToUnit(InteractionUnit childUnit);
    }

    private Integer createContextId() {
        int contextId = ++scopeIdx;
        return contextId;
    }
}
