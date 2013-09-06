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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jboss.as.console.client.shared.model.HasNameComparator;

/**
 * Contains a list of principals stored in the management model.
 *
 * @author Harald Pehl
 */
public class Principals implements Iterable<Principal> {

    private final Map<Principal.Type, SortedSet<Principal>> principals;

    public Principals() {
        principals = new HashMap<Principal.Type, SortedSet<Principal>>();
        principals.put(GROUP, new TreeSet<Principal>(new HasNameComparator<Principal>()));
        principals.put(USER, new TreeSet<Principal>(new HasNameComparator<Principal>()));
    }

    public void add(Principal principal) {
        if (principal != null) {
            SortedSet<Principal> set = principals.get(principal.getType());
            if (set != null) {
                set.add(principal);
            }
        }
    }

    @Override
    public Iterator<Principal> iterator() {
        SortedSet<Principal> all = new TreeSet<Principal>(new HasNameComparator<Principal>());
        all.addAll(principals.get(GROUP));
        all.addAll(principals.get(USER));
        return all.iterator();
    }

    public List<Principal> get(Principal.Type type) {
        return new ArrayList<Principal>(principals.get(type));
    }
}
