package org.jboss.as.console.client.widgets.nav.v3;

/**
 * @author Heiko Braun
 * @since 19/01/15
 */
public interface ContextualCommand<T> {
    void executeOn(T item);
}