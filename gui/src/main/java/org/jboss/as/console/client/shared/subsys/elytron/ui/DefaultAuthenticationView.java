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

import java.util.Map;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.shared.subsys.elytron.store.ModifyResourceGeneric;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.gwt.circuit.Dispatcher;

import static org.jboss.dmr.client.ModelDescriptionConstants.DESCRIPTION;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class DefaultAuthenticationView {

    protected final Dispatcher circuit;
    protected final ResourceDescription resourceDescription;
    protected String title;
    protected AddressTemplate addressTemplate;
    protected final SecurityContext securityContext;

    protected ModelNodeFormBuilder.FormAssets formAssets;

    public DefaultAuthenticationView(final Dispatcher circuit, final ResourceDescription resourceDescription,
            final SecurityContext securityContext, String title, AddressTemplate addressTemplate) {
        this.circuit = circuit;
        this.securityContext = securityContext;
        this.resourceDescription = resourceDescription;
        this.title = title;
        this.addressTemplate = addressTemplate;
    }

    public Widget asWidget() {

        ModelNodeFormBuilder formBuilder = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext);
        formAssets = formBuilder.build();

        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                circuit.dispatch(new ModifyResourceGeneric(addressTemplate, null, changeset));
            }

            @Override
            public void onCancel(final Object entity) {
                formAssets.getForm().cancel();
            }
        });

        VerticalPanel formPanel = new VerticalPanel();
        formPanel.setStyleName("fill-layout-width");
        formPanel.add(formAssets.getHelp().asWidget());
        formPanel.add(formAssets.getForm().asWidget());

        OneToOneLayout layout = new OneToOneLayout()
                .setPlain(true)
                .setHeadline(title)
                .setDescription(resourceDescription.get(DESCRIPTION).asString())
                .addDetail(Console.CONSTANTS.common_label_attributes(), formPanel);

        return layout.build();
    }

    public void update(final ModelNode model) {
        formAssets.getForm().edit(model);
    }

}
