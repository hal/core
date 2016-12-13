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

import java.util.List;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.elytron.store.ElytronStore;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class TransformerView {

    private Dispatcher circuit;
    private ResourceDescription rootDescription;
    private SecurityContext securityContext;

    private GenericTransformerView aggregatePrincipalTransformerView;
    private ElytronGenericResourceView constantPrincipalTransformerView;
    private GenericTransformerView chainedPrincipalTransformerView;
    private ElytronGenericResourceView customPrincipalTransformerView;
    private ElytronGenericResourceView regexValidatingPrincipalTransformerView;
    private ElytronGenericResourceView regexPrincipalTransformerView;

    public TransformerView(final Dispatcher circuit, final ResourceDescription rootDescription,
            final SecurityContext securityFramework) {
        this.circuit = circuit;
        this.rootDescription = rootDescription;
        this.securityContext = securityFramework;
    }

    public Widget asWidget() {

        ResourceDescription aggregatePrincipalTransformerDescription = rootDescription.getChildDescription("aggregate-principal-transformer");
        ResourceDescription constantPrincipalTransformerDescription = rootDescription.getChildDescription("constant-principal-transformer");
        ResourceDescription chainedPrincipalTransformerDescription = rootDescription.getChildDescription("chained-principal-transformer");
        ResourceDescription customPrincipalTransformerDescription = rootDescription.getChildDescription("custom-principal-transformer");
        ResourceDescription regexValidatingPrincipalTransformerDescription = rootDescription.getChildDescription("regex-validating-principal-transformer");
        ResourceDescription regexPrincipalTransformerDescription = rootDescription.getChildDescription("regex-principal-transformer");
        
        aggregatePrincipalTransformerView = new GenericTransformerView(circuit, aggregatePrincipalTransformerDescription, securityContext, "Aggregate Principal Transformer",
                ElytronStore.AGGREGATE_TRANSFORMER_ADDRESS);
        
        constantPrincipalTransformerView = new ElytronGenericResourceView(circuit, constantPrincipalTransformerDescription, securityContext, "Constant Principal Transformer",
                ElytronStore.CONSTANT_TRANSFORMER_ADDRESS);
        
        chainedPrincipalTransformerView = new GenericTransformerView(circuit, chainedPrincipalTransformerDescription, securityContext, "Chained Principal Transformer",
                ElytronStore.CHAINED_TRANSFORMER_ADDRESS);
        
        customPrincipalTransformerView = new ElytronGenericResourceView(circuit, customPrincipalTransformerDescription, securityContext, "Custom Principal Transformer",
                ElytronStore.CUSTOM_TRANSFORMER_ADDRESS);
        
        regexValidatingPrincipalTransformerView = new ElytronGenericResourceView(circuit, regexPrincipalTransformerDescription, securityContext, "Regex Validating Principal Transformer",
                ElytronStore.REGEX_VALIDATING_TRANSFORMER_ADDRESS);
        
        regexPrincipalTransformerView = new ElytronGenericResourceView(circuit, regexValidatingPrincipalTransformerDescription, securityContext, "Regex Principal Transformer",
                ElytronStore.REGEX_TRANSFORMER_ADDRESS);
        
        PagedView panel = new PagedView(true);
        panel.addPage("Aggregate", aggregatePrincipalTransformerView.asWidget());
        panel.addPage("Constant", constantPrincipalTransformerView.asWidget());
        panel.addPage("Chained", chainedPrincipalTransformerView.asWidget());
        panel.addPage("Custom", customPrincipalTransformerView.asWidget());
        panel.addPage("Regex Validating", regexValidatingPrincipalTransformerView.asWidget());
        panel.addPage("Regex", regexPrincipalTransformerView.asWidget());
        // default page
        panel.showPage(0);
        
        return panel.asWidget();
    }
    
    public void updateAggregatePrincipalTransformer(final List<Property> models) {
        aggregatePrincipalTransformerView.update(models);
    }
    
    public void updateConstantPrincipalTransformer(final List<Property> models) {
        constantPrincipalTransformerView.update(models);
    }
    
    public void updateChainedPrincipalTransformer(final List<Property> models) {
        chainedPrincipalTransformerView.update(models);
    }
    
    public void updateCustomPrincipalTransformer(final List<Property> models) {
        customPrincipalTransformerView.update(models);
    }
    
    public void updateRegexValidatingPrincipalTransformer(final List<Property> models) {
        regexValidatingPrincipalTransformerView.update(models);
    }
    
    public void updateRegexPrincipalTransformer(final List<Property> models) {
        regexPrincipalTransformerView.update(models);
    }
    
}
