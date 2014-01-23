package org.jboss.as.console.client.plugins;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.console.spi.SearchIndex;

/**
 * @author Harald Pehl
 */
public class SearchIndexMetaData {

    private final String token;
    private final Set<SearchIndex.OperationMode> scope;
    private final Set<String> resources;
    private final Set<String> keywords;

    public SearchIndexMetaData(final String token, final SearchIndex.OperationMode[] scope,
            final String[] resources, final String[] keywords) {
        this.token = token;
        if (scope == null) {
            this.scope = EnumSet.allOf(SearchIndex.OperationMode.class);
        } else {
            this.scope = new HashSet<SearchIndex.OperationMode>();
            for (SearchIndex.OperationMode operationMode : scope) {
                this.scope.add(operationMode);
            }
        }
        this.resources = resources == null ? Collections.<String>emptySet() : new HashSet<String>(asList(resources));
        this.keywords = keywords == null ? Collections.<String>emptySet() : new HashSet<String>(asList(keywords));
    }

    public String getToken() {
        return token;
    }

    public Set<SearchIndex.OperationMode> getScope() {
        return scope;
    }

    public Set<String> getResources() {
        return resources;
    }

    public Set<String> getKeywords() {
        return keywords;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (!(o instanceof SearchIndexMetaData)) { return false; }

        SearchIndexMetaData that = (SearchIndexMetaData) o;

        if (token != null ? !token.equals(that.token) : that.token != null) { return false; }

        return true;
    }

    @Override
    public int hashCode() {
        return token != null ? token.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "SearchIndexMetaData{" +
                "token='" + token + '\'' +
                ", scope=" + scope +
                ", resources=" + resources +
                ", keywords=" + keywords +
                '}';
    }
}
