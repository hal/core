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
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

import static org.jboss.dmr.client.ModelDescriptionConstants.NAME;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class PolicyView extends ElytronGenericResourceView {

    private static final String CUSTOM_POLICY = "custom-policy";
    private static final String JACC_POLICY = "jacc-policy";
    private static final String DEFAULT_POLICY = "default-policy";
    private GenericListComplexAttribute customPolicy;
    private GenericListComplexAttribute jaccPolicy;

    public PolicyView(final Dispatcher circuit,
            final ResourceDescription resourceDescription,
            final SecurityContext securityContext, final String title,
            final AddressTemplate addressTemplate) {
        super(circuit, resourceDescription, securityContext, title, addressTemplate);
        excludesFormAttributes(CUSTOM_POLICY, JACC_POLICY);

    }

    @Override
    public Map<String, Widget> additionalTabDetails() {
        Map<String, Widget> additionalWidgets = new HashMap<>();
        customPolicy = new GenericListComplexAttribute(circuit, resourceDescription, securityContext,
                ElytronStore.POLICY_ADDRESS, CUSTOM_POLICY);
        jaccPolicy = new GenericListComplexAttribute(circuit, resourceDescription, securityContext,
                ElytronStore.POLICY_ADDRESS, JACC_POLICY);
        additionalWidgets.put("Custom Policy", customPolicy.asWidget());
        additionalWidgets.put("JACC Policy", jaccPolicy.asWidget());
        return additionalWidgets;
    }

    protected void onAddCallbackBeforeSubmit(final ModelNode payload) {
        ModelNode policyName = payload.get(NAME);
        // set the defaul-policy if user didn't set in UI
        if (!payload.hasDefined(DEFAULT_POLICY)) {
            payload.get(DEFAULT_POLICY).set(policyName);
        }
        // add a policy to the list of jacc-policy.
        // r-r-d doesn't requires a jacc-policy, but a policy resource cannot be added with no jacc-policy/custom-policy
        ModelNode jaccPolicy = new ModelNode();
        jaccPolicy.get(NAME).set(policyName);
        payload.get(JACC_POLICY).add(jaccPolicy);
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
