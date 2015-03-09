package org.jboss.as.console.client.widgets.nav.v3;

/**
 * @author Heiko Braun
 * @since 09/03/15
 */
public interface ValueProvider<T> {
    String get(T item);
}
