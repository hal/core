package org.jboss.as.console.client.shared.subsys.jgroups;

import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.widgets.forms.Binding;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 2/16/12
 */
public interface JGroupsProtocol {

    @Binding(skip = true)
    String getName();
    void setName(String name);

    @Binding(detypedName = "socket-binding")
    String getSocketBinding();
    void setSocketBinding(String socketBinding);

    @Deprecated
    @Binding(skip = true)
    String getType();
    void setType(String type);

    @Binding(skip = true)
    List<PropertyRecord> getProperties();
    void setProperties(List<PropertyRecord> properties);
}
