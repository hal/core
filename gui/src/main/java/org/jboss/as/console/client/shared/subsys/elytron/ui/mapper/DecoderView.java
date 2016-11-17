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
package org.jboss.as.console.client.shared.subsys.elytron.ui.mapper;

import java.util.List;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.elytron.store.ElytronStore;
import org.jboss.as.console.client.shared.subsys.elytron.ui.ElytronGenericResourceView;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class DecoderView {

    private Dispatcher circuit;
    private ResourceDescription rootDescription;
    private SecurityContext securityContext;

    private ElytronGenericResourceView aggregatePrincipal;
    private ElytronGenericResourceView concatenatingPrincipal;
    private ElytronGenericResourceView constantPrincipal;
    private ElytronGenericResourceView customPrincipal;
    private ElytronGenericResourceView x500AttributePrincipal;
    private ElytronGenericResourceView customRole;
    private ElytronGenericResourceView simpleRole;

    public DecoderView(final Dispatcher circuit, final ResourceDescription rootDescription,
            final SecurityContext securityFramework) {
        this.circuit = circuit;
        this.rootDescription = rootDescription;
        this.securityContext = securityFramework;
    }
    public Widget asWidget() {

        aggregatePrincipal = new ElytronGenericResourceView(circuit,
                rootDescription.getChildDescription("aggregate-principal-decoder"),
                securityContext, "Aggregate Principal Decoder", ElytronStore.AGGREGATE_PRINCIPAL_DECODER_ADDRESS);

        concatenatingPrincipal = new ElytronGenericResourceView(circuit,
                rootDescription.getChildDescription("concatenating-principal-decoder"),
                securityContext, "Concatenating Principal Decoder", ElytronStore.CONCATENATING_PRINCIPAL_DECODER_ADDRESS);

        constantPrincipal= new ElytronGenericResourceView(circuit,
                rootDescription.getChildDescription("constant-principal-decoder"),
                securityContext, "Constant Principal Decoder", ElytronStore.CONSTANT_PRINCIPAL_DECODER_ADDRESS);
        
        customPrincipal= new ElytronGenericResourceView(circuit,
                rootDescription.getChildDescription("custom-principal-decoder"),
                securityContext, "Custom Principal Decoder", ElytronStore.CUSTOM_PRINCIPAL_DECODER_ADDRESS);

        x500AttributePrincipal= new ElytronGenericResourceView(circuit,
                rootDescription.getChildDescription("x500-attribute-principal-decoder"),
                securityContext, "X500 Attribute Principal Decoder", ElytronStore.X500_PRINCIPAL_DECODER_ADDRESS);

        customRole = new ElytronGenericResourceView(circuit,
                rootDescription.getChildDescription("custom-role-decoder"),
                securityContext, "Custom Role Decoder", ElytronStore.CUSTOM_ROLE_DECODER_ADDRESS);

        simpleRole = new ElytronGenericResourceView(circuit,
                rootDescription.getChildDescription("simple-role-decoder"),
                securityContext, "Simple Role Decoder", ElytronStore.SIMPLE_ROLE_DECODER_ADDRESS);

        PagedView panel = new PagedView(true);
        panel.addPage("Aggregate Principal Decoder", aggregatePrincipal.asWidget());
        panel.addPage("Concatenating Principal Decoder", concatenatingPrincipal.asWidget());
        panel.addPage("Constant Principal Decoder", constantPrincipal.asWidget());
        panel.addPage("Custom Principal Decoder", customPrincipal.asWidget());
        panel.addPage("X500 Attribute Principal Decoder", x500AttributePrincipal.asWidget());
        panel.addPage("Custom Role Decoder", customRole.asWidget());
        panel.addPage("Simple Role Decoder", simpleRole.asWidget());
        // default page
        panel.showPage(0);

        return panel.asWidget();
    }

    public void updateAggregatePrincipal(List<Property> models) {
        this.aggregatePrincipal.update(models);
    }

    public void updateConcatenatingPrincipal(List<Property> models) {
        this.concatenatingPrincipal.update(models);
    }

    public void updateConstantPrincipal(List<Property> models) {
        this.constantPrincipal.update(models);
    }

    public void updateCustomPrincipal(List<Property> models) {
        this.customPrincipal.update(models);
    }

    public void updateX500AttributePrincipal(List<Property> models) {
        this.x500AttributePrincipal.update(models);
    }

    public void updateCustomRole(List<Property> models) {
        this.customRole.update(models);
    }

    public void updateSimpleRole(List<Property> models) {
        this.simpleRole.update(models);
    }
}
