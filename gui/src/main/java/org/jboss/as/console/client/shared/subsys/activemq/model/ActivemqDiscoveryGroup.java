package org.jboss.as.console.client.shared.subsys.activemq.model;

import org.jboss.as.console.client.widgets.forms.Binding;

/**
 * @author Heiko Braun
 * @date 4/18/12
 */
public interface ActivemqDiscoveryGroup {

    @Binding(skip = true)
    String getName();
    void setName(String name);

    @Binding(detypedName = "initial-wait-timeout")
    Long getInitialWaitTimeout();
    void setInitialWaitTimeout(Long timeout);

    @Binding(detypedName = "refresh-timeout")
    Long getRefreshTimeout();
    void setRefreshTimeout(Long timeout);

    @Binding(detypedName = "socket-binding")
    String getSocketBinding();
    void setSocketBinding(String bidning);
}
