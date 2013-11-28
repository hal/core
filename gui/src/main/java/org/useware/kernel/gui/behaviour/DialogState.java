package org.useware.kernel.gui.behaviour;

import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.mapping.Node;
import org.useware.kernel.model.scopes.BranchActivation;
import org.useware.kernel.model.scopes.Scope;
import org.useware.kernel.model.structure.Container;
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

    // hides the interaction coordinator implementation
    private final StateCoordination stateCoordination;

    private Dialog dialog;

    // external context (delegation)
    private final StatementContext externalContext;

    // the actual statement context instances, currently in-memory
    private Map<Integer, MutableContext> statementContexts;

    // maps parent scopes to child scopes: only one child can be active at a time
    // this is a result of the current scope assignment policies
    private Map<Integer, Scope> activeChildMapping;

    public DialogState(Dialog dialog, StatementContext parentContext, StateCoordination coordination) {
        this.stateCoordination = coordination;
        this.dialog = dialog;
        this.externalContext = parentContext;
        this.activeChildMapping = new HashMap<Integer, Scope>();
        this.statementContexts = new HashMap<Integer, MutableContext>();
    }

    public void reset() {

        activeChildMapping.clear();
        dialog.getScopeModel().clearActivation();
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
        int parentScopeId = getParentScope(targetUnit).getId();

        Scope activeScope = activeChildMapping.get(parentScopeId);
        if(!nextScope.equals(activeScope))
        {
            //System.out.println("Replace activation of scope "+activeScope+" with "+ nextScope);

            // root element might not have an active scope
            if(activeScope!=null)
                activeScope.setActive(false);

            nextScope.setActive(true);
            activeChildMapping.put(parentScopeId, nextScope);

        }
    }

    /**
     * Flush the scope of unit and all children.
     *
     * @param unitId
     */
    public void flushChildScopes(QName unitId) {
        Set<Integer> childScopes = findChildScopes(unitId);
        for(Integer scopeId : childScopes)
        {
            MutableContext mutableContext = statementContexts.get(scopeId);
            mutableContext.clearStatements();
        }
    }

    private Set<Integer> findChildScopes(QName unitId)
    {

        Node<Scope> root = dialog.getScopeModel().findNode(
                dialog.findUnit(unitId).getScopeId()
        );
        assert root!=null : "Unit not present in scopeModel: "+ unitId;

        Node<Scope> parent = root.getParent();
        assert parent!=null : "No parent scope for : "+ unitId;

        Set<Integer> childScopes = new HashSet<Integer>();
        collectChildScopes(parent, childScopes);

        return childScopes;
    }

    private void collectChildScopes(Node<Scope> parent, Set<Integer> scopes)
    {
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

    public StatementContext getContext(QName unitId) {

        final Node<Scope> self = dialog.getScopeModel().findNode(
                dialog.findUnit(unitId).getScopeId()
        );
        assert self!=null : "Unit not present in scopeModel: "+ unitId;

        Scope scope = self.getData();

        // lazy initialisation
        if(!statementContexts.containsKey(scope.getId()))
        {

            // extract parent scopes
            LinkedList<Scope> parentScopes = new LinkedList<Scope>();
            getParentScopes(self, parentScopes);


            statementContexts.put(scope.getId(),
                    new ParentDelegationContextImpl(
                            scope, parentScopes,
                            new StateManagement() {
                                @Override
                                public StatementContext get(Integer scopeId) {
                                    return statementContexts.get(scopeId);
                                }

                                @Override
                                public StatementContext getExternal() {
                                    return externalContext;
                                }
                            })
            );
        }

        return statementContexts.get(scope.getId());
    }

    private void getParentScopes(Node<Scope> self, LinkedList<Scope> scopes)
    {

        Node<Scope> parent = self.getParent();
        if(parent!=null)
        {
            scopes.add(parent.getData());
            getParentScopes(parent, scopes);
        }
    }

    /**
     * A unit can be activated if the parent is a demarcation type
     * or it is a root element
     *
     * @param interactionUnit
     * @return
     */
    public boolean canBeActivated(QName interactionUnit) {

        return dialog.findUnit(interactionUnit) instanceof Container;
        /*Node<Scope> node = dialog.getScopeModel().findNode(
                dialog.findUnit(interactionUnit).getScopeId()
        );
        assert node!=null : "Unit doesn't exist in scopeModel: "+interactionUnit;

        boolean isRootElement = node.getData().getId() == 0;
        boolean parentIsDemarcationType = node.getParent()!=null && node.getParent().getData().isDemarcationType();
        return isRootElement || parentIsDemarcationType;*/
    }

    /**
     * Is within active scope when itself and all it's parent scopes are active as well.
     * This is necessary to ensure access to statements from parent scopes.
     *
     * @param unitId
     * @return
     */
    public boolean isWithinActiveScope(final QName unitId) {

        final Node<Scope> self = dialog.getScopeModel().findNode(
                dialog.findUnit(unitId).getScopeId()
        );
        final Scope scopeOfUnit = self.getData();
        int parentScopeId = getParentScope(unitId).getId();

        Scope activeScope = activeChildMapping.get(parentScopeId);
        boolean selfIsActive = activeScope != null && activeScope.equals(scopeOfUnit);

        if(selfIsActive) // only verify parents if necessary
        {
            LinkedList<Scope> parentScopes = new LinkedList<Scope>();
            getParentScopes(self, parentScopes);

            boolean inActiveParent = false;
            for(Scope parent : parentScopes)
            {
                if(!parent.isActive())
                {
                    inActiveParent = true;
                    break;
                }
            }
            return inActiveParent;
        }

        return selfIsActive;
    }

    private Scope getParentScope(QName targetUnit) {

        final Integer id = dialog.findUnit(targetUnit).getScopeId();

        return dialog.getScopeModel()
                .findNode(id)
                .getParent().getData();

    }

    private Scope getScope(QName unitId) {

        final Integer id = dialog.findUnit(unitId).getScopeId();

        return dialog.getScopeModel()
                .findNode(id)
                .getData();

    }

    interface MutableContext extends StatementContext {
        Scope getScope();
        String get(String key);
        String[] getTuple(String key);
        void setStatement(String key, String value);
        void clearStatement(String key);
        void clearStatements();
    }

    interface StateManagement {
        StatementContext get(Integer scopeId);
        StatementContext getExternal();
    }

}
