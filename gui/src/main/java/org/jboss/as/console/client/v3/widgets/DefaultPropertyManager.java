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

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.HashMap;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.NAME;

/**
 * A default implementation for {@link PropertyManager} based on
 * {@link org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate}.
 * <p/>
 * This implementation emits events using the {@link Console#getEventBus()} after successful
 * adding / modifying / removing properties:
 * <ul>
 * <li>{@link PropertyAddedEvent}</li>
 * <li>{@link PropertyModifiedEvent}</li>
 * <li>{@link PropertyRemovedEvent}</li>
 * </ul>
 *
 * @author Harald Pehl
 */
public class DefaultPropertyManager implements PropertyManager {

    private final CrudOperationDelegate operationDelegate;

    public DefaultPropertyManager(DispatchAsync dispatcher, StatementContext statementContext) {
        this.operationDelegate = new CrudOperationDelegate(statementContext, dispatcher);
    }

    @Override
    public void onSelect(final AddressTemplate addressTemplate, final Property property) {
        // nop
    }

    @Override
    public void onDeselect(final AddressTemplate addressTemplate) {
        // nop
    }

    @Override
    public void openAddDialog(AddressTemplate addressTemplate, DefaultWindow addDialog) {
        addDialog.setWidth(480);
        addDialog.setHeight(360);
        addDialog.setGlassEnabled(true);
        addDialog.center();
    }

    @Override
    public void closeAddDialog(AddressTemplate addressTemplate, DefaultWindow window) {
        window.hide();
    }

    @Override
    public void onAdd(final AddressTemplate addressTemplate, final Property property, final DefaultWindow window) {
        operationDelegate.onCreateResource(addressTemplate, property.getName(), property.getValue(), new CrudOperationDelegate.Callback() {
            @Override
            public void onSuccess(AddressTemplate addressTemplate, String name) {
                closeAddDialog(addressTemplate, window);
                onAddSuccess(addressTemplate, property);
                Console.getEventBus().fireEvent(new PropertyAddedEvent(addressTemplate, property));
            }

            @Override
            public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {
                closeAddDialog(addressTemplate, window);
                onAddFailed(addressTemplate, property, t);
            }
        });
    }

    @Override
    public void onAddSuccess(final AddressTemplate addressTemplate, final Property property) {
        // nop
    }

    @Override
    public void onAddFailed(final AddressTemplate addressTemplate, final Property property, Throwable t) {
        Console.error("Failed to add " + property.getName(), "Error adding " + property.getName() + " = " + property.getValue() + " to " + addressTemplate + ": " + t.getMessage());
    }

    @Override
    public void onModify(final AddressTemplate addressTemplate, final Property property) {
        Map<String, Object> changeSet = new HashMap<>();
        changeSet.put(NAME, property.getValue().get(NAME).asString());
        operationDelegate.onSaveResource(addressTemplate, property.getName(), changeSet, new CrudOperationDelegate.Callback() {
            @Override
            public void onSuccess(AddressTemplate addressTemplate, String name) {
                onModifySuccess(addressTemplate, property);
                Console.getEventBus().fireEvent(new PropertyModifiedEvent(addressTemplate, property));
            }

            @Override
            public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {
                onModifyFailed(addressTemplate, property, t);
            }
        });
    }

    @Override
    public void onModifySuccess(final AddressTemplate addressTemplate, final Property property) {
        // nop
    }

    @Override
    public void onModifyFailed(final AddressTemplate addressTemplate, final Property property, Throwable t) {
        Console.error("Failed to modify " + property.getName(), "Error modifying " + property.getName() + " = " + property.getValue() + " at " + addressTemplate + ": " + t.getMessage());
    }

    @Override
    public void onRemove(final AddressTemplate addressTemplate, final Property property) {
        operationDelegate.onRemoveResource(addressTemplate, property.getName(), new CrudOperationDelegate.Callback() {
            @Override
            public void onSuccess(AddressTemplate addressTemplate, String name) {
                onRemoveSuccess(addressTemplate, property);
                Console.getEventBus().fireEvent(new PropertyRemovedEvent(addressTemplate, property));
            }

            @Override
            public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {
                onRemoveFailed(addressTemplate, property, t);
            }
        });
    }

    @Override
    public void onRemoveSuccess(final AddressTemplate addressTemplate, final Property property) {
        // nop
    }

    @Override
    public void onRemoveFailed(final AddressTemplate addressTemplate, final Property property, Throwable t) {
        Console.error("Failed to remove " + property.getName(), "Error removing " + property.getName() + " = " + property.getValue() + " from " + addressTemplate + ": " + t.getMessage());
    }
}
