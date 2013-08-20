package org.useware.kernel.gui.behaviour;

import java.util.LinkedList;

/**
 * Intercepts the resolution and allows to filter/replace certain statement values.
 *
 * @author Heiko Braun
 * @date 8/20/13
 */
public class FilteringStatementContext implements StatementContext {

    public interface Filter {
        String filter(String key);
        String[] filterTuple(String key);
    }

    private Filter filter;
    private StatementContext delegate;

    public FilteringStatementContext(StatementContext delegate, Filter filter) {
        this.delegate = delegate;
        this.filter = filter;
    }

    @Override
    public String get(String key) {
        String filtered = filter.filter(key);
        return filtered!=null ? filtered : delegate.get(key);
    }

    @Override
    public String[] getTuple(String key) {
        String[] filtered = filter.filterTuple(key);
        return filtered!=null ? filtered : delegate.getTuple(key);
    }

    @Override
    public String resolve(String key) {
        String filtered = filter.filter(key);
        return filtered!=null ? filtered : delegate.resolve(key);
    }

    @Override
    public String[] resolveTuple(String key) {
        String[] filtered = filter.filterTuple(key);
        return filtered!=null ? filtered : delegate.resolveTuple(key);
    }

    @Override
    public LinkedList<String> collect(String key) {
        LinkedList<String> actualCollection = delegate.collect(key);
        String filtered = filter.filter(key);
        if(filtered!=null)
        {
            LinkedList<String> filteredCollection = new LinkedList<String>();
            for(String val : actualCollection)
            {
                filteredCollection.add(filtered);
            }

            //TODO: this currently enforces a filter to be applied (hack?)
            if(actualCollection.isEmpty())
                filteredCollection.add(filtered);

            return filteredCollection;
        }

        return actualCollection;
    }

    @Override
    public LinkedList<String[]> collectTuples(String key) {
        LinkedList<String[]> actualCollection = delegate.collectTuples(key);
        String[] filtered = filter.filterTuple(key);
        if(filtered!=null)
        {
            LinkedList<String[]> filteredCollection = new LinkedList<String[]>();
            for(String[] val : actualCollection)
            {
                filteredCollection.add(filtered);
            }
            return filteredCollection;
        }

        return actualCollection;
    }
}
