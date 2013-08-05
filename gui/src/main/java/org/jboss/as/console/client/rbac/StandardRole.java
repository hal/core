package org.jboss.as.console.client.rbac;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Heiko Braun
 * @date 7/12/13
 */
public enum StandardRole {

    MONITOR(),
    OPERATOR(),
    MAINTAINER(),
    DEPLOYER(),
    ADMINISTRATOR(),
    AUDITOR(),
    SUPERUSER() ;


    public static List<String> getRoleNames() {
        List<String> names = new ArrayList<String>();

        for (StandardRole role : StandardRole.values()) {
            names.add(role.name().toUpperCase());
        }

        return names;
    }
}

