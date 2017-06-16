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
package org.jboss.as.console.client.shared.subsys.elytron.ui.factory;

import java.util.List;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.elytron.ElytronFactoryPresenter;
import org.jboss.as.console.client.shared.subsys.elytron.store.ElytronStore;
import org.jboss.as.console.client.shared.subsys.elytron.ui.TransformerView;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class ElytronFactoryView extends SuspendableViewImpl implements ElytronFactoryPresenter.MyView {

    private final Dispatcher circuit;
    private final ResourceDescriptionRegistry resourceDescriptionRegistry;
    private final SecurityFramework securityFramework;
    private ElytronFactoryPresenter presenter;
    private FactoryView factoryView;
    private TransformerView transformerView;

    @Inject
    public ElytronFactoryView(final Dispatcher circuit, final ResourceDescriptionRegistry resourceDescriptionRegistry,
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

        factoryView = new FactoryView(circuit, rootDescription, securityContext);
        transformerView = new TransformerView(circuit, rootDescription, securityContext);

        tabLayoutpanel.add(factoryView.asWidget(), "Factory", true);
        tabLayoutpanel.add(transformerView.asWidget(), "Transformer", true);
        tabLayoutpanel.selectTab(0);

        return tabLayoutpanel;
    }

    @Override
    public void setPresenter(final ElytronFactoryPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void initFactory(final List<Property> aggregateHttpServerMechanismFactory,
            final List<Property> aggregateSaslServerFactory,
            final List<Property> configurableHttpServerMechanismFactory,
            final List<Property> configurableSaslServerFactory, final List<Property> customCredentialSecurityFactory,
            final List<Property> httpAuthenticationFactory, final List<Property> kerberosSecurityFactory,
            final List<Property> mechanismProviderFilteringSaslServerFactory,
            final List<Property> providerHttpServerMechanismFactory, final List<Property> providerSaslServerFactory,
            final List<Property> saslAuthenticationFactory,
            final List<Property> serviceLoaderHttpServerMechanismFactory,
            final List<Property> serviceLoaderSaslServerFactory) {

        factoryView.updateAggregateHttpServerMechanismFactoryView(aggregateHttpServerMechanismFactory);
        factoryView.updateAggregateSaslServerFactoryView(aggregateSaslServerFactory);
        factoryView.updateConfigurableHttpServerMechanismFactoryView(configurableHttpServerMechanismFactory);
        factoryView.updateConfigurableSaslServerFactoryView(configurableSaslServerFactory);
        factoryView.updateCustomCredentialSecurityFactoryView(customCredentialSecurityFactory);
        factoryView.updateHttpAuthenticationFactoryView(httpAuthenticationFactory);
        factoryView.updateKerberosSecurityFactoryView(kerberosSecurityFactory);
        factoryView.updateMechanismProviderFilteringSaslServerFactoryView(mechanismProviderFilteringSaslServerFactory);
        factoryView.updateProviderHttpServerMechanismFactoryView(providerHttpServerMechanismFactory);
        factoryView.updateProviderSaslServerFactoryView(providerSaslServerFactory);
        factoryView.updateSaslAuthenticationFactoryView(saslAuthenticationFactory);
        factoryView.updateServiceLoaderHttpServerMechanismFactoryView(serviceLoaderHttpServerMechanismFactory);
        factoryView.updateServiceLoaderSaslServerFactoryView(serviceLoaderSaslServerFactory);

    }

    @Override
    public void initTransformers(final List<Property> aggregatePrincTransf, final List<Property> chainedPrincTransf,
            final List<Property> constantPrincTransf, final List<Property> customPrincTransf,
            final List<Property> regexValidatingPrincTransf, final List<Property> regexPrincTransf) {

        transformerView.updateAggregatePrincipalTransformer(aggregatePrincTransf);
        transformerView.updateConstantPrincipalTransformer(constantPrincTransf);
        transformerView.updateChainedPrincipalTransformer(chainedPrincTransf);
        transformerView.updateCustomPrincipalTransformer(customPrincTransf);
        transformerView.updateRegexValidatingPrincipalTransformer(regexValidatingPrincTransf);
        transformerView.updateRegexPrincipalTransformer(regexPrincTransf);
    }


}
