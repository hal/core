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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.view.client.ProvidesKey;
import org.jboss.as.console.client.rbac.Role;

/**
 * A role assignment as used in the UI. This model differs from the management model.
 *
 * @author Harald Pehl
 */
public class RoleAssignment {

    private final Principal principal;
    private final Set<Role> roles;
    private final Set<Role> excludes;
    private String realm;

    public RoleAssignment(final Principal principal) {
        this.principal = principal;
        this.roles = new HashSet<Role>();
        this.excludes = new HashSet<Role>();
    }

    @Override
    @SuppressWarnings("RedundantIfStatement")
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (!(o instanceof RoleAssignment)) { return false; }

        RoleAssignment that = (RoleAssignment) o;

        if (!excludes.equals(that.excludes)) { return false; }
        if (!principal.equals(that.principal)) { return false; }
        if (realm != null ? !realm.equals(that.realm) : that.realm != null) { return false; }
        if (!roles.equals(that.roles)) { return false; }

        return true;
    }

    @Override
    public int hashCode() {
        int result = principal.hashCode();
        result = 31 * result + (realm != null ? realm.hashCode() : 0);
        result = 31 * result + roles.hashCode();
        result = 31 * result + excludes.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return getId();
    }

    public String getId() {
        StringBuilder id = new StringBuilder();
        id.append(principal);
        if (realm != null) {
            id.append("@").append(realm);
        }
        id.append(" --> ").append(roles).append(" excludes ").append(excludes);
        return id.toString();
    }

    public Principal getPrincipal() {
        return principal;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(final String realm) {
        this.realm = realm;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void addRole(Role role) {
        roles.add(role);
    }

    public void addRoles(Collection<Role> roles) {
        if (roles != null) {
            this.roles.addAll(roles);
        }
    }

    public Set<Role> changedRoles(RoleAssignment diff) {
        if (diff != null) {
            Set<Role> copy = new HashSet<Role>(diff.getRoles());
            copy.removeAll(this.roles);
            return copy;
        }
        return Collections.emptySet();
    }

    public Set<Role> getExcludes() {
        return excludes;
    }

    public void addExclude(Role exclude) {
        excludes.add(exclude);
    }

    public void addExcludes(Collection<Role> excludes) {
        if (excludes != null) {
            this.excludes.addAll(excludes);
        }
    }

    public Set<Role> changedExcludes(RoleAssignment diff) {
        if (diff != null) {
            Set<Role> copy = new HashSet<Role>(diff.getExcludes());
            copy.removeAll(this.excludes);
            return copy;
        }
        return Collections.emptySet();
    }

    public static class Key implements ProvidesKey<RoleAssignment> {

        @Override
        public Object getKey(final RoleAssignment roleAssignment) {
            return roleAssignment.getId();
        }
    }

    /**
     * The role maping as used in the management model. This is a kind of helper class used to read the role mapping
     * from the management model. After that the data is transformed into instances of
     * {@link org.jboss.as.console.client.administration.role.model.RoleAssignment}.
     *
     * @author Harald Pehl
     */
    public static class Internal {

        private final Role role;
        private final List<PrincipalRealmTupel> includes;
        private final List<PrincipalRealmTupel> excludes;

        public Internal(final Role role) {
            this.role = role;
            includes = new ArrayList<PrincipalRealmTupel>();
            excludes = new ArrayList<PrincipalRealmTupel>();
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) { return true; }
            if (!(o instanceof Internal)) { return false; }

            Internal that = (Internal) o;
            return role.getName().equals(that.role.getName());
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(role.getName()).append(" +").append(includes).append(" -").append(excludes);
            return builder.toString();
        }

        @Override
        public int hashCode() {
            return role.getName().hashCode();
        }

        public void include(PrincipalRealmTupel principal) {
            includes.add(principal);
        }

        public void exclude(PrincipalRealmTupel principal) {
            excludes.add(principal);
        }

        public Role getRole() {
            return role;
        }

        public List<PrincipalRealmTupel> getIncludes() {
            return includes;
        }

        public List<PrincipalRealmTupel> getExcludes() {
            return excludes;
        }
    }

    public static class PrincipalRealmTupel {
        public final Principal principal;
        public final String realm;

        public PrincipalRealmTupel(final Principal principal, final String realm) {
            this.principal = principal;
            this.realm = realm;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) { return true; }
            if (!(o instanceof PrincipalRealmTupel)) { return false; }

            PrincipalRealmTupel that = (PrincipalRealmTupel) o;

            if (!principal.equals(that.principal)) { return false; }
            if (realm != null ? !realm.equals(that.realm) : that.realm != null) { return false; }

            return true;
        }

        @Override
        public int hashCode() {
            int result = principal.hashCode();
            result = 31 * result + (realm != null ? realm.hashCode() : 0);
            return result;
        }
    }
}
