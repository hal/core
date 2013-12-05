package org.useware.kernel.gui.behaviour;

import org.useware.kernel.model.scopes.Scope;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

/**
 *
 * Parent delegation for statement contexts.
 *
 * @author Heiko Braun
 * @date 3/20/13
 */
class ParentDelegationContextImpl implements DialogState.MutableContext{

    private final DialogState.StateManagement stateManager;
    private Map<String,String> statements = new HashMap<String,String>();

    private final Scope scope;
    private final LinkedList<Scope> parents;

    public ParentDelegationContextImpl(
            Scope scope, LinkedList<Scope> parents,
            DialogState.StateManagement stateManager) {
        this.scope = scope;
        this.parents = parents;
        this.stateManager = stateManager;
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public String get(String key) {
        return statements.get(key);
    }

    @Override
    public String[] getTuple(String key) {
        return null;
    }

    @Override
    public void setStatement(String key, String value) {
        statements.put(key, value);
    }

    @Override
    public void clearStatement(String key) {
        statements.remove(key);
    }

    @Override
    public String resolve(String key) {
        String resolvedValue = null;

        // local
        resolvedValue = get(key);

        // iterate delegates
        Iterator<Scope> delegates = parents.iterator();
        while(null==resolvedValue && delegates.hasNext())
        {
            StatementContext delegationContext = stateManager.get(delegates.next().getId());
            if(delegationContext!=null) // may not be created yet, aka unused
            {
                resolvedValue = delegationContext.resolve(key);
            }
        }

        // last but not least: external context
        return resolvedValue == null ? stateManager.getExternal().resolve(key) : resolvedValue;
    }

    @Override
    public String[] resolveTuple(String key) {
        String[] resolvedTuple = null;

        // local
        resolvedTuple = getTuple(key);

        // iterate delegates
        Iterator<Scope> delegates = parents.iterator();
        while(null==resolvedTuple && delegates.hasNext())
        {
            StatementContext delegationContext = stateManager.get(delegates.next().getId());
            if(delegationContext!=null) // may not be created yet, aka unused
            {
                resolvedTuple = delegationContext.resolveTuple(key);
            }
        }

        // last but not least: external context
        return resolvedTuple == null ? stateManager.getExternal().resolveTuple(key) : resolvedTuple;
    }

    @Override
    public LinkedList<String> collect(String key) {
        LinkedList<String> resolvedValues = new LinkedList<String>();

        // local
        if(get(key) !=null)
            resolvedValues.add(get(key));

        // iterate delegates
        Iterator<Scope> delegates = parents.iterator();
        while(delegates.hasNext())
        {
            StatementContext delegationContext = stateManager.get(delegates.next().getId());
            if(delegationContext!=null && delegationContext.get(key)!=null) // may not be created yet, aka unused
            {
                resolvedValues.add(delegationContext.get(key));
            }
        }

        // last but not least: external context
        LinkedList<String> external = stateManager.getExternal().collect(key);
        if(external!=null) resolvedValues.addAll(external);

        return resolvedValues;
    }

    @Override
    public LinkedList<String[]> collectTuples(String key) {
        LinkedList<String[]> resolvedTuple = new LinkedList<String[]>();

        // local
        if(getTuple(key) !=null)
            resolvedTuple.add(getTuple(key));

        // iterate delegates
        Iterator<Scope> delegates = parents.iterator();
        while(delegates.hasNext())
        {
            StatementContext delegationContext = stateManager.get(delegates.next().getId());
            if(delegationContext!=null && delegationContext.getTuple(key)!=null) // may not be created yet, aka unused
            {
                resolvedTuple.add(delegationContext.getTuple(key));
            }
        }

        // last but not least: external context
        LinkedList<String[]> external = stateManager.getExternal().collectTuples(key);
        if(external!=null) resolvedTuple.addAll(external);

        return resolvedTuple;
    }

    @Override
    public void clearStatements() {
        statements.clear();
    }
}
