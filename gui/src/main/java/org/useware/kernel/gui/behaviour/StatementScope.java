package org.useware.kernel.gui.behaviour;

import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.mapping.Node;
import org.useware.kernel.model.mapping.NodePredicate;
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
    private int activeScope = -1;

    public StatementScope(Dialog dialog, StatementContext parentContext) {
        this.dialog = dialog;
        this.externalContext = parentContext;
        this.scope2context = new HashMap<Integer, MutableContext>();
    }

    public void clearStatement(QName sourceId, String key) {
        ((MutableContext)getContext(sourceId)).clearStatement(key);
    }

    public void setStatement(QName interactionUnitId, String key, String value) {
        MutableContext context = (MutableContext) getContext(interactionUnitId);

        System.out.println(">> Set '"+key+"' on scope ["+context.getScopeId()+"]: "+value);
        context.setStatement(key, value);
    }

    public StatementContext getContext(QName interactionUnitId) {


        final Node<Integer> self = dialog.getScopeModel().findNode(interactionUnitId);
        assert self!=null : "Unit not present in shim: "+ interactionUnitId;

        Integer scope = self.getData();

        // lazy initialisation
        if(!scope2context.containsKey(scope))
        {

            // extract parent scopes

            List<Node<Integer>> parentScopeNodes = self.collectParents(new NodePredicate<Integer>() {
                Set<Integer> tracked = new HashSet<Integer>();

                @Override
                public boolean appliesTo(Node<Integer> candidate) {
                    if (self.getData() != candidate.getData()) {
                        if (!tracked.contains(candidate.getData())) {
                            tracked.add(candidate.getData());
                            return true;
                        }

                        return false;
                    }

                    return false;
                }
            });

            // delegation scheme
            List<Integer> parentScopeIds = new LinkedList<Integer>();
            for(Node<Integer> parentNode : parentScopeNodes)
            {
                parentScopeIds.add(parentNode.getData());
            }

            scope2context.put(scope, new ParentDelegationContextImpl(scope, externalContext, parentScopeIds,
                    new Scopes() {
                        @Override
                        public StatementContext get(Integer scopeId) {
                            return scope2context.get(scopeId);
                        }
                    }));
        }

        return scope2context.get(scope);
    }

    public void activateScope(QName targetUnit) {
        MutableContext context = (MutableContext)getContext(targetUnit);
        this.activeScope = context.getScopeId();

        //System.out.println("<< active scope "+this.activeScope+" >>");
    }

    /**
     * Check if the scope local to the unit is active.
     *
     * @param unitId
     * @return
     */
    public boolean isWithinActiveScope(QName unitId) {

        // scope local to unit
        MutableContext context = (MutableContext)getContext(unitId);
        return activeScope == context.getScopeId();
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
