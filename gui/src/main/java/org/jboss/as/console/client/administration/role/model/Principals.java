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

import static org.jboss.as.console.client.administration.role.model.Principal.Type.GROUP;
import static org.jboss.as.console.client.administration.role.model.Principal.Type.USER;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Contains a list of principals stored in the management model.
 *
 * @author Harald Pehl
 */
public class Principals implements Iterable<Principal> {

    private final Map<Principal.Type, Set<Principal>> principals;
    private final Map<String, Principal> lookup;

    public Principals() {
        principals = new HashMap<Principal.Type, Set<Principal>>();
        principals.put(GROUP, new HashSet<Principal>());
        principals.put(USER, new HashSet<Principal>());
        lookup = new HashMap<String, Principal>();
    }

    public void add(Principal principal) {
        if (principal != null) {
            Set<Principal> set = principals.get(principal.getType());
            if (set != null) {
                set.add(principal);
            }
            // Principal.getId() already encodes the type
            lookup.put(principal.getId(), principal);
        }
    }

    @Override
    public Iterator<Principal> iterator() {
        Set<Principal> all = new HashSet<Principal>();
        all.addAll(principals.get(GROUP));
        all.addAll(principals.get(USER));
        return all.iterator();
    }

    public Set<Principal> get(Principal.Type type) {
        return principals.get(type);
    }

    public Principal lookup(final Principal.Type type, final String name) {
        return lookup.get(Principal.id(type, name));
    }
}
