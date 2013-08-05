package org.jboss.as.console.client.administration.role;


import org.jboss.as.console.client.widgets.forms.Binding;


/**
 * A user or a group inside an include / exclude list in {@link org.jboss.as.console.client.administration.role.RoleAssignment}
 * @author Harald Pehl
 * @date 07/23/2013
 */
public interface Principal {

    String getName();
    void setName(String name);

    String getRealm();
    void setRealm(String realm);

    @Binding(skip = true)
    Type getType();
    void setType(Type type);

    boolean isInclude();
    void setInclude(boolean include);

    enum Type {USER, GROUP}
}
