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

import static org.jboss.dmr.client.ModelDescriptionConstants.ADD;
import static org.jboss.dmr.client.ModelDescriptionConstants.NAME;
import static org.jboss.dmr.client.ModelDescriptionConstants.REMOVE;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.useware.kernel.gui.behaviour.StatementContext;

/**
 * An implementation for {@link PropertyManager} which expects the properties to be sub resources of
 * the specified address template. An instance of {@link org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate}
 * is used to manage the property resources.
 * <p>
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
public class SubResourcePropertyManager implements PropertyManager {

    private final AddressTemplate addressTemplate;
    private final CrudOperationDelegate operationDelegate;
    private final StatementContext statementContext;

    public SubResourcePropertyManager(final AddressTemplate addressTemplate,
            StatementContext statementContext, DispatchAsync dispatcher) {
        this.addressTemplate = addressTemplate;
        this.statementContext = statementContext;
        this.operationDelegate = new CrudOperationDelegate(statementContext, dispatcher);
    }

    @Override
    public AddressTemplate getAddress() {
        return addressTemplate;
    }

    @Override
    public void onSelect(final Property property) {
        // nop
    }

    @Override
    public void onDeselect() {
        // nop
    }

    @Override
    public void openAddDialog(AddPropertyDialog addDialog) {
        addDialog.setWidth(480);
        addDialog.setHeight(360);
        addDialog.setGlassEnabled(true);
        addDialog.center();
    }

    @Override
    public void closeAddDialog(AddPropertyDialog addDialog) {
        addDialog.hide();
    }

    @Override
    public String getAddOperationName() {
        return ADD;
    }

    @Override
    public void onAdd(final Property property,
            final AddPropertyDialog addDialog) {
        operationDelegate.onCreateResource(addressTemplate, property.getName(), property.getValue(),
                new CrudOperationDelegate.Callback() {
                    @Override
                    public void onSuccess(AddressTemplate addressTemplate, String name) {
                        closeAddDialog(addDialog);
                        onAddSuccess(property);
                        ResourceAddress resolve = addressTemplate.resolve(statementContext, name);
                        String elemName = null;
                        if (resolve != null && resolve.get(1) != null && resolve.get(1).get(0) != null)
                            elemName = resolve.get(1).get(0).asString();
                        if ("undefined".equals(elemName))
                            elemName = null;
                        Console.getEventBus().fireEvent(new PropertyAddedEvent(addressTemplate, elemName, property));
                    }

                    @Override
                    public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {
                        closeAddDialog(addDialog);
                        onAddFailed(property, t);
                    }
                });
    }

    @Override
    public void onAddSuccess(final Property property) {
        // nop
    }

    @Override
    public void onAddFailed(final Property property,
            Throwable t) {
        Console.error("Failed to add " + property.getName(),
                "Error adding " + property.getName() + " = " + property.getValue() + " to " + addressTemplate + ": " + t
                        .getMessage());
    }

    @Override
    public void onModify(final Property property) {
        Map<String, Object> changeSet = new HashMap<>();
        changeSet.put(NAME, property.getValue().get(NAME).asString());
        operationDelegate
                .onSaveResource(addressTemplate, property.getName(), changeSet, new CrudOperationDelegate.Callback() {
                    @Override
                    public void onSuccess(AddressTemplate addressTemplate, String name) {
                        onModifySuccess(property);
                        Console.getEventBus().fireEvent(new PropertyModifiedEvent(addressTemplate, property));
                    }

                    @Override
                    public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {
                        onModifyFailed(property, t);
                    }
                });
    }

    @Override
    public void onModifySuccess(final Property property) {
        // nop
    }

    @Override
    public void onModifyFailed(final Property property,
            Throwable t) {
        Console.error("Failed to modify " + property.getName(),
                "Error modifying " + property.getName() + " = " + property
                        .getValue() + " at " + addressTemplate + ": " + t.getMessage());
    }

    @Override
    public String getRemoveOperationName() {
        return REMOVE;
    }

    @Override
    public void onRemove(final Property property) {
        operationDelegate.onRemoveResource(addressTemplate, property.getName(), new CrudOperationDelegate.Callback() {
            @Override
            public void onSuccess(AddressTemplate addressTemplate, String name) {
                onRemoveSuccess(property);
                ResourceAddress resolve = addressTemplate.resolve(statementContext, name);
                String elemName = null;
                if (resolve != null && resolve.get(1) != null && resolve.get(1).get(0) != null)
                    elemName = resolve.get(1).get(0).asString();
                if ("undefined".equals(elemName))
                    elemName = null;
                Console.getEventBus().fireEvent(new PropertyRemovedEvent(addressTemplate, elemName, property));
            }

            @Override
            public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {
                onRemoveFailed(property, t);
            }
        });
    }

    @Override
    public void onRemoveSuccess(final Property property) {
        // nop
    }

    @Override
    public void onRemoveFailed(final Property property,
            Throwable t) {
        Console.error("Failed to remove " + property.getName(),
                "Error removing " + property.getName() + " = " + property
                        .getValue() + " from " + addressTemplate + ": " + t.getMessage());
    }
}
