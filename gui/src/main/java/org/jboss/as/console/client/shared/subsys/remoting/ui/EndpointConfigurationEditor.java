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

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.shared.subsys.remoting.store.ModifyEndpointConfiguration;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.DESCRIPTION;

/**
 * @author Harald Pehl
 */
class EndpointConfigurationEditor implements IsWidget {

    private final Dispatcher circuit;
    private final SecurityContext securityContext;
    private final ResourceDescription resourceDescription;
    private ModelNodeFormBuilder.FormAssets commonForm;


    private final String[] SECURITY = new String[] {
            "auth-realm",
            "authentication-retries",
            "authorize-id",
            "sasl-protocol"
    };

    private final String[] CHANNELS = new String[] {
            "max-inbound-channels",
            "max-inbound-message-size",
            "max-inbound-messages",
            "max-outbound-channels",
            "max-outbound-message-size",
            "max-outbound-messages",
    };
    private ModelNodeFormBuilder.FormAssets channelForm;
    private ModelNodeFormBuilder.FormAssets secForm;

    public EndpointConfigurationEditor(Dispatcher circuit, SecurityContext securityContext,
                                       ResourceDescription resourceDescription) {
        this.circuit = circuit;
        this.securityContext = securityContext;
        this.resourceDescription = resourceDescription;
    }

    @Override
    public Widget asWidget() {

        final FormCallback callback = new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(Map changeSet) {
                circuit.dispatch(new ModifyEndpointConfiguration(changeSet));
            }

            @Override
            public void onCancel(Object entity) {
                commonForm.getForm().cancel();
            }
        };

        // commmon
        ModelNodeFormBuilder commonAtts = new ModelNodeFormBuilder()
                .setConfigOnly()
                .exclude(SECURITY, CHANNELS)
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext);

        commonForm = commonAtts.build();
        commonForm.getForm().setToolsCallback(callback);

        // channel
        ModelNodeFormBuilder channelAtts = new ModelNodeFormBuilder()
                .setConfigOnly()
                .include(CHANNELS)
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext);

        channelForm = channelAtts.build();
        channelForm.getForm().setToolsCallback(callback);


         // security
        ModelNodeFormBuilder secAtts = new ModelNodeFormBuilder()
                .setConfigOnly()
                .include(SECURITY)
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext);

        secForm = secAtts.build();
        secForm.getForm().setToolsCallback(callback);

        OneToOneLayout layout = new OneToOneLayout()
                .setPlain(true)
                .setHeadline("Endpoint Configuration")
                .setDescription(resourceDescription.get(DESCRIPTION).asString())
                .addDetail("Attributes", commonForm.asWidget())
                .addDetail("Security", secForm.asWidget())
                .addDetail("Channels", channelForm.asWidget());

        return layout.build();
    }

    void update(ModelNode model) {
        commonForm.getForm().edit(model);
        secForm.getForm().edit(model);
        channelForm.getForm().edit(model);
    }
}
