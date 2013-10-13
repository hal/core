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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jboss.as.console.client.rbac.StandardRole;

/**
 * Contains the list of standard roles plus the custom defined scoped roles.
 *
 * @author Harald Pehl
 */
public class Roles implements Iterable<Role> {

    private final Map<String, Role> lookup;
    private final SortedSet<Role> standardRoles;
    private final SortedSet<Role> scopedRoles;

    public Roles() {
        this.lookup = new HashMap<String, Role>();
        this.standardRoles = new TreeSet<Role>(new RoleComparator());
        this.scopedRoles = new TreeSet<Role>(new RoleComparator());
        for (StandardRole standardRole : StandardRole.values()) {
            add(new Role(standardRole));
        }
    }

    public void add(Role role) {
        if (role != null) {
            lookup.put(role.getName(), role);
            if (role.isStandard()) {
                standardRoles.add(role);
            } else if (role.isScoped()) {
                scopedRoles.add(role);
            }
        }
    }

    public void clear() {
        lookup.clear();
        standardRoles.clear();
        scopedRoles.clear();
    }

    public List<Role> getRoles() {
        List<Role> roles = new ArrayList<Role>(standardRoles);
        roles.addAll(scopedRoles);
        return roles;
    }

    public List<Role> getStandardRoles() {
        return new ArrayList<Role>(standardRoles);
    }

    public List<Role> getScopedRoles() {
        return new ArrayList<Role>(scopedRoles);
    }

    public Role getRole(String name) {
        if (name != null) {
            return lookup.get(name);
        }
        return null;
    }

    @Override
    public Iterator<Role> iterator() {
        return getRoles().iterator();
    }
}
