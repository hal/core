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
package org.jboss.as.console.client.shared.subsys.elytron.store;

import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class RemoveListAttribute extends ResourceGeneric {

    private String resourceName;
    private String attributeName;
    private ModelNode payload;

    /**
     * 
     * @param address - The resource address as /subsystem=elytron/key-store=*
     * @param resourceName  - The resource name, keyStoreSample1 as in /subsystem=elytron/key-store=keyStoreSample1 
     * @param attributeName - The attribute name, example: filters
     * @param payload - The payload modelnode
     */
    public RemoveListAttribute(AddressTemplate address, String resourceName, String attributeName, ModelNode payload) {
        super(address);
        this.resourceName = resourceName;
        this.attributeName = attributeName;
        this.payload = payload;
    }

    public ModelNode getPayload() {
        return payload;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getResourceName() {
        return resourceName;
    }
}
