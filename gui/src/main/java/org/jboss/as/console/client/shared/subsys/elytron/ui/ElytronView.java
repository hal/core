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

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.elytron.ElytronPresenter;
import org.jboss.as.console.client.shared.subsys.elytron.store.ElytronStore;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class ElytronView extends SuspendableViewImpl implements ElytronPresenter.MyView {

    private final Dispatcher circuit;
    private final ResourceDescriptionRegistry resourceDescriptionRegistry;
    private final SecurityFramework securityFramework;
    private ElytronPresenter presenter;
    private SSLView sslView;
    private RewriterView rewriterView;
    private DirContextView dirContextView;

    @Inject
    public ElytronView(final Dispatcher circuit, final ResourceDescriptionRegistry resourceDescriptionRegistry,
            final SecurityFramework securityFramework) {
        this.circuit = circuit;
        this.resourceDescriptionRegistry = resourceDescriptionRegistry;
        this.securityFramework = securityFramework;
    }

    @Override
    public Widget createWidget() {

        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());
        ResourceDescription rootDescription = resourceDescriptionRegistry.lookup(ElytronStore.ROOT_ADDRESS);
        
        DefaultTabLayoutPanel tabLayoutpanel = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        tabLayoutpanel.addStyleName("default-tabpanel");
        
        sslView = new SSLView(circuit, rootDescription, securityContext);
        rewriterView = new RewriterView(circuit, rootDescription, securityContext);
        dirContextView = new DirContextView(circuit, rootDescription, securityContext);

        tabLayoutpanel.add(sslView.asWidget(), "SSL", true);
        tabLayoutpanel.add(rewriterView.asWidget(), "Rewriter", true);
        tabLayoutpanel.add(dirContextView.asWidget(), "Dir Context", true);
        tabLayoutpanel.selectTab(0);

        return tabLayoutpanel;
    }

    @Override
    public void initSSL(final List<Property> keyStore, List<Property> keyManager, List<Property> serverSSLContext,
            List<Property> trustManager, List<Property> securityDomainModel, List<Property> securityPropertyModel) {
        sslView.updateKeyStore(keyStore);
        sslView.updateKeyManager(keyManager);
        sslView.updateServerSSLContext(serverSSLContext);
        sslView.updateTrustManager(trustManager);
        sslView.updateSecurityDomain(securityDomainModel);
        sslView.updateSecurityProperty(securityPropertyModel);
    }

    @Override
    public void initRewriters(final List<Property> aggregateNameRewriter, final List<Property> chainedNameRewriter,
            final List<Property> constantNameRewriter, final List<Property> customNameRewriter,
            final List<Property> regexNameValidatingRewriter, final List<Property> regexNameRewriter) {

        rewriterView.updateAggregateNameRewriter(aggregateNameRewriter);
        rewriterView.updateConstantNameRewriter(constantNameRewriter);
        rewriterView.updateChainedNameRewriterView(chainedNameRewriter);
        rewriterView.updateCustomNameRewriter(customNameRewriter);
        rewriterView.updateRegexNameValidatingRewriter(regexNameValidatingRewriter);
        rewriterView.updateRegexNameValidatingRewriter(regexNameRewriter);
    }

    @Override
    public void initDirContext(final List<Property> dirContextModels) {
        dirContextView.updateKeyStore(dirContextModels);
    }

    @Override
    public void setPresenter(final ElytronPresenter presenter) {
        this.presenter = presenter;
    }

}
