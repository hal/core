package org.useware.kernel.gui.behaviour;

import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.mapping.Node;
import org.useware.kernel.model.mapping.NodePredicate;
import org.useware.kernel.model.scopes.DefaultActivation;
import org.useware.kernel.model.scopes.Scope;
import org.useware.kernel.model.structure.QName;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reflects the dialog state. Breaks down into statements and scopes.
 *
 * @author Heiko Braun
 * @date 1/22/13
 */
public class DialogState {

    private Dialog dialog;
    private final StatementContext externalContext;
    private Map<Integer, MutableContext> scope2context;
    private Map<Integer, Scope> parent2childScopes = new HashMap<Integer, Scope>();
    private Map<Integer, Boolean> scopeActivationState = new HashMap<Integer, Boolean>();

    public DialogState(Dialog dialog, StatementContext parentContext) {
        this.dialog = dialog;
        this.externalContext = parentContext;
        this.scope2context = new HashMap<Integer, MutableContext>();

        resetActivation();
    }

    public void resetActivation() {

        parent2childScopes.clear();

        DefaultActivation activation = new DefaultActivation();
        dialog.getInterfaceModel().accept(activation);
        for(QName unitId : activation.getActiveItems().values())
        {
            Scope scope = getScope(unitId);
            scopeActivationState.put(scope.getScopeId(), true);
            parent2childScopes.put(getParentScopeId(unitId), scope);
        }
    }

    public void clearStatement(QName sourceId, String key) {
        ((MutableContext)getContext(sourceId)).clearStatement(key);
    }

    public void setStatement(QName interactionUnitId, String key, String value) {
        MutableContext context = (MutableContext) getContext(interactionUnitId);
        assert context!=null : "No context for " + interactionUnitId;

        System.out.println(">> Set '"+key+"' on scope ["+context.getScope().getScopeId()+"]: "+value);
        context.setStatement(key, value);
    }

    public StatementContext getContext(QName interactionUnitId) {

        final Node<Scope> self = dialog.getScopeModel().findNode(interactionUnitId);
        assert self!=null : "Unit not present in shim: "+ interactionUnitId;

        Scope scope = self.getData();

        // lazy initialisation
        if(!scope2context.containsKey(scope.getScopeId()))
        {

            // extract parent scopes

            List<Node<Scope>> parentScopeNodes = self.collectParents(new NodePredicate<Scope>() {
                Set<Integer> tracked = new HashSet<Integer>();

                @Override
                public boolean appliesTo(Node<Scope> candidate) {
                    if (self.getData().getScopeId() != candidate.getData().getScopeId()) {
                        if (!tracked.contains(candidate.getData().getScopeId())) {
                            tracked.add(candidate.getData().getScopeId());
                            return true;
                        }

                        return false;
                    }

                    return false;
                }
            });

            // delegation scheme
            List<Integer> parentScopeIds = new LinkedList<Integer>();
            for(Node<Scope> parentNode : parentScopeNodes)
            {
                parentScopeIds.add(parentNode.getData().getScopeId());
            }

            scope2context.put(scope.getScopeId(),
                    new ParentDelegationContextImpl(scope, externalContext, parentScopeIds,
                            new Scopes() {
                                @Override
                                public StatementContext get(Integer scopeId) {
                                    return scope2context.get(scopeId);
                                }
                            })
            );
        }

        return scope2context.get(scope.getScopeId());
    }

    public void activateScope(QName targetUnit) {

        Scope nextScope = getScope(targetUnit);
        int parentScopeId = getParentScopeId(targetUnit);

        Scope activeScope = parent2childScopes.get(parentScopeId);
        if(activeScope!=null && !activeScope.equals(nextScope))    // TODO: can be null for root elements
        {
            System.out.println("Replace activation of scope "+activeScope+" with "+ nextScope);
            scopeActivationState.put(activeScope.getScopeId(), false);
            scopeActivationState.put(nextScope.getScopeId(), true);
            parent2childScopes.put(parentScopeId, nextScope);
        }
    }

    /**
     * A unit can be activated if the parent is a demarcation type
     * or it is a non demarcating root element
     *
     * @param interactionUnit
     * @return
     */
    public boolean canBeActivated(QName interactionUnit) {

        Node<Scope> node = dialog.getScopeModel().findNode(interactionUnit);
        assert node!=null : "Unit doesn't exist in shim: "+interactionUnit;
        boolean isRootElement = node.getParent() == null;
        boolean parentIsDemarcationType = node.getParent()!=null && node.getParent().getData().isDemarcationType();
        return isRootElement || parentIsDemarcationType;
    }

    public boolean isWithinActiveScope(final QName unitId) {
        final Scope scopeOfUnit = getScope(unitId);
        int parentScopeId = getParentScopeId(unitId);
        Scope activeScope = parent2childScopes.get(parentScopeId);
        boolean selfIsActive = activeScope != null && activeScope.equals(scopeOfUnit);

        if(selfIsActive) // only verify parents if necessary
        {
            Node<Scope> inactiveParentScope = dialog.getScopeModel().findNode(unitId).findParent(
                    new InActiveParentPredicate(unitId)
            );

            return inactiveParentScope==null;
        }

        return selfIsActive;
    }

    private int getParentScopeId(QName targetUnit) {
        final Scope scope = getScope(targetUnit);
        Node<Scope> parent = dialog.getScopeModel().findNode(targetUnit).findParent(new NodePredicate<Scope>() {
            @Override
            public boolean appliesTo(Node<Scope> node) {
                return node.getData().getScopeId() != scope.getScopeId();
            }
        });

        // fallback to root scope if none found
        return parent!= null ?
                parent.getData().getScopeId() : 0;
    }

    private Scope getScope(QName targetUnit) {
        MutableContext context = (MutableContext)getContext(targetUnit);
        assert context!=null : "No context for "+targetUnit;
        return context.getScope();
    }

    interface MutableContext extends StatementContext {
        Scope getScope();
        String get(String key);
        String[] getTuple(String key);
        void setStatement(String key, String value);
        void clearStatement(String key);
    }

    interface Scopes {
        StatementContext get(Integer scopeId);
    }

    class InActiveParentPredicate implements NodePredicate<Scope> {

        private final Scope scopeOfUnit;
        private QName unitId;

        InActiveParentPredicate(QName unitId) {
            this.unitId = unitId;
            this.scopeOfUnit = getScope(unitId);
        }

        @Override
        public boolean appliesTo(Node<Scope> node) {

            Scope parentScope = node.getData();
            boolean isParent = parentScope.getScopeId() != scopeOfUnit.getScopeId();
            boolean isActive = scopeActivationState.get(parentScope.getScopeId())!=null
                    ? scopeActivationState.get(parentScope.getScopeId()) : false;

            if(!isActive)
            {
                System.out.println("Inactive parent scope: "+ parentScope.getScopeId() +" for "+ unitId);
            }

            return isParent && !isActive;
        }
    }
}
