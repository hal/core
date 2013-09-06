/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.console.client.administration.role.model;

import static org.jboss.as.console.client.administration.role.model.Principal.Type.GROUP;
import static org.jboss.as.console.client.administration.role.model.Principal.Type.USER;
import static org.jboss.as.console.client.administration.role.model.RoleAssignment.PrincipalRealmTupel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Harald Pehl
 */
public class RoleAssignments {

    private final Map<PrincipalRealmTupel, RoleAssignment> lookup;
    private final List<RoleAssignment.Internal> models;
    private final Map<Principal.Type, Set<RoleAssignment>> assignments;

    public RoleAssignments() {
        this.lookup = new HashMap<PrincipalRealmTupel, RoleAssignment>();
        this.models = new ArrayList<RoleAssignment.Internal>();
        this.assignments = new HashMap<Principal.Type, Set<RoleAssignment>>();
        this.assignments.put(GROUP, new HashSet<RoleAssignment>());
        this.assignments.put(USER, new HashSet<RoleAssignment>());
    }

    public void add(RoleAssignment.Internal internal) {
        models.add(internal);
    }

    public Set<RoleAssignment> getGroupAssignments() {
        return assignments.get(GROUP);
    }

    public Set<RoleAssignment> getUserAssignments() {
        return assignments.get(USER);
    }

    public void toUI(Principals principals) {
        // The UI model is based on principals, so iterate over all known principals and find the relevant assignments
        for (Principal principal : principals) {
            for (RoleAssignment.Internal internal : models) {
                for (PrincipalRealmTupel include : internal.getIncludes()) {
                    if (include.principal.equals(principal)) {
                        RoleAssignment roleAssignment = new RoleAssignment(include.principal);
                        roleAssignment.setRealm(include.realm);
                        roleAssignment.addRole(internal.getRole());
                        add(roleAssignment);
                    }
                }
                for (PrincipalRealmTupel exclude : internal.getExcludes()) {
                    if (exclude.principal.equals(principal)) {
                        RoleAssignment roleAssignment = new RoleAssignment(exclude.principal);
                        roleAssignment.setRealm(exclude.realm);
                        roleAssignment.addExclude(internal.getRole());
                        add(roleAssignment);
                    }
                }
            }
        }
    }

    private void add(RoleAssignment newAssignment) {
        PrincipalRealmTupel key = new PrincipalRealmTupel(newAssignment.getPrincipal(), newAssignment.getRealm());
        RoleAssignment existingAssignment = lookup.get(key);
        if (existingAssignment == null) {
            lookup.put(key, newAssignment);
            Set<RoleAssignment> byType = assignments.get(newAssignment.getPrincipal().getType());
            byType.add(newAssignment);
        } else {
            existingAssignment.addRoles(newAssignment.getRoles());
            existingAssignment.addExcludes(newAssignment.getExcludes());
        }
    }
}
