package org.jboss.as.console.spi;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Access control meta data for dialogs (presenter).
 *
 * @author Heiko Braun
 * @date 3/26/12
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface RequiredResources {

    /**
     * Set of required resource to operate on (addressable privilege) within the dialog
     */
    String[] resources();

    /**
     * Set of required operations (execution privileges) upon initialisation of the dialog
     */
    String[] operations() default {};

    /**
     * Recursively parse child resources
     */
    boolean recursive() default true;
}
