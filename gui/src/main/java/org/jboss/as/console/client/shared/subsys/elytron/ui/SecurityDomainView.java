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
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

import static org.jboss.dmr.client.ModelDescriptionConstants.ADD;
import static org.jboss.dmr.client.ModelDescriptionConstants.NILLABLE;
import static org.jboss.dmr.client.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.dmr.client.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.dmr.client.ModelDescriptionConstants.VALUE_TYPE;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class SecurityDomainView extends ElytronGenericResourceView {

    private SecurityDomainRealmEditor realms;

    public SecurityDomainView(final Dispatcher circuit,
            final ResourceDescription resourceDescription,
            final SecurityContext securityContext, final String title,
            final AddressTemplate addressTemplate) {
        super(circuit, resourceDescription, securityContext, title, addressTemplate);
        excludesFormAttributes("realms");

        // repackage realms inner attributes to show up in the ADD modal dialog
        ModelNode reqPropsDescription = resourceDescription.get(OPERATIONS).get(ADD).get(REQUEST_PROPERTIES);
        ModelNode realmDesc = reqPropsDescription.get("realms").get(VALUE_TYPE);
        reqPropsDescription.get("realm-name").set(realmDesc.get("realm")).get(NILLABLE).set(false);
        reqPropsDescription.get("realm-principal-transformer").set(realmDesc.get("principal-transformer"))
                .get(NILLABLE).set(true);
        reqPropsDescription.get("realm-role-decoder").set(realmDesc.get("role-decoder")).get(NILLABLE).set(true);
        reqPropsDescription.get("realm-role-mapper").set(realmDesc.get("role-mapper")).get(NILLABLE).set(true);
    }

    @Override
    public Map<String, Widget> additionalTabDetails() {
        Map<String, Widget> additionalWidgets = new HashMap<>();
        realms = new SecurityDomainRealmEditor(circuit, resourceDescription, securityContext);
        additionalWidgets.put("Realms", realms.asWidget());
        return additionalWidgets;
    }

    @Override
    public void update(final List<Property> models) {
        super.update(models);
        if (models.isEmpty()) {
            realms.clearValues();
        }
    }

    @Override
    protected void selectTableItem(final Property prop) {
        if (prop != null) {
            realms.update(prop);
        } else {
            realms.clearValues();
        }
    }

    @Override
    protected ModelNodeFormBuilder.FormAssets customFormOnAdd() {
        ModelNodeFormBuilder.FormAssets formAssets = new ModelNodeFormBuilder()
            .setResourceDescription(resourceDescription)
            .setCreateMode(true)
            .unsorted()
            .exclude("realms", "default-realm")
            .setSecurityContext(securityContext)
            .createValidators(true)
            .build();

        return formAssets;
    }

    @Override
    protected void onAddCallbackBeforeSubmit(final ModelNode payload) {

        // outflow-anonymous attribute is a boolean with default=false, even if the user doesn't set the field, the
        // payload sets it as false, then it requires the outflow-security-domains that is undefined
        // to prevent this error, if the outflow-anonymous is false, we remove from the payload
        boolean outflowAnonymousUndefined = payload.hasDefined("outflow-anonymous") && !payload.get("outflow-anonymous").asBoolean();
        if (outflowAnonymousUndefined)
            payload.remove("outflow-anonymous");

        // repackage the payload to create the "realms" attribute of type LIST
        // and create each "realm" from the repackaged description.
        ModelNode realm = new ModelNode();
        realm.get("realm").set(payload.get("realm-name"));
        realm.get("principal-transformer").set(payload.get("realm-principal-transformer"));
        realm.get("role-decoder").set(payload.get("realm-role-decoder"));
        realm.get("role-mapper").set(payload.get("realm-role-mapper"));
        ModelNode m = payload.get("realms").setEmptyList();
        m.add(realm);
        payload.get("default-realm").set(payload.get("realm-name"));
        payload.remove("realm-name");
        payload.remove("realm-principal-transformer");
        payload.remove("realm-role-decoder");
        payload.remove("realm-role-mapper");
    }

}
