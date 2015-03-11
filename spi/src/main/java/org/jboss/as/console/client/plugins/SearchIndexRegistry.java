package org.jboss.as.console.client.plugins;

import java.util.Set;

/**
 * Registry for presenters annotated with
 * {@link org.jboss.as.console.spi.RequiredResources} and
 * {@link org.jboss.as.console.spi.SearchIndex}.
 *
 * @author Harad Pehl
 */
public interface SearchIndexRegistry {

    /**
     * Returns the tokens for the given operation mode which are not excluded by
     * {@link org.jboss.as.console.spi.SearchIndex#exclude()} are not included.
     *
     * @param standalone the execution mode
     * @return a set of matching tokens
     */
    Set<String> getTokens(boolean standalone);

    Set<String> getResources(String token);

    Set<String> getKeywords(String token);
}
