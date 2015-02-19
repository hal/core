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
package org.jboss.as.console.client.v3.widgets;

import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * A view capable of selecting and updating its state based on an address template and DMR payload.
 *
 * @author Harald Pehl
 */
public interface AddressableResourceView {

    void select(AddressTemplate addressTemplate, String name);

    void update(AddressTemplate addressTemplate, ModelNode model);

    /**
     * Update a list of models.
     *
     * @param addressTemplate the address of the resource
     * @param model           the models
     */
    void update(AddressTemplate addressTemplate, List<Property> model);
}
