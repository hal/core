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
package org.jboss.as.console.client.shared.subsys.remoting.ui;

import org.jboss.as.console.client.shared.subsys.remoting.store.ModifySaslSecurity;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.Map;

/**
 * @author Harald Pehl
 */
class SaslSecurityEditor extends SaslSingletonEditor {

    public SaslSecurityEditor(Dispatcher circuit, SecurityContext securityContext,
                              AddressTemplate connectorAddress, ResourceDescription saslSecurityDefinition) {
        super(circuit, securityContext, connectorAddress, saslSecurityDefinition);
    }

    @Override
    protected void onSave(String connectorName, Map<String, Object> changeSet) {
        circuit.dispatch(new ModifySaslSecurity(connectorName, connectorAddress, changeSet));
    }

    @Override
    protected void onUpdate(Property parentConnector) {
        if (parentConnector.getValue().hasDefined("security")) {
            Property security = parentConnector.getValue().get("security").asProperty();
            formAssets.getForm().edit(security.getValue());
        } else {
            formAssets.getForm().clearValues();
        }
    }
}
