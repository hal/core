package org.jboss.as.console.client.shared.subsys.jmx.model;

import org.jboss.as.console.client.widgets.forms.Address;
import org.jboss.as.console.client.widgets.forms.Binding;

/**
 * @author Heiko Braun
 * @date 11/28/11
 */
@Address("/subsystem=jmx")
public interface JMXSubsystem {

    @Binding(detypedName = "non-core-mbean-sensitivity")
    boolean isSensitive();
    void setSensitive(boolean sensitive);


    @Binding(detypedName = "use-management-endpoint")
    boolean isMgmtEndpoint();
    void setMgmtEndpoint(boolean useMgmtEndpint);

    @Binding(detypedName = "show-model")
    boolean isShowModel();
    void setShowModel(boolean b);

}
