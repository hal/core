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

import static org.jboss.as.console.client.administration.accesscontrol.store.Principal.Type.GROUP;
import static org.jboss.as.console.client.administration.accesscontrol.store.Principal.Type.USER;

/**
 * Contains a list of principals stored in the management model.
 *
 * @author Harald Pehl
 */
public class Principals implements Iterable<Principal> {

    private final Map<Principal.Type, Set<Principal>> principals;
    private final Map<String, Principal> lookup;

    public Principals() {
        principals = new HashMap<>();
        principals.put(GROUP, new HashSet<>());
        principals.put(USER, new HashSet<>());
        lookup = new HashMap<>();
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

    public boolean contains(final Principal principal) {
        return lookup.containsKey(principal.getId());
    }

    public Principal get(final String id) {
        return lookup.get(id);
    }

    public Set<Principal> get(Principal.Type type) {
        return principals.get(type);
    }

    public void remove(final Principal principal) {
        principals.get(principal.getType()).remove(principal);
        lookup.remove(principal.getId());
    }

    public void clear() {
        if (principals.containsKey(GROUP)) {
            principals.get(GROUP).clear();
        }
        if (principals.containsKey(USER)) {
            principals.get(USER).clear();
        }
        lookup.clear();
    }

    @Override
    public Iterator<Principal> iterator() {
        return lookup.values().iterator();
    }

    public static Ordering<Principal> orderedByName() {
        //noinspection Convert2MethodRef
        return Ordering.natural().onResultOf(principal -> principal.getName());
    }
}
