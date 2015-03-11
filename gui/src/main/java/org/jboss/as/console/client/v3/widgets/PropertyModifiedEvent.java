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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.dmr.client.Property;

/**
 * @author Harald Pehl
 */
public class PropertyModifiedEvent extends GwtEvent<PropertyModifiedEvent.PropertyModifiedHandler> {

    public static final Type TYPE = new Type<>();

    public static void fire(final HasHandlers source, AddressTemplate addressTemplate, Property property) {
        source.fireEvent(new PropertyModifiedEvent(addressTemplate, property));
    }

    private final AddressTemplate addressTemplate;
    private final String attribute;
    private final Property property;

    public PropertyModifiedEvent(AddressTemplate addressTemplate, Property property) {
        this(addressTemplate, null, property);
    }

    public PropertyModifiedEvent(AddressTemplate addressTemplate, String attribute, Property property) {
        this.addressTemplate = addressTemplate;
        this.attribute = attribute;
        this.property = property;
    }

    @Override
    public Type<PropertyModifiedHandler> getAssociatedType() {
        //noinspection unchecked
        return TYPE;
    }

    @Override
    protected void dispatch(PropertyModifiedHandler handler) {
        handler.onPropertyModified(this);
    }

    public AddressTemplate getAddressTemplate() {
        return addressTemplate;
    }

    public Property getProperty() {
        return property;
    }

    public String getAttribute() {
        return attribute;
    }

    public interface PropertyModifiedHandler extends EventHandler {
        void onPropertyModified(PropertyModifiedEvent event);
    }
}
