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
package org.jboss.as.console.mbui.widgets;

import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.dmr.ResourceDefinition;
import org.jboss.dmr.client.ModelDescriptionConstants;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.CHILDREN;
import static org.jboss.dmr.client.ModelDescriptionConstants.MODEL_DESCRIPTION;

/**
 * @deprecated Replace with {@link org.jboss.as.console.client.v3.dmr.ResourceDescription}
 * @author Harald Pehl
 */
@Deprecated
public class ResourceDescription {

    private final AddressTemplate template;
    private final ResourceAddress address;
    private ResourceDefinition definition;

    public ResourceDescription(AddressTemplate template, ResourceAddress address) {
        this.template = template;
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceDescription)) return false;

        ResourceDescription that = (ResourceDescription) o;
        return template.equals(that.template);

    }

    @Override
    public int hashCode() {
        return template.hashCode();
    }

    public ResourceAddress getAddress() {
        return address;
    }

    public ResourceDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(ResourceDefinition definition) {
        this.definition = definition;
    }

    public ResourceDefinition getChildDefinition(String name) {
        if (definition != null) {
            if (definition.hasDefined(CHILDREN)) {
                List<Property> properties = definition.get(CHILDREN).asPropertyList();
                if (!properties.isEmpty()) {
                    for (Property property : properties) {
                        if (name.equals(property.getName()) && property.getValue().hasDefined(MODEL_DESCRIPTION)) {
                            Property modelDescription = property.getValue().get(MODEL_DESCRIPTION).asProperty();
                            return new ResourceDefinition(modelDescription.getValue());
                        }
                    }
                }
            }
        }
        return null;
    }

    public AddressTemplate getTemplate() {
        return template;
    }
}
