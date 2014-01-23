package org.jboss.as.console.client.plugins;

import java.util.Set;

/**
 * Registry for presenters annotated with {@link org.jboss.as.console.spi.AccessControl} and {@link
 * org.jboss.as.console.spi.SearchIndex}.
 *
 * @author Harad Pehl
 */
public interface SearchIndexRegistry {

    /**
     * Returns the tokens for the given operation mode which are not excluded by
     * {@link org.jboss.as.console.spi.SearchIndex#exclude()} are not included.
     *
     * @param scope the execution mode - must not be null
     *
     * @return a set of matching tokens
     */
    public Set<String> getTokens(OperationMode scope);

    public Set<String> getResources(String token);

    public Set<String> getKeywords(String token);
}
