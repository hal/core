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

import static org.jboss.as.console.client.administration.role.model.Role.Type.HOST;
import static org.jboss.as.console.client.administration.role.model.Role.Type.SERVER_GROUP;
import static org.jboss.dmr.client.ModelDescriptionConstants.ADDRESS;

import org.jboss.dmr.client.ModelNode;

/**
 * @author Harald Pehl
 */
public final class ModelHelper {

    public static final String LOCAL_USERNAME = "$local";

    private ModelHelper() {
    }

    public static ModelNode roleMapping(final Role role) {
        ModelNode node = new ModelNode();
        node.get(ADDRESS).add("core-service", "management");
        node.get(ADDRESS).add("access", "authorization");
        node.get(ADDRESS).add("role-mapping", role.getId());
        return node;
    }

    public static ModelNode includeExclude(final Role role, final Principal principal, final String realm,
            final String includeExclude) {
        StringBuilder principalId = new StringBuilder(principal.getId());
        if (realm != null && realm.trim().length() != 0) {
            principalId.append("@").append(realm);
        }

        ModelNode node = new ModelNode();
        node.get(ADDRESS).add("core-service", "management");
        node.get(ADDRESS).add("access", "authorization");
        node.get(ADDRESS).add("role-mapping", role.getId());
        node.get(ADDRESS).add(includeExclude, principalId.toString());
        return node;
    }

    public static ModelNode scopedRole(Role scopedRole) {
        ModelNode node = new ModelNode();
        node.get(ADDRESS).add("core-service", "management");
        node.get(ADDRESS).add("access", "authorization");
        if (scopedRole.getType() == HOST) {
            node.get(ADDRESS).add("host-scoped-role", scopedRole.getId());
        } else if (scopedRole.getType() == SERVER_GROUP) {
            node.get(ADDRESS).add("server-group-scoped-role", scopedRole.getId());
        }
        return node;
    }
}
