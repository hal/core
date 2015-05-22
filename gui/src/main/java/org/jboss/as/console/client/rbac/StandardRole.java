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
package org.jboss.as.console.client.rbac;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author Heiko Braun
 */
public class StandardRole {

    public static final String SUPER_USER = "SuperUser";
    public static final String ADMINISTRATOR = "Administrator";
    private static SortedMap<String, StandardRole> repository = new TreeMap<String, StandardRole>();
    private final String id;

    private StandardRole(final String id) {this.id = id;}

    public static void clearValues() {
        repository.clear();
    }

    public static StandardRole add(String id) {
        StandardRole standardRole = new StandardRole(id);
        repository.put(id, standardRole);
        return standardRole;
    }

    public static StandardRole fromId(String id) {
        return repository.get(id);
    }

    public static StandardRole matchId(String id) {
        for (StandardRole role : repository.values()) {
            if (id.equalsIgnoreCase(role.getId())) {
                return role;
            }
        }
        return null;
    }

    public static Collection<StandardRole> values() {
        return repository.values();
    }

    public static SortedMap<String, StandardRole> repository() {
        return repository;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StandardRole)) {
            return false;
        }

        StandardRole that = (StandardRole) o;

        if (!id.equals(that.id)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "StandardRole{" + id + "}";
    }

    public String getId() {
        return id;
    }
}
