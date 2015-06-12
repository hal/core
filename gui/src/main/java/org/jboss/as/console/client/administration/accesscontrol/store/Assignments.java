/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.administration.accesscontrol.store;

import com.google.common.collect.Ordering;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Harald Pehl
 */
public class Assignments implements Iterable<Assignment> {

    private final Set<Assignment> assignments;

    public Assignments() {assignments = new HashSet<>();}

    public boolean add(final Assignment assignment) {return assignments.add(assignment);}

    public void clear() {assignments.clear();}

    @Override
    public Iterator<Assignment> iterator() {
        return assignments.iterator();
    }

    public static Ordering<Assignment> orderedByPrincipal() {
        Ordering<Assignment> byType = new Ordering<Assignment>() {
            @Override
            public int compare(final Assignment left, final Assignment right) {
                return left.getPrincipal().getType().compareTo(right.getPrincipal().getType());
            }
        };
        Ordering<Assignment> byName = Ordering.natural().onResultOf(input -> input.getPrincipal().getName());
        return byType.compound(byName);
    }

    public static Ordering<Assignment> orderedByRole() {
        Ordering<Assignment> byType = new Ordering<Assignment>() {
            @Override
            public int compare(final Assignment left, final Assignment right) {
                return left.getRole().getType().compareTo(right.getRole().getType());
            }
        };
        Ordering<Assignment> byName = Ordering.natural().onResultOf(input -> input.getRole().getName());
        return byType.compound(byName);
    }
}
