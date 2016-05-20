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
package org.jboss.as.console.client.shared.subsys.ws;

import java.util.Map;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.as.console.client.shared.subsys.ws.store.ModifyProviderConfiguration;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.gwt.circuit.Dispatcher;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Heiko Braun
 * @date 1/11/12
 */
public class ProviderEditor implements IsWidget {

    private final Dispatcher circuit;
    private final SecurityContext securityContext;
    private final ResourceDescription resourceDescription;
    private ModelNodeFormBuilder.FormAssets providerForm;

    public ProviderEditor(Dispatcher circuit, SecurityContext securityContext,
            ResourceDescription resourceDescription) {
        this.circuit = circuit;
        this.securityContext = securityContext;
        this.resourceDescription = resourceDescription;
    }

    public Widget asWidget() {

        final FormCallback callback = new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(Map changeSet) {
                circuit.dispatch(new ModifyProviderConfiguration(changeSet));
            }

            @Override
            public void onCancel(Object entity) {
                providerForm.getForm().cancel();
            }
        };

        // commmon
        ModelNodeFormBuilder commonAtts = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext);

        providerForm = commonAtts.build();
        providerForm.getForm().setToolsCallback(callback);

        SimpleLayout layout = new SimpleLayout()
                .setPlain(true)
                .setTitle("Provider")
                .setHeadline("Web Services Provider")
                .setDescription(Console.CONSTANTS.subsys_ws_desc())
                .addContent("", providerForm.asWidget()
                );

        return layout.build();
    }

    void update(ModelNode model) {
        providerForm.getForm().edit(model);
    }
}
