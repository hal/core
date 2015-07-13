package org.jboss.as.console.client.domain.model;

/**
 * @author Heiko Braun
 * @since 13/07/15
 */
public enum RuntimeState {

    DISABLED,
    STARTING,
    STARTED,
    STOPPING,
    STOPPED,
    FAILED,
    UNKNOWN,
    DOES_NOT_EXIST;

}
