package org.jboss.as.console.spi;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.jboss.as.console.client.plugins.RuntimeGroup.METRICS;

/**
 * @author Heiko Braun
 * @date 3/26/12
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface RuntimeExtension {

    String name();

    String group() default METRICS;

    /**
     * DMR name of the subsystem
     */
    String key();
}
