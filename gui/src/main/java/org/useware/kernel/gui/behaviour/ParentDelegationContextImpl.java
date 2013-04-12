package org.useware.kernel.gui.behaviour;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Heiko Braun
 * @date 3/20/13
 */
class ParentDelegationContextImpl implements StatementScope.MutableContext{

    Map<String,String> delegate = new HashMap<String,String>();
    private final List<Integer> parentScopeIds;
    private final StatementContext externalContext;
    private final StatementScope.Scopes availableScopes;
    private final Integer scopeId;

    public ParentDelegationContextImpl(Integer scopeId, StatementContext externalContext, List<Integer> parentScopeIds, StatementScope.Scopes scopes) {
        this.externalContext = externalContext;
        this.parentScopeIds = parentScopeIds;
        this.availableScopes = scopes;
        this.scopeId = scopeId;
    }

    @Override
    public Integer getScopeId() {
        return scopeId;
    }

    @Override
    public String get(String key) {
        return delegate.get(key);
    }

    @Override
    public String[] getTuple(String key) {
        return null;
    }

    @Override
    public void setStatement(String key, String value) {
        delegate.put(key, value);
    }

    @Override
    public void clearStatement(String key) {
        delegate.remove(key);
    }

    @Override
    public String resolve(String key) {
        String resolvedValue = null;

        // local
        resolvedValue = get(key);

        // iterate delegates
        Iterator<Integer> delegateIds = parentScopeIds.iterator();
        while(null==resolvedValue && delegateIds.hasNext())
        {
            StatementContext delegationContext = availableScopes.get(delegateIds.next());
            if(delegationContext!=null) // may not be created yet, aka unused
            {
                resolvedValue = delegationContext.resolve(key);
            }
        }

        // last but not least: external context
        return resolvedValue == null ? externalContext.resolve(key) : resolvedValue;
    }

    @Override
    public String[] resolveTuple(String key) {
        String[] resolvedTuple = null;

        // local
        resolvedTuple = getTuple(key);

        // iterate delegates
        Iterator<Integer> delegateIds = parentScopeIds.iterator();
        while(null==resolvedTuple && delegateIds.hasNext())
        {
            StatementContext delegationContext = availableScopes.get(delegateIds.next());
            if(delegationContext!=null) // may not be created yet, aka unused
            {
                resolvedTuple = delegationContext.resolveTuple(key);
            }
        }

        // last but not least: external context
        return resolvedTuple == null ? externalContext.resolveTuple(key) : resolvedTuple;
    }

    @Override
    public LinkedList<String> collect(String key) {
        LinkedList<String> resolvedValues = new LinkedList<String>();

        // local
        if(get(key) !=null)
            resolvedValues.add(get(key));

        // iterate delegates
        Iterator<Integer> delegateIds = parentScopeIds.iterator();
        while(delegateIds.hasNext())
        {
            StatementContext delegationContext = availableScopes.get(delegateIds.next());
            if(delegationContext!=null && delegationContext.get(key)!=null) // may not be created yet, aka unused
            {
                resolvedValues.add(delegationContext.get(key));
            }
        }

        // last but not least: external context
        LinkedList<String> external = externalContext.collect(key);
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
        Iterator<Integer> delegateIds = parentScopeIds.iterator();
        while(delegateIds.hasNext())
        {
            StatementContext delegationContext = availableScopes.get(delegateIds.next());
            if(delegationContext!=null && delegationContext.getTuple(key)!=null) // may not be created yet, aka unused
            {
                resolvedTuple.add(delegationContext.getTuple(key));
            }
        }

        // last but not least: external context
        LinkedList<String[]> external = externalContext.collectTuples(key);
        if(external!=null) resolvedTuple.addAll(external);

        return resolvedTuple;
    }

}
