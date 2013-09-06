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

import static org.jboss.as.console.client.administration.role.model.PrincipalType.GROUP;
import static org.jboss.as.console.client.administration.role.model.PrincipalType.USER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.console.client.rbac.Role;
import org.jboss.as.console.client.shared.BeanFactory;

/**
 * @author Harald Pehl
 */
public class RoleAssignments {

    private final BeanFactory beanFactory;
    private final Map<String, RoleAssignment> lookup;
    private final List<RoleAssignment.ManagementModel> models;
    private final Map<PrincipalType, List<RoleAssignment>> assignments;

    public RoleAssignments(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        this.lookup = new HashMap<String, RoleAssignment>();
        this.models = new ArrayList<RoleAssignment.ManagementModel>();
        this.assignments = new HashMap<PrincipalType, List<RoleAssignment>>();
        this.assignments.put(GROUP, new ArrayList<RoleAssignment>());
        this.assignments.put(USER, new ArrayList<RoleAssignment>());
    }

    public void add(RoleAssignment.ManagementModel managementModel) {
        models.add(managementModel);
    }

    public List<RoleAssignment> getGroupAssignments() {
        return assignments.get(GROUP);
    }

    public List<RoleAssignment> getUserAssignments() {
        return assignments.get(USER);
    }

    public void toUI(Principals principals) {
        // The UI model is based on principals, so iterate over all known principals and find the relevant assignments
        for (Principal principal : principals) {
            for (RoleAssignment.ManagementModel managementModel : models) {
                for (Principal include : managementModel.getIncludes()) {
                    if (include.getName().equals(principal.getName())) {
                        RoleAssignment roleAssignment = newRoleAssignment(include);
                        roleAssignment.getRoles().add(managementModel.getRole());
                        add(roleAssignment);
                    }
                }
                for (Principal exclude : managementModel.getExcludes()) {
                    if (exclude.getName().equals(principal.getName())) {
                        RoleAssignment roleAssignment = newRoleAssignment(exclude);
                        roleAssignment.getExcludes().add(managementModel.getRole());
                        add(roleAssignment);
                    }
                }
            }
        }
    }

    private RoleAssignment newRoleAssignment(final Principal principal) {
        RoleAssignment roleAssignment = beanFactory.roleAssignment().as();
        roleAssignment.setId(ModelHelper.principalIdentifier(principal));
        roleAssignment.setPrincipal(principal);
        roleAssignment.setRoles(new ArrayList<Role>());
        roleAssignment.setExcludes(new ArrayList<Role>());
        return roleAssignment;
    }

    private void add(RoleAssignment newAssignment) {
        RoleAssignment existingAssignment = lookup.get(newAssignment.getId());
        if (existingAssignment == null) {
            lookup.put(newAssignment.getId(), newAssignment);
            List<RoleAssignment> list = assignments.get(newAssignment.getPrincipal().getType());
            list.add(newAssignment);
        } else {
            List<Role> existingRoles = existingAssignment.getRoles();
            List<Role> newRoles = newAssignment.getRoles();
            mergeRoles(existingRoles, newRoles);

            List<Role> existingExcludes = existingAssignment.getExcludes();
            List<Role> newExcludes = newAssignment.getExcludes();
            mergeRoles(existingExcludes, newExcludes);
        }
    }

    private void mergeRoles(final List<Role> existingRoles, final List<Role> newRoles) {
        for (Role newRole : newRoles) {
            boolean addRole = true;
            for (Role existingRole : existingRoles) {
                if (existingRole.getName().equals(newRole.getName())) {
                    addRole = false;
                    break;
                }
            }
            if (addRole) {
                existingRoles.add(newRole);
            }
        }
    }
}
