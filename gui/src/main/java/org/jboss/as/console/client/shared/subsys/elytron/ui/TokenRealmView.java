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
import org.jboss.as.console.client.shared.subsys.elytron.store.ModifyComplexAttribute;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ComplexAttributeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class TokenRealmView extends ElytronGenericResourceView {

    private ModelNodeFormBuilder.FormAssets jwtForm;
    private ModelNodeFormBuilder.FormAssets oauth2Form;

    public TokenRealmView(final Dispatcher circuit,
            final ResourceDescription resourceDescription,
            final SecurityContext securityContext, final String title,
            final AddressTemplate addressTemplate) {
        super(circuit, resourceDescription, securityContext, title, addressTemplate);
        excludesFormAttributes("jwt", "oauth2-introspection");
    }

    @Override
    public Map<String, Widget> additionalTabDetails() {
        Map<String, Widget> additionalWidgets = new HashMap<>();
        jwtForm = new ComplexAttributeForm("jwt", securityContext, resourceDescription).build();
        oauth2Form = new ComplexAttributeForm("oauth2-introspection", securityContext, resourceDescription).build();

        jwtForm.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                circuit.dispatch(new ModifyComplexAttribute(ElytronStore.TOKEN_REALM_ADDRESS, "jwt",
                        selectionModel.getSelectedObject().getName(), jwtForm.getForm().getUpdatedEntity()));
            }

            @Override
            public void onCancel(final Object entity) {
                jwtForm.getForm().cancel();
            }
        });
        oauth2Form.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                circuit.dispatch(new ModifyComplexAttribute(ElytronStore.TOKEN_REALM_ADDRESS, "oauth2-introspection",
                        selectionModel.getSelectedObject().getName(), oauth2Form.getForm().getUpdatedEntity()));
            }

            @Override
            public void onCancel(final Object entity) {
                oauth2Form.getForm().cancel();
            }
        });

        jwtForm.getForm().setResetCallback(
                () -> circuit.dispatch(new ModifyComplexAttribute(ElytronStore.TOKEN_REALM_ADDRESS, "jwt",
                        selectionModel.getSelectedObject().getName(), new ModelNode().setEmptyList())));
        oauth2Form.getForm().setResetCallback(() -> circuit
                .dispatch(new ModifyComplexAttribute(ElytronStore.TOKEN_REALM_ADDRESS, "oauth2-introspection",
                        selectionModel.getSelectedObject().getName(), new ModelNode().setEmptyList())));

        additionalWidgets.put("JWT", jwtForm.asWidget());
        additionalWidgets.put("OAuth 2 Introspection", oauth2Form.asWidget());
        return additionalWidgets;
    }

    @Override
    public void update(final List<Property> models) {
        super.update(models);
        if (models.isEmpty()) {
            jwtForm.getForm().clearValues();
            oauth2Form.getForm().clearValues();
        }
    }

    @Override
    protected void selectTableItem(final Property prop) {
        if (prop != null) {
            jwtForm.getForm().edit(prop.getValue().get("jwt"));
            oauth2Form.getForm().edit(prop.getValue().get("oauth2-introspection"));
        } else {
            jwtForm.getForm().clearValues();
            oauth2Form.getForm().clearValues();
        }
    }

}
