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
public class FactoryView {

    private Dispatcher circuit;
    private ResourceDescription rootDescription;
    private SecurityContext securityContext;

    private ElytronGenericResourceView aggregateHttpServerMechanismFactoryView;
    private ElytronGenericResourceView aggregateSaslServerFactoryView;
    private ElytronGenericResourceView configurableHttpServerMechanismFactoryView;
    private ConfigurableSaslServerFactoryView configurableSaslServerFactoryView;
    private ElytronGenericResourceView customCredentialSecurityFactoryView;
    private GenericAuthenticationFactoryView httpAuthenticationFactoryView;
    private ElytronGenericResourceView kerberosSecurityFactoryView;
    private MechanismProviderFilteringSaslSserverFactoryView mechanismProviderFilteringSaslServerFactoryView;
    private ElytronGenericResourceView providerHttpServerMechanismFactoryView;
    private ElytronGenericResourceView providerSaslServerFactoryView;
    private GenericAuthenticationFactoryView saslAuthenticationFactoryView;
    private ElytronGenericResourceView serviceLoaderHttpServerMechanismFactoryView;
    private ElytronGenericResourceView serviceLoaderSaslServerFactoryView;

    public FactoryView(final Dispatcher circuit, final ResourceDescription rootDescription,
            final SecurityContext securityFramework) {
        this.circuit = circuit;
        this.rootDescription = rootDescription;
        this.securityContext = securityFramework;
    }

    public Widget asWidget() {

        ResourceDescription aggregatehttpservermechanismfactoryDescription = rootDescription
                .getChildDescription("aggregate-http-server-mechanism-factory");
        ResourceDescription aggregatesaslserverfactoryDescription = rootDescription
                .getChildDescription("aggregate-sasl-server-factory");
        ResourceDescription configurablehttpservermechanismfactoryDescription = rootDescription
                .getChildDescription("configurable-http-server-mechanism-factory");
        ResourceDescription configurablesaslserverfactoryDescription = rootDescription
                .getChildDescription("configurable-sasl-server-factory");
        ResourceDescription customcredentialsecurityfactoryDescription = rootDescription
                .getChildDescription("custom-credential-security-factory");
        ResourceDescription httpauthenticationfactoryDescription = rootDescription
                .getChildDescription("http-authentication-factory");
        ResourceDescription kerberossecurityfactoryDescription = rootDescription
                .getChildDescription("kerberos-security-factory");
        ResourceDescription mechanismproviderfilteringsaslserverfactoryDescription = rootDescription
                .getChildDescription("mechanism-provider-filtering-sasl-server-factory");
        ResourceDescription providerhttpservermechanismfactoryDescription = rootDescription
                .getChildDescription("provider-http-server-mechanism-factory");
        ResourceDescription providersaslserverfactoryDescription = rootDescription
                .getChildDescription("provider-sasl-server-factory");
        ResourceDescription saslauthenticationfactoryDescription = rootDescription
                .getChildDescription("sasl-authentication-factory");
        ResourceDescription serviceloaderhttpservermechanismfactoryDescription = rootDescription
                .getChildDescription("service-loader-http-server-mechanism-factory");
        ResourceDescription serviceloadersaslserverfactoryDescription = rootDescription
                .getChildDescription("service-loader-sasl-server-factory");

        aggregateHttpServerMechanismFactoryView = new ElytronGenericResourceView(circuit,
                aggregatehttpservermechanismfactoryDescription, securityContext, "Aggregate HTTP Server Mechanism",
                ElytronStore.AGGREGATE_HTTP_SERVER_MECHANISM_FACTORY_ADDRESS);
        
        aggregateSaslServerFactoryView = new ElytronGenericResourceView(circuit, aggregatesaslserverfactoryDescription,
                securityContext, "Aggregate SASL Server", ElytronStore.AGGREGATE_SASL_SERVER_FACTORY_ADDRESS);
        
        configurableHttpServerMechanismFactoryView = new ConfigurableHttpServerMechanismFactoryView(circuit,
                configurablehttpservermechanismfactoryDescription, securityContext,
                "Configurable HTTP Server Mechanism", ElytronStore.CONFIGURABLE_HTTP_SERVER_MECHANISM_FACTORY_ADDRESS);
        
        configurableSaslServerFactoryView = new ConfigurableSaslServerFactoryView(circuit,
                configurablesaslserverfactoryDescription, securityContext, "Configurable SASL Server",
                ElytronStore.CONFIGURABLE_SASL_SERVER_FACTORY_ADDRESS);
        
        customCredentialSecurityFactoryView = new ElytronGenericResourceView(circuit,
                customcredentialsecurityfactoryDescription, securityContext, "Custom Credential Security",
                ElytronStore.CUSTOM_CREDENTIAL_SECURITY_FACTORY_ADDRESS);
        
        httpAuthenticationFactoryView = new GenericAuthenticationFactoryView(circuit, httpauthenticationfactoryDescription,
                securityContext, "HTTP Authentication", ElytronStore.HTTP_AUTHENTICATION_FACTORY_ADDRESS);
        
        kerberosSecurityFactoryView = new ElytronGenericResourceView(circuit, kerberossecurityfactoryDescription,
                securityContext, "Kerberos Security", ElytronStore.KERBEROS_SECURITY_FACTORY_ADDRESS);
        
        mechanismProviderFilteringSaslServerFactoryView = new MechanismProviderFilteringSaslSserverFactoryView(circuit,
                mechanismproviderfilteringsaslserverfactoryDescription, securityContext,
                "Mechanism Provider Filtering Sasl Server",
                ElytronStore.MECHANISM_PROVIDER_FILTERING_SASL_SERVER_FACTORY_ADDRESS);
        
        providerHttpServerMechanismFactoryView = new ElytronGenericResourceView(circuit,
                providerhttpservermechanismfactoryDescription, securityContext, "Provider HTTP Server Mechanism",
                ElytronStore.PROVIDER_HTTP_SERVER_MECHANISM_FACTORY_ADDRESS);
        
        providerSaslServerFactoryView = new ElytronGenericResourceView(circuit, providersaslserverfactoryDescription,
                securityContext, "Provider SASL Server", ElytronStore.PROVIDER_SASL_SERVER_FACTORY_ADDRESS);
        
        saslAuthenticationFactoryView = new GenericAuthenticationFactoryView(circuit, saslauthenticationfactoryDescription,
                securityContext, "SASL Authentication", ElytronStore.SASL_AUTHENTICATION_FACTORY_ADDRESS);
        
        serviceLoaderHttpServerMechanismFactoryView = new ElytronGenericResourceView(circuit,
                serviceloaderhttpservermechanismfactoryDescription, securityContext,
                "Service Loader HTTP Server Mechanism",
                ElytronStore.SERVICE_LOADER_HTTP_SERVER_MECHANISM_FACTORY_ADDRESS);
        
        serviceLoaderSaslServerFactoryView = new ElytronGenericResourceView(circuit,
                serviceloadersaslserverfactoryDescription, securityContext, "Service Loader SASL Server",
                ElytronStore.SERVICE_LOADER_SASL_SERVER_FACTORY_ADDRESS);

        PagedView panel = new PagedView(true);
        panel.addPage("Aggregate HTTP Server", aggregateHttpServerMechanismFactoryView.asWidget());
        panel.addPage("Aggregate SASL Server", aggregateSaslServerFactoryView.asWidget());
        panel.addPage("Configurable HTTP Server", configurableHttpServerMechanismFactoryView.asWidget());
        panel.addPage("Configurable SASL Server", configurableSaslServerFactoryView.asWidget());
        panel.addPage("Custom Credential Security", customCredentialSecurityFactoryView.asWidget());
        panel.addPage("HTTP Authentication", httpAuthenticationFactoryView.asWidget());
        panel.addPage("Kerberos Security", kerberosSecurityFactoryView.asWidget());
        panel.addPage("Mechanism Provider Filtering Sasl",
                mechanismProviderFilteringSaslServerFactoryView.asWidget());
        panel.addPage("Provider HTTP Server Mechanism", providerHttpServerMechanismFactoryView.asWidget());
        panel.addPage("Provider SASL Server", providerSaslServerFactoryView.asWidget());
        panel.addPage("SASL Authentication", saslAuthenticationFactoryView.asWidget());
        panel.addPage("Service Loader HTTP Server", serviceLoaderHttpServerMechanismFactoryView.asWidget());
        panel.addPage("Service Loader SASL Server", serviceLoaderSaslServerFactoryView.asWidget());
        // default page
        panel.showPage(0);

        return panel.asWidget();
    }

    public void updateServiceLoaderSaslServerFactoryView(List<Property> models) {
        this.serviceLoaderSaslServerFactoryView.update(models);
    }

    public void updateServiceLoaderHttpServerMechanismFactoryView(List<Property> models) {
        this.serviceLoaderHttpServerMechanismFactoryView.update(models);
    }

    public void updateSaslAuthenticationFactoryView(List<Property> models) {
        this.saslAuthenticationFactoryView.update(models);
    }

    public void updateProviderSaslServerFactoryView(List<Property> models) {
        this.providerSaslServerFactoryView.update(models);
    }

    public void updateProviderHttpServerMechanismFactoryView(List<Property> models) {
        this.providerHttpServerMechanismFactoryView.update(models);
    }

    public void updateMechanismProviderFilteringSaslServerFactoryView(List<Property> models) {
        this.mechanismProviderFilteringSaslServerFactoryView.update(models);
    }

    public void updateKerberosSecurityFactoryView(List<Property> models) {
        this.kerberosSecurityFactoryView.update(models);
    }

    public void updateHttpAuthenticationFactoryView(List<Property> models) {
        this.httpAuthenticationFactoryView.update(models);
    }

    public void updateCustomCredentialSecurityFactoryView(List<Property> models) {
        this.customCredentialSecurityFactoryView.update(models);
    }

    public void updateConfigurableSaslServerFactoryView(List<Property> models) {
        this.configurableSaslServerFactoryView.update(models);
    }

    public void updateConfigurableHttpServerMechanismFactoryView(List<Property> models) {
        this.configurableHttpServerMechanismFactoryView.update(models);
    }

    public void updateAggregateSaslServerFactoryView(List<Property> models) {
        this.aggregateSaslServerFactoryView.update(models);
    }

    public void updateAggregateHttpServerMechanismFactoryView(List<Property> models) {
        this.aggregateHttpServerMechanismFactoryView.update(models);
    }
}
