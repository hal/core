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
package org.jboss.as.console.client.shared.subsys.remoting.store;

import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.gwt.circuit.Action;

import java.util.Map;

/**
 * @author Harald Pehl
 */
public abstract class ModifySaslSingleton implements Action {

    private final String connectorName;
    private final AddressTemplate connectorAddress;
    private final Map<String, Object> changedValues;

    public ModifySaslSingleton(String connectorName, AddressTemplate connectorAddress,
                               Map<String, Object> changedValues) {
        this.connectorAddress = connectorAddress;
        this.connectorName = connectorName;
        this.changedValues = changedValues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModifySaslSingleton)) return false;

        ModifySaslSingleton that = (ModifySaslSingleton) o;

        if (!connectorAddress.equals(that.connectorAddress)) return false;
        return connectorName.equals(that.connectorName);

    }

    @Override
    public int hashCode() {
        int result = connectorAddress.hashCode();
        result = 31 * result + connectorName.hashCode();
        return result;
    }

    public AddressTemplate getConnectorAddress() {
        return connectorAddress;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public Map<String, Object> getChangedValues() {
        return changedValues;
    }
}
