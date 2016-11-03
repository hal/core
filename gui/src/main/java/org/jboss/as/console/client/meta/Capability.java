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
package org.jboss.as.console.client.meta;

import java.util.LinkedHashSet;
import java.util.Set;

import com.google.common.collect.Iterables;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;

/**
 * @author Claudio Miranda
 */
public class Capability {

    private final String name;
    private final Set<AddressTemplate> templates;
    private final boolean dynamic;

    public Capability(final String name, final boolean dynamic) {
        this.name = name;
        this.dynamic = dynamic;
        this.templates = new LinkedHashSet<>();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (!(o instanceof Capability)) { return false; }

        Capability that = (Capability) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("Capability(").append(name).append(" => ");
        for (AddressTemplate addr: templates) {
            str.append(addr.toString()).append(", ");
        }
        return str.toString();
    }

    public void addTemplate(final AddressTemplate template) {
        templates.add(template);
    }

    public String getName() {
        return name;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public Iterable<AddressTemplate> getTemplates() {
        return Iterables.unmodifiableIterable(templates);
    }
}
