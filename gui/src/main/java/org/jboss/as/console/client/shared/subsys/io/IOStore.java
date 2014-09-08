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
package org.jboss.as.console.client.shared.subsys.io;

import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.mbui.behaviour.ModelNodeAdapter;
import org.jboss.dmr.client.ModelNode;
import org.jboss.gwt.circuit.ChangeSupport;

import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
public abstract class IOStore extends ChangeSupport {

    private final Baseadress baseAddress;
    private final String resourceName;

    protected IOStore(Baseadress baseAddress, String resourceName) {
        this.baseAddress = baseAddress;
        this.resourceName = resourceName;
    }


    // ------------------------------------------------------ model node factory methods

    protected ModelNode baseAddress() {
        ModelNode address = baseAddress.getAdress();
        address.add("subsystem", "io");
        return address;
    }

    protected ModelNode resourceAddress(final String name) {
        ModelNode address = baseAddress();
        address.add(resourceName, name);
        return address;
    }

    protected ModelNode readChildrenOp() {
        final ModelNode op = new ModelNode();
        op.get(ADDRESS).set(baseAddress());
        op.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        op.get(CHILD_TYPE).set(resourceName);
        op.get(INCLUDE_RUNTIME).set(true);
        return op;
    }

    protected ModelNode modifyOp(final String name, final Map<String, Object> changedValues) {
        final ModelNodeAdapter adapter = new ModelNodeAdapter();
        ModelNode address = resourceAddress(name);
        return adapter.fromChangeset(changedValues, address);
    }
}