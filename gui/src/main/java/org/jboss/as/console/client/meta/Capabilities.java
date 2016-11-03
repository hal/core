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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.console.client.v3.dmr.AddressTemplate;

/**
 * @author Claudio Miranda
 */
public class Capabilities {
    
    private final Map<String, Capability> registry;

    public Capabilities() {
        this.registry = new HashMap<>();
    }

    public Iterable<AddressTemplate> lookup(final String name) {
        if (contains(name)) {
            return registry.get(name).getTemplates();
        }
        return Collections.emptyList();
    }

    public boolean contains(final String name) {return registry.containsKey(name);}

    public void register(final String name, final boolean dynamic,
            final AddressTemplate first, AddressTemplate... rest) {
        Capability capability = safeGet(name, dynamic);
        capability.addTemplate(first);
        if (rest != null) {
            for (AddressTemplate template : rest) {
                capability.addTemplate(template);
            }
        }
    }

    public void register(final Capability capability) {
        if (contains(capability.getName())) {
            Capability existing = registry.get(capability.getName());
            for (AddressTemplate template : capability.getTemplates()) {
                existing.addTemplate(template);
            }
        } else {
            registry.put(capability.getName(), capability);
        }
    }

    private Capability safeGet(String name, final boolean dynamic) {
        if (registry.containsKey(name)) {
            return registry.get(name);
        } else {
            Capability capability = new Capability(name, dynamic);
            registry.put(name, capability);
            return capability;
        }
    }

}
