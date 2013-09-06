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

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jboss.as.console.client.rbac.Role;
import org.jboss.as.console.client.rbac.StandardRole;

/**
 * @author Harald Pehl
 */
public class ScopedRole implements Role {

    private String name;
    private StandardRole baseRole;
    private Type type;
    private SortedSet<String> scope;

    public ScopedRole(final String name, final StandardRole baseRole,
            final Type type, final Collection<String> scope) {
        this.name = name;
        this.baseRole = baseRole;
        this.type = type;
        this.scope = new TreeSet<String>();
        if (scope != null) {
            this.scope.addAll(scope);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (!(o instanceof ScopedRole)) { return false; }

        ScopedRole that = (ScopedRole) o;

        if (baseRole != that.baseRole) { return false; }
        if (name != null ? !name.equals(that.name) : that.name != null) { return false; }
        if (scope != null ? !scope.equals(that.scope) : that.scope != null) { return false; }
        if (type != that.type) { return false; }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (baseRole != null ? baseRole.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (scope != null ? scope.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(name).append(" extends ").append(baseRole.getName()).append(" scoped to ")
                .append(type.name().toLowerCase()).append(scope);
        return builder.toString();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    public StandardRole getBaseRole() {
        return baseRole;
    }

    public void setBaseRole(final StandardRole baseRole) {
        this.baseRole = baseRole;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public SortedSet<String> getScope() {
        return scope;
    }

    public void setScope(final Collection<String> scope) {
        this.scope.addAll(scope);
    }

    public static enum Type {
        HOST, SERVER_GROUP
    }
}
