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

import org.jboss.as.console.client.v3.dmr.ResourceAddress;

import static org.jboss.as.console.client.administration.accesscontrol.store.Role.Type.HOST;
import static org.jboss.as.console.client.administration.accesscontrol.store.Role.Type.SERVER_GROUP;

/**
 * @author Harald Pehl
 */
final class AddressHelper {

    private AddressHelper() {}

    static ResourceAddress root() {
        return new ResourceAddress()
                .add("core-service", "management")
                .add("access", "authorization");
    }

    static ResourceAddress roleMapping(Role role) {
        return root().add("role-mapping", role.getId());
    }

    static ResourceAddress scopedRole(Role scopedRole) {
        ResourceAddress address = root();
        if (scopedRole.getType() == HOST) {
            address.add("host-scoped-role", scopedRole.getId());
        } else if (scopedRole.getType() == SERVER_GROUP) {
            address.add("server-group-scoped-role", scopedRole.getId());
        }
        return address;
    }

    static ResourceAddress assignment(Assignment assignment) {
        return root()
                .add("role-mapping", assignment.getRole().getId())
                .add(assignment.isInclude() ? "include" : "exclude", assignment.getPrincipal().getId());
    }
}
