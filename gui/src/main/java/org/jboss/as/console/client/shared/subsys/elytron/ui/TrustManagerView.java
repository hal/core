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
public class TrustManagerView extends ElytronGenericResourceView {

    private static final String CERTIFICATE_REVOCATION_LIST = "certificate-revocation-list";
    private ModelNodeFormBuilder.FormAssets certificateRevocationListForm;

    public TrustManagerView(final Dispatcher circuit,
            final ResourceDescription resourceDescription,
            final SecurityContext securityContext, final String title,
            final AddressTemplate addressTemplate) {
        super(circuit, resourceDescription, securityContext, title, addressTemplate);
        excludesFormAttributes(CERTIFICATE_REVOCATION_LIST);
    }

    @Override
    public Map<String, Widget> additionalTabDetails() {
        Map<String, Widget> additionalWidgets = new HashMap<>();
        certificateRevocationListForm = new ComplexAttributeForm(CERTIFICATE_REVOCATION_LIST, securityContext, resourceDescription).build();

        certificateRevocationListForm.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                circuit.dispatch(new ModifyComplexAttribute(ElytronStore.TRUST_MANAGER_ADDRESS,
                        CERTIFICATE_REVOCATION_LIST,
                        selectionModel.getSelectedObject().getName(), certificateRevocationListForm.getForm().getUpdatedEntity()));
            }

            @Override
            public void onCancel(final Object entity) {
                certificateRevocationListForm.getForm().cancel();
            }
        });

        certificateRevocationListForm.getForm()
                .setResetCallback(() -> circuit.dispatch(new ModifyComplexAttribute(ElytronStore.TRUST_MANAGER_ADDRESS,
                        CERTIFICATE_REVOCATION_LIST, selectionModel.getSelectedObject().getName(),
                        new ModelNode().setEmptyList())));

        additionalWidgets.put("Certificate Revocation List", certificateRevocationListForm.asWidget());
        return additionalWidgets;
    }

    @Override
    public void update(final List<Property> models) {
        super.update(models);
        if (models.isEmpty()) {
            certificateRevocationListForm.getForm().clearValues();
        }
    }

    @Override
    protected void selectTableItem(final Property prop) {
        if (prop != null) {
            certificateRevocationListForm.getForm().edit(prop.getValue().get(CERTIFICATE_REVOCATION_LIST));
        } else {
            certificateRevocationListForm.getForm().clearValues();
        }
    }

}
