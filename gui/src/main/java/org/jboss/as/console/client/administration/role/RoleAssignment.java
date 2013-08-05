package org.jboss.as.console.client.administration.role;

import java.util.List;


/**
 * @author Harald Pehl
 * @date 07/23/2013
 */
public interface RoleAssignment {

    List<Principal> getPrincipals();
    void setPrincipals(List<Principal> principals);

    boolean isInclude();
    void setInclude(boolean include);
}
