package org.jboss.as.console.client.core.bootstrap.hal;

import com.google.inject.Inject;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.plugins.SubsystemRegistry;
import org.jboss.as.console.client.shared.SubsystemMetaData;
import org.jboss.gwt.flow.client.Control;

/**
 * @author Heiko Braun
 * @date 3/27/12
 */
public class RegisterSubsystems implements BootstrapStep {

    private final SubsystemRegistry registry;

    @Inject
    public RegisterSubsystems(SubsystemRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void execute(Control<BootstrapContext> control) {
        SubsystemMetaData.bootstrap(registry);
        control.proceed();
    }
}
