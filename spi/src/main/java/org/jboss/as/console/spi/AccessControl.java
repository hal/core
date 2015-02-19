package org.jboss.as.console.spi;

/**
 * Access control meta data for dialogs (presenter).
 *
 * @deprecated Replace with {@link org.jboss.as.console.spi.RequiredResources}
 * @author Heiko Braun
 * @date 3/26/12
 */
@Deprecated
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE})
public @interface AccessControl {

    /**
     * Set of required resource to operate on (addressable privilege) within the dialog
     * @return
     */
    String[] resources();

    /**
     * Set of required operations (execution privileges) upon initialisation of the dialog
     * @return
     */
    String[] operations() default {};

    /**
     * Recursively parse child resources
     *
     * @return
     */
    boolean recursive() default true;
}
