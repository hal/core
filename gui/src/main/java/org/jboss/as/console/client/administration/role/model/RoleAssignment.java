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
import java.util.List;

import org.jboss.as.console.client.rbac.Role;

/**
 * A role assignment as used in the UI. This model differs from the management model.
 *
 * @author Harald Pehl
 */
public interface RoleAssignment {

    String getId();
    void setId(String id);

    Principal getPrincipal();
    void setPrincipal(Principal principal);

    List<Role> getRoles();
    void setRoles(List<Role> roles);

    /**
     * The list of roles this principal is excluded from
     */
    List<Role> getExcludes();
    void setExcludes(List<Role> excludes);

    /**
     * The role maping as used in the management model. This is a kind of helper class used to read the role mapping
     * from the management model. After that the data is transformed into instances of
     * {@link org.jboss.as.console.client.administration.role.model.RoleAssignment}.
     *
     * @author Harald Pehl
     */
    class ManagementModel {

        private final Role role;
        private final List<Principal> includes;
        private final List<Principal> excludes;


        public ManagementModel(final Role role) {
            this.role = role;
            includes = new ArrayList<Principal>();
            excludes = new ArrayList<Principal>();
        }

        public void include(Principal principal) {
            includes.add(principal);
        }

        public void exclude(Principal principal) {
            excludes.add(principal);
        }

        public Role getRole() {
            return role;
        }

        public List<Principal> getIncludes() {
            return includes;
        }

        public List<Principal> getExcludes() {
            return excludes;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) { return true; }
            if (!(o instanceof ManagementModel)) { return false; }

            ManagementModel that = (ManagementModel) o;
            if (!role.getName().equals(that.role.getName())) { return false; }

            return true;
        }

        @Override
        public int hashCode() {
            return role.getName().hashCode();
        }
    }
}
