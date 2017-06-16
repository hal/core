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
package org.jboss.as.console.client.shared.subsys.elytron.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.elytron.store.ElytronStore;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class PolicyView extends ElytronGenericResourceView {

    private GenericListComplexAttribute customPolicy;
    private GenericListComplexAttribute jaccPolicy;

    public PolicyView(final Dispatcher circuit,
            final ResourceDescription resourceDescription,
            final SecurityContext securityContext, final String title,
            final AddressTemplate addressTemplate) {
        super(circuit, resourceDescription, securityContext, title, addressTemplate);
        excludesFormAttributes("custom-policy", "jacc-policy");

    }

    @Override
    public Map<String, Widget> additionalTabDetails() {
        Map<String, Widget> additionalWidgets = new HashMap<>();
        customPolicy = new GenericListComplexAttribute(circuit, resourceDescription, securityContext, ElytronStore.POLICY_ADDRESS, "custom-policy");
        jaccPolicy = new GenericListComplexAttribute(circuit, resourceDescription, securityContext, ElytronStore.POLICY_ADDRESS, "jacc-policy");
        additionalWidgets.put("Custom Policy", customPolicy.asWidget());
        additionalWidgets.put("JACC Policy", jaccPolicy.asWidget());
        return additionalWidgets;
    }

    @Override
    public void update(final List<Property> models) {
        super.update(models);
        if (models.isEmpty()) {
            customPolicy.clearValues();
            jaccPolicy.clearValues();
        }
    }

    @Override
    protected void selectTableItem(final Property prop) {
        if (prop != null) {
            customPolicy.update(prop);
            jaccPolicy.update(prop);
        } else {
            customPolicy.clearValues();
            jaccPolicy.clearValues();
        }
    }

}
