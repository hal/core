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

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.StatementContext;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * An implementation for {@link PropertyManager} which expects the properties to be an attribute of type 'map' of
 * the specified address template.
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
public class MapAttributePropertyManager implements PropertyManager {

    private final AddressTemplate addressTemplate;
    private final String attribute;
    private final StatementContext statementContext;
    private final DispatchAsync dispatcher;

    public MapAttributePropertyManager(final AddressTemplate addressTemplate, final String attribute,
            StatementContext statementContext, DispatchAsync dispatcher) {
        this.addressTemplate = addressTemplate;
        this.attribute = attribute;
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
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
    public void openAddDialog(final AddPropertyDialog addDialog) {
        addDialog.setWidth(480);
        addDialog.setHeight(360);
        addDialog.setGlassEnabled(true);
        addDialog.center();
    }

    @Override
    public void closeAddDialog(final AddPropertyDialog addDialog) {
        addDialog.hide();
    }

    @Override
    public String getAddOperationName() {
        return "map-put";
    }

    @Override
    public void onAdd(final Property property, final AddPropertyDialog addDialog) {
        ResourceAddress address = addressTemplate.resolve(statementContext);
        ModelNode op = new ModelNode();
        op.get(ADDRESS).set(address);
        op.get(OP).set("map-put");
        op.get(NAME).set(attribute);
        op.get("key").set(property.getName());
        op.get("value").set(property.getValue());
        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                closeAddDialog(addDialog);
                onAddFailed(property, caught);
            }

            @Override
            public void onSuccess(final DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    closeAddDialog(addDialog);
                    onAddFailed(property, new RuntimeException(response.getFailureDescription()));
                } else {
                    closeAddDialog(addDialog);
                    onAddSuccess(property);
                    Console.getEventBus().fireEvent(new PropertyAddedEvent(addressTemplate, attribute, property));
                }
            }
        });
    }

    @Override
    public void onAddSuccess(final Property property) {
        // nop
    }

    @Override
    public void onAddFailed(final Property property,
            final Throwable t) {
        Console.error("Failed to add " + property.getName(),
                "Error adding " + property.getName() + " = " + property.getValue() + " to " + addressTemplate +
                        "/" + attribute + ": " + t.getMessage());
    }

    @Override
    public void onModify(final Property property) {
        ResourceAddress address = addressTemplate.resolve(statementContext);
        ModelNode op = new ModelNode();
        op.get(ADDRESS).set(address);
        op.get(OP).set("map-put");
        op.get(NAME).set(attribute);
        op.get("key").set(property.getName());
        op.get("value").set(property.getValue());
        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                onModifyFailed(property, caught);
            }

            @Override
            public void onSuccess(final DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    onModifyFailed(property, new RuntimeException(response.getFailureDescription()));
                } else {
                    onModifySuccess(property);
                    Console.getEventBus().fireEvent(new PropertyModifiedEvent(addressTemplate, attribute, property));
                }
            }
        });
    }

    @Override
    public void onModifySuccess(final Property property) {
        // nop
    }

    @Override
    public void onModifyFailed(final Property property, final Throwable t) {
        Console.error("Failed to modify " + property.getName(),
                "Error modifying " + property.getName() + " = " + property.getValue() + " at " + addressTemplate +
                        "/" + attribute + ": " + t.getMessage());
    }

    @Override
    public String getRemoveOperationName() {
        return "map-remove";
    }

    @Override
    public void onRemove(final Property property) {
        ResourceAddress address = addressTemplate.resolve(statementContext);
        ModelNode op = new ModelNode();
        op.get(ADDRESS).set(address);
        op.get(OP).set("map-remove");
        op.get(NAME).set(attribute);
        op.get("key").set(property.getName());
        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                onRemoveFailed(property, caught);
            }

            @Override
            public void onSuccess(final DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    onRemoveFailed(property, new RuntimeException(response.getFailureDescription()));
                } else {
                    onRemoveSuccess(property);
                    Console.getEventBus().fireEvent(new PropertyRemovedEvent(addressTemplate, attribute, property));
                }
            }
        });
    }

    @Override
    public void onRemoveSuccess(final Property property) {
        // nop
    }

    @Override
    public void onRemoveFailed(final Property property, final Throwable t) {
        Console.error("Failed to remove " + property.getName(),
                "Error removing " + property.getName() + " = " + property.getValue() + " from " + addressTemplate +
                        "/" + attribute + ": " + t.getMessage());
    }
}
