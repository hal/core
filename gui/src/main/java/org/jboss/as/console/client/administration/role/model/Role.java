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
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.gwt.view.client.ProvidesKey;
import org.jboss.as.console.client.rbac.StandardRole;

/**
 * @author Harald Pehl
 */
public class Role {

    private String id;
    private String name;
    private StandardRole baseRole;
    private Type type;
    private SortedSet<String> scope;
    private boolean includeAll;

    public Role(StandardRole role) {
        this(role.getId(), role.getId(), null, Type.STANDARD, Collections.<String>emptySet());
    }

    public Role(final String id, final String name, final StandardRole baseRole, final Type type,
            final Collection<String> scope) {
        this.id = id;
        this.name = name;
        this.baseRole = baseRole;
        this.type = type;
        this.scope = new TreeSet<String>();
        if (scope != null) {
            this.scope.addAll(scope);
        }
        this.includeAll = false;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Role)) {
            return false;
        }

        Role role = (Role) o;
        //noinspection RedundantIfStatement
        if (!id.equals(role.id)) {
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
        if (isStandard()) {
            return name;
        }
        return id + " extends " + baseRole.getId() + " scoped to " + type.name()
                .toLowerCase() + scope + " includeAll: " + includeAll;
    }

    public boolean isStandard() {
        return type == Type.STANDARD;
    }

    public boolean isScoped() {
        return type != Type.STANDARD;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

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
        this.scope.clear();
        this.scope.addAll(scope);
    }

    public boolean isIncludeAll() {
        return includeAll;
    }

    public void setIncludeAll(final boolean includeAll) {
        this.includeAll = includeAll;
    }

    public static enum Type {
        STANDARD, HOST, SERVER_GROUP
    }

    public static class Key implements ProvidesKey<Role> {

        @Override
        public Object getKey(final Role item) {
            return item.getName();
        }
    }
}
