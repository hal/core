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
package org.jboss.as.console.client.administration.accesscontrol.store;

import com.google.common.collect.Ordering;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Contains the list of standard roles plus the custom defined scoped roles.
 *
 * @author Harald Pehl
 */
public class Roles implements Iterable<Role> {

    private final Map<String, Role> lookup;
    private final Set<Role> standardRoles;
    private final Set<Role> scopedRoles;

    public Roles() {
        this.lookup = new HashMap<>();
        this.standardRoles = new HashSet<>();
        this.scopedRoles = new HashSet<>();
    }

    public void add(Role role) {
        if (role != null) {
            lookup.put(role.getId(), role);
            if (role.isStandard()) {
                standardRoles.add(role);
            } else if (role.isScoped()) {
                scopedRoles.add(role);
            }
        }
    }

    public Role get(String id) {
        if (id != null) {
            return lookup.get(id);
        }
        return null;
    }

    public Set<Role> getScopedRoles() {
        return scopedRoles;
    }

    public void clear() {
        lookup.clear();
        standardRoles.clear();
        scopedRoles.clear();
    }

    @Override
    public Iterator<Role> iterator() {
        return lookup.values().iterator();
    }

    public static Ordering<Role> orderedByName() {
        return Ordering.natural().onResultOf(Role::getName);
    }

    public static Ordering<Role> orderedByType() {
        return new Ordering<Role>() {
            @Override
            public int compare(final Role left, final Role right) {
                return left.getType().compareTo(right.getType());
            }
        };
    }
}
