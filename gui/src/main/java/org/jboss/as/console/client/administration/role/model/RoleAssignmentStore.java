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
public class RoleAssignmentStore {

    private final BeanFactory beanFactory;
    private final Map<String, RoleAssignment> lookup;
    private final List<RoleAssignment.ManagementModel> models;
    private final Map<PrincipalType, List<RoleAssignment>> assignments;

    public RoleAssignmentStore(BeanFactory beanFactory) {
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

    public void add(RoleAssignment roleAssignment) {
        if (roleAssignment != null && roleAssignment.getPrincipal() != null) {
            RoleAssignment existingAssignment = lookup.get(roleAssignment.getId());
            if (existingAssignment == null) {
                lookup.put(roleAssignment.getId(), roleAssignment);
                List<RoleAssignment> list = assignments.get(roleAssignment.getPrincipal().getType());
                list.add(roleAssignment);
            } else {
                existingAssignment.getRoles().addAll(roleAssignment.getRoles());
                if (roleAssignment.getExcludes() != null) {
                    if (existingAssignment.getExcludes() == null) {
                        existingAssignment.setExcludes(new ArrayList<Principal>());
                    }
                    existingAssignment.getExcludes().addAll(roleAssignment.getExcludes());
                }
            }
        }
    }

    public void clear() {
        lookup.clear();
        models.clear();
        assignments.get(GROUP).clear();
        assignments.get(USER).clear();
    }

    public List<RoleAssignment> getGroupAssignments() {
        return assignments.get(GROUP);
    }

    public List<RoleAssignment> getUserAssignments() {
        return assignments.get(USER);
    }

    public void transform(PrincipalStore principals) {
        // The UI model is based on principals, so iterate over all known principals and find the relevant assignments
        for (Principal principal : principals) {
            for (RoleAssignment.ManagementModel managementModel : models) {
                for (Principal include : managementModel.getIncludes()) {
                    if (include.getName().equals(principal.getName())) {
                        StringBuilder id = new StringBuilder();
                        id.append(include.getType().name().toLowerCase()).append("-").append(include.getName());
                        if (include.getRealm() != null) {
                            id.append("@").append(include.getRealm());
                        }
                        RoleAssignment roleAssignment = beanFactory.roleAssignment().as();
                        roleAssignment.setId(id.toString());
                        roleAssignment.setPrincipal(include);
                        if (roleAssignment.getRoles() == null) {
                            roleAssignment.setRoles(new ArrayList<Role>());
                        }
                        roleAssignment.getRoles().add(managementModel.getRole());
                        if (principal.getType() == GROUP) {
                            for (Principal exclude : managementModel.getExcludes()) {
                                if (exclude.getType() == USER) {
                                    if (roleAssignment.getExcludes() == null) {
                                        roleAssignment.setExcludes(new ArrayList<Principal>());
                                    }
                                    roleAssignment.getExcludes().add(exclude);
                                }
                            }
                        }
                        add(roleAssignment);
                    }
                }
            }
        }
    }
}
