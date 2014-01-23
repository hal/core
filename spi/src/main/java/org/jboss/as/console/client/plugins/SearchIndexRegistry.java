package org.jboss.as.console.client.plugins;

import java.util.Set;

/**
 * Registry for presenters annotated with {@link org.jboss.as.console.spi.AccessControl} and {@link
 * SearchIndex}.
 *
 * @author Harad Pehl
 */
public interface SearchIndexRegistry {

    /**
     * Returns the tokens for the given operation mode which are not excluded by
     * {@link SearchIndex#exclude()} are not included.
     *
     * @param standalone the execution mode
     *
     * @return a set of matching tokens
     */
    public Set<String> getTokens(boolean standalone);

    public Set<String> getResources(String token);

    public Set<String> getKeywords(String token);
}
