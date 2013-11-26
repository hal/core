package org.useware.kernel.gui.behaviour;

import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.mapping.Node;
import org.useware.kernel.model.mapping.NodePredicate;
import org.useware.kernel.model.scopes.BranchActivation;
import org.useware.kernel.model.scopes.Scope;
import org.useware.kernel.model.structure.InteractionUnit;
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

    private final StateCoordination stateCoordination;
    private Dialog dialog;
    private final StatementContext externalContext;
    private Map<Integer, MutableContext> scope2context;
    private Map<Integer, Scope> activeChildMapping = new HashMap<Integer, Scope>();
    private Map<Integer, Boolean> scopeActivationState = new HashMap<Integer, Boolean>();

    public DialogState(Dialog dialog, StatementContext parentContext, StateCoordination coordination) {
        this.stateCoordination = coordination;
        this.dialog = dialog;
        this.externalContext = parentContext;
        this.scope2context = new HashMap<Integer, MutableContext>();
    }

    public void reset() {

        activeChildMapping.clear();
        scopeActivationState.clear();
    }

    /**
     * Activate a branch of the interface model.
     * @param unit the entry point
     *
     * @return the leaf unit of that branch (used for scope activation)
     */
    final static String NONE = "none";
    public QName activateBranch(InteractionUnit unit) {
        return activateBranch(unit, NONE);
    }

    public QName activateBranch(InteractionUnit unit, String suffix) {

        BranchActivation activation = new BranchActivation();
        unit.accept(activation);

        for(QName unitId : activation.getActiveItems().values())
        {
            // trigger activation procedure
            // TODO: Improve passing of relative nav information
            QName target = NONE.equals(suffix) ? unitId :
                    new QName(unitId.getNamespaceURI(), unitId.getLocalPart()+"#"+suffix);

            stateCoordination.activateUnit(target);
        }

        return activation.getActiveItems().get(activation.getActiveItems().size()-1);
    }

    /**
     * Deactivates a previously active sibling scope and activate a new one.
     * (Only a single sibling scope can be active at a time)
     *
     * @param targetUnit the unit from which the scope will be derived
     */
    public void activateScope(QName targetUnit) {

        Scope nextScope = getScope(targetUnit);
        int parentScopeId = getParentScopeId(targetUnit);

        Scope activeScope = activeChildMapping.get(parentScopeId);
        if(!nextScope.equals(activeScope))
        {
            //System.out.println("Replace activation of scope "+activeScope+" with "+ nextScope);

            // root element might not have an active scope
            if(activeScope!=null)
                scopeActivationState.put(activeScope.getScopeId(), false);  // deactivation

            scopeActivationState.put(nextScope.getScopeId(), true); // activation
            activeChildMapping.put(parentScopeId, nextScope);

        }
    }

    public void flushChildScopes(QName unitId) {
        Set<Integer> childScopes = findChildScopes(unitId);
        for(Integer scopeId : childScopes)
        {
            MutableContext mutableContext = scope2context.get(scopeId);
            mutableContext.clearStatements();
        }

    }

    private Set<Integer> findChildScopes(QName unitId)
    {
        Set<Integer> scopes = new HashSet<Integer>();

        Node<Scope> root = dialog.getScopeModel().findNode(unitId);
        assert root!=null : "Unit not present in scopeModel: "+ unitId;

        Node<Scope> parent = root.getParent() !=null ? root.getParent() : root;
        collectChildScopes(parent, scopes);
        scopes.remove(root.getData().getScopeId()); // self reference due to parent resolution

        return scopes;
    }

    private void collectChildScopes(Node<Scope> parent, Set<Integer> scopes)
    {
        int currentScopeId = parent.getData().getScopeId();
        scopes.add(currentScopeId);
        List<Node<Scope>> children = parent.getChildren();

        for(Node<Scope> child : children)
        {
            collectChildScopes(child, scopes);
        }
    }

    public void clearStatement(QName sourceId, String key) {
        ((MutableContext)getContext(sourceId)).clearStatement(key);
    }

    public void setStatement(QName interactionUnitId, String key, String value) {
        MutableContext context = (MutableContext) getContext(interactionUnitId);
        context.setStatement(key, value);
    }

    public StatementContext getContext(QName interactionUnitId) {

        final Node<Scope> self = dialog.getScopeModel().findNode(interactionUnitId);
        assert self!=null : "Unit not present in scopeModel: "+ interactionUnitId;

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



    /**
     * A unit can be activated if the parent is a demarcation type
     * or it is a root element
     *
     * @param interactionUnit
     * @return
     */
    public boolean canBeActivated(QName interactionUnit) {

        Node<Scope> node = dialog.getScopeModel().findNode(interactionUnit);
        assert node!=null : "Unit doesn't exist in scopeModel: "+interactionUnit;
        boolean isRootElement = node.getData().getScopeId() == 0;
        boolean parentIsDemarcationType = node.getParent()!=null && node.getParent().getData().isDemarcationType();
        return isRootElement || parentIsDemarcationType;
    }

    /**
     * Is within active scope when itself and all it's parent scopes are active as well.
     * This is necessary to ensure access to statements from parent scopes.
     *
     * @param unitId
     * @return
     */
    public boolean isWithinActiveScope(final QName unitId) {
        final Scope scopeOfUnit = getScope(unitId);
        int parentScopeId = getParentScopeId(unitId);
        Scope activeScope = activeChildMapping.get(parentScopeId);
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
        void clearStatements();
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

            // skip root scope
            if(node.getData().getScopeId()==0) return false;

            Scope parentScope = node.getData();
            boolean isParent =(parentScope.getScopeId() != scopeOfUnit.getScopeId());
            boolean isActive = scopeActivationState.get(parentScope.getScopeId())!=null
                    ? scopeActivationState.get(parentScope.getScopeId()) : false;

            /*if(!isActive)
            {
                System.out.println("Inactive parent scope: "+ parentScope.getScopeId() +" for "+ unitId);
            } */

            return isParent && !isActive;
        }
    }


}
