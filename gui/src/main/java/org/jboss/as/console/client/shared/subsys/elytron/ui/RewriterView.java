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
public class RewriterView {

    private Dispatcher circuit;
    private ResourceDescription rootDescription;
    private SecurityContext securityContext;

    private GenericRewriterView aggregateNameRewriterView;
    private ElytronGenericResourceView constantNameRewriterView;
    private GenericRewriterView chainedNameRewriterView;
    private ElytronGenericResourceView customNameRewriterView;
    private ElytronGenericResourceView regexNameValidatingRewriterView;
    private ElytronGenericResourceView regexNameRewriterView;

    public RewriterView(final Dispatcher circuit, final ResourceDescription rootDescription,
            final SecurityContext securityFramework) {
        this.circuit = circuit;
        this.rootDescription = rootDescription;
        this.securityContext = securityFramework;
    }

    public Widget asWidget() {

        ResourceDescription aggregateNameRewriterDescription = rootDescription.getChildDescription("aggregate-name-rewriter");
        ResourceDescription constantNameRewriterDescription = rootDescription.getChildDescription("constant-name-rewriter");
        ResourceDescription chainedNameRewriterDescription = rootDescription.getChildDescription("chained-name-rewriter");
        ResourceDescription customNameRewriterDescription = rootDescription.getChildDescription("custom-name-rewriter");
        ResourceDescription regexNameValidatingRewriterDescription = rootDescription.getChildDescription("regex-name-validating-rewriter");
        ResourceDescription regexNameRewriterDescription = rootDescription.getChildDescription("regex-name-rewriter");
        
        aggregateNameRewriterView = new GenericRewriterView(circuit, aggregateNameRewriterDescription, securityContext, "Aggregate Name Rewriter",
                ElytronStore.AGGREGATE_REWRITER_ADDRESS);
        
        constantNameRewriterView = new ElytronGenericResourceView(circuit, constantNameRewriterDescription, securityContext, "Constant Name Rewriter",
                ElytronStore.CONSTANT_REWRITER_ADDRESS);
        
        chainedNameRewriterView = new GenericRewriterView(circuit, chainedNameRewriterDescription, securityContext, "Chained Name Rewriter",
                ElytronStore.CHAINED_REWRITER_ADDRESS);
        
        customNameRewriterView = new ElytronGenericResourceView(circuit, customNameRewriterDescription, securityContext, "Custom Name Rewriter",
                ElytronStore.CUSTOM_REWRITER_ADDRESS);
        
        regexNameValidatingRewriterView = new ElytronGenericResourceView(circuit, regexNameRewriterDescription, securityContext, "Regex Name Validating Rewriter",
                ElytronStore.REGEX_NAME_VALIDATING_REWRITER_ADDRESS);
        
        regexNameRewriterView = new ElytronGenericResourceView(circuit, regexNameValidatingRewriterDescription, securityContext, "Regex Name Rewriter",
                ElytronStore.REGEX_NAME_REWRITER_ADDRESS);
        
        PagedView panel = new PagedView(true);
        panel.addPage("Aggregate Name Rewriter", aggregateNameRewriterView.asWidget());
        panel.addPage("Constant Name Rewriter", constantNameRewriterView.asWidget());
        panel.addPage("Chained Name Rewriter", chainedNameRewriterView.asWidget());
        panel.addPage("Custom Name Rewriter", customNameRewriterView.asWidget());
        panel.addPage("Regex Name Validating Rewriter", regexNameValidatingRewriterView.asWidget());
        panel.addPage("Regex Name Rewriter", regexNameRewriterView.asWidget());
        // default page
        panel.showPage(0);
        
        return panel.asWidget();
    }
    
    public void updateAggregateNameRewriter(final List<Property> models) {
        aggregateNameRewriterView.update(models);
    }
    
    public void updateConstantNameRewriter(final List<Property> models) {
        constantNameRewriterView.update(models);
    }
    
    public void updateChainedNameRewriterView(final List<Property> models) {
        chainedNameRewriterView.update(models);
    }
    
    public void updateCustomNameRewriter(final List<Property> models) {
        customNameRewriterView.update(models);
    }
    
    public void updateRegexNameValidatingRewriter(final List<Property> models) {
        regexNameValidatingRewriterView.update(models);
    }
    
    public void updateRegexNameRewriter(final List<Property> models) {
        regexNameRewriterView.update(models);
    }
    
}
