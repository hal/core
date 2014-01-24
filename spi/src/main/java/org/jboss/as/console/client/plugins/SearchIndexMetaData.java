package org.jboss.as.console.client.plugins;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Harald Pehl
 */
public class SearchIndexMetaData {

    private final String token;
    private final boolean standalone;
    private final boolean domain;
    private final Set<String> resources;
    private final Set<String> keywords;

    public SearchIndexMetaData(final String token, final boolean standalone, final boolean domain,
            final String[] resources, final String[] keywords) {
        this.token = token;
        this.standalone = standalone;
        this.domain = domain;
        this.resources = resources == null ? Collections.<String>emptySet() : new HashSet<String>(asList(resources));
        this.keywords = keywords == null ? Collections.<String>emptySet() : new HashSet<String>(asList(keywords));
    }

    public String getToken() {
        return token;
    }

    public boolean isStandalone() {
        return standalone;
    }

    public boolean isDomain() {
        return domain;
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
                ", standalone=" + standalone +
                ", domain=" + domain +
                ", resources=" + resources +
                ", keywords=" + keywords +
                '}';
    }
}
