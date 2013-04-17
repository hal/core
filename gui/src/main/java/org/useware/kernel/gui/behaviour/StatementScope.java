package org.useware.kernel.gui.behaviour;

import org.useware.kernel.model.scopes.DefaultActivationVisitor;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.mapping.Node;
import org.useware.kernel.model.mapping.NodePredicate;
import org.useware.kernel.model.scopes.Scope;
import org.useware.kernel.model.structure.QName;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A registry for dialog statements. It reflects the current dialog state.
 *
 * @author Heiko Braun
 * @date 1/22/13
 */
public class StatementScope {

    private Dialog dialog;
    private final StatementContext externalContext;
    private Map<Integer, MutableContext> scope2context;

    /**
     * Maps the active units under their parent (!) scope
     */
    private Map<Integer, QName> activeBelowScope = new HashMap<Integer, QName>();

    public StatementScope(Dialog dialog, StatementContext parentContext) {
        this.dialog = dialog;
        this.externalContext = parentContext;
        this.scope2context = new HashMap<Integer, MutableContext>();

        resetActivation();
    }

    public void resetActivation() {

        activeBelowScope.clear();

        DefaultActivationVisitor activation = new DefaultActivationVisitor();
        dialog.getInterfaceModel().accept(activation);
        for(QName unitId : activation.getActiveItems().values())
        {
            activeBelowScope.put(getParentScopeId(unitId), unitId);
        }
    }

    public void clearStatement(QName sourceId, String key) {
        ((MutableContext)getContext(sourceId)).clearStatement(key);
    }

    public void setStatement(QName interactionUnitId, String key, String value) {
        MutableContext context = (MutableContext) getContext(interactionUnitId);
        assert context!=null : "No context for " + interactionUnitId;

        System.out.println(">> Set '"+key+"' on scope ["+context.getScopeId()+"]: "+value);
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

            scope2context.put(scope.getScopeId(), new ParentDelegationContextImpl(scope.getScopeId(), externalContext, parentScopeIds,
                    new Scopes() {
                        @Override
                        public StatementContext get(Integer scopeId) {
                            return scope2context.get(scopeId);
                        }
                    }));
        }

        return scope2context.get(scope.getScopeId());
    }

    public void activateScope(QName targetUnit) {

        int parentScopeId = getParentScopeId(targetUnit);

        QName currentlyActive = activeBelowScope.get(parentScopeId);

        if(!targetUnit.equals(currentlyActive))
        {
            System.out.println("Replace "+currentlyActive+" with "+ targetUnit);
        }

        activeBelowScope.put(parentScopeId, targetUnit);

    }

    public boolean isWithinActiveScope(QName unitId) {
        QName activeUnit= activeBelowScope.get(getParentScopeId(unitId));
        return activeUnit!=null && activeUnit.equals(unitId);

    }

    private int getParentScopeId(QName targetUnit) {
        final int selfScope = getScopeId(targetUnit);
        Node<Scope> parent = dialog.getScopeModel().findNode(targetUnit).findParent(new NodePredicate<Scope>() {
            @Override
            public boolean appliesTo(Node<Scope> node) {
                return node.getData().getScopeId() != selfScope;
            }
        });

        return parent.getData().getScopeId();
    }

    private int getScopeId(QName targetUnit) {
        MutableContext context = (MutableContext)getContext(targetUnit);
        assert context!=null : "No context for "+targetUnit;
        return context.getScopeId();
    }

    interface MutableContext extends StatementContext {
        Integer getScopeId();
        String get(String key);
        String[] getTuple(String key);
        void setStatement(String key, String value);
        void clearStatement(String key);
    }

    interface Scopes {
        StatementContext get(Integer scopeId);
    }

}
