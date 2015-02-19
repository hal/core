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
package org.jboss.as.console.client.v3.stores;

import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.dmr.client.ModelNode;
import org.jboss.gwt.circuit.Action;

import java.util.Collections;
import java.util.Map;

import static org.jboss.as.console.client.v3.stores.CrudAction.Crud.*;

/**
 * @author Harald Pehl
 */
public abstract class CrudAction implements Action {

    public enum Crud {
        CREATE, READ, UPDATE, DELETE
    }

    private final Crud crud;
    private final AddressTemplate addressTemplate;
    private final ModelNode newModel;
    private final Map<String, Object> changedValues;
    private final String instanceName;

    /**
     * Create
     */
    public CrudAction(AddressTemplate addressTemplate, String instanceName, ModelNode newModel) {
        this(CREATE, addressTemplate, instanceName, newModel, Collections.<String, Object>emptyMap());
    }

    /**
     * Read
     */
    public CrudAction(AddressTemplate addressTemplate) {
        this(READ, addressTemplate, null, new ModelNode(), Collections.<String, Object>emptyMap());
    }

    /**
     * Update
     */
    public CrudAction(AddressTemplate addressTemplate, String instanceName, Map<String, Object> changedValues) {
        this(UPDATE, addressTemplate, instanceName, new ModelNode(), changedValues);
    }

    /**
     * Delete
     */
    public CrudAction(AddressTemplate addressTemplate, String instanceName) {
        this(DELETE, addressTemplate, instanceName, new ModelNode(), Collections.<String, Object>emptyMap());
    }

    private CrudAction(Crud crud, AddressTemplate addressTemplate, String instanceName, ModelNode newModel,
                       Map<String, Object> changedValues) {
        this.crud = crud;
        this.addressTemplate = addressTemplate;
        this.newModel = newModel;
        this.changedValues = changedValues;
        this.instanceName = instanceName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CrudAction)) return false;

        CrudAction that = (CrudAction) o;
        return crud == that.crud && addressTemplate.equals(that.addressTemplate);

    }

    @Override
    public int hashCode() {
        int result = crud.hashCode();
        result = 31 * result + addressTemplate.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CrudAction{").append(crud);
        if (instanceName != null) {
            builder.append(" \"").append(instanceName).append("\"");
        }
        builder.append(" @ ").append(addressTemplate).append("}");
        return builder.toString();
    }

    public AddressTemplate getAddressTemplate() {
        return addressTemplate;
    }

    public Map<String, Object> getChangedValues() {
        return changedValues;
    }

    public Crud getCrud() {
        return crud;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public ModelNode getNewModel() {
        return newModel;
    }
}
