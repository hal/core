/*
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.console.client.shared.subsys.elytron.ui.factory;

import org.jboss.as.console.client.shared.subsys.elytron.ui.ElytronGenericResourceView;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.gwt.circuit.Dispatcher;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class CustomCredentialSecurityFactoryView extends ElytronGenericResourceView {

    private ConfigurableSaslServerFactoryFilterEditor filterEditor;

    public CustomCredentialSecurityFactoryView(final Dispatcher circuit,
            final ResourceDescription resourceDescription,
            final SecurityContext securityContext, final String title,
            final AddressTemplate addressTemplate) {
        super(circuit, resourceDescription, securityContext, title, addressTemplate);

        // workaround for HAL-1265 - Not possible to create Elytron custom credetial security factory in standalone mode
        // this workaround it to allow testing elytron subsystem in HAL, until it is fixed in the subsystem level
        ModelNode moduleDesc = resourceDescription.get(OPERATIONS).get(ADD).get(REQUEST_PROPERTIES).get("module");
        moduleDesc.get(REQUIRED).set(true);
        moduleDesc.get(NILLABLE).set(false);

        moduleDesc = resourceDescription.get(ATTRIBUTES).get("module");
        moduleDesc.get(REQUIRED).set(true);
        moduleDesc.get(NILLABLE).set(false);
    }

}
