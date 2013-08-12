package org.jboss.as.console.client.rbac;

/**
 * Resolves security contexts at runtime.
 * @author Heiko Braun
 * @date 8/12/13
 */
public interface ContextKeyResolver {
    String resolveKey();
}
