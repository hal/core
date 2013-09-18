package org.jboss.as.console.client.shared.subsys.jca.model;

import org.jboss.as.console.client.widgets.forms.Address;
import org.jboss.as.console.client.widgets.forms.Binding;

/**
 * @author Heiko Braun
 * @date 11/29/11
 */
@Address("/subsystem=jca/bootstrap-context={0}")
public interface JcaBootstrapContext {

    @Binding(key=true)
    String getName();
    void setName(String name);

    String getWorkmanager();
    void setWorkmanager(String name);
}
