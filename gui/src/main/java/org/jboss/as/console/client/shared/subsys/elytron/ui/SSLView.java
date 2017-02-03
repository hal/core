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
public class SSLView {

    private Dispatcher circuit;
    private ResourceDescription rootDescription;
    private SecurityContext securityContext;

    private GenericStoreView keystoreView;
    private GenericStoreView credentialstoreView;
    private ElytronGenericResourceView filteringKeystoreView;
    private LdapKeystoreView ldapKeystoreView;
    private GenericStoreView keyManagerView;
    private ElytronGenericResourceView serverSSLContextView;
    private ElytronGenericResourceView clientSSLContextView;
    private ElytronGenericResourceView trustManagerView;
    private SecurityDomainView securityDomainView;
    private ElytronGenericResourceView securityPropertyView;
    private ProviderLoaderView providerLoaderView;
    private ElytronGenericResourceView aggregateProvidersView;

    public SSLView(final Dispatcher circuit, final ResourceDescription rootDescription,
            final SecurityContext securityFramework) {
        this.circuit = circuit;
        this.rootDescription = rootDescription;
        this.securityContext = securityFramework;
    }

    public Widget asWidget() {

        ResourceDescription keyStoreDescription = rootDescription.getChildDescription("key-store");
        ResourceDescription credentialStoreDescription = rootDescription.getChildDescription("credential-store");
        ResourceDescription filteringKeyStoreDescription = rootDescription.getChildDescription("filtering-key-store");
        ResourceDescription ldapKeyStoreDescription = rootDescription.getChildDescription("ldap-key-store");
        ResourceDescription keyManagerDescription = rootDescription.getChildDescription("key-managers");
        ResourceDescription serverSSLContextDescription = rootDescription.getChildDescription("server-ssl-context");
        ResourceDescription clientSSLContextDescription = rootDescription.getChildDescription("client-ssl-context");
        ResourceDescription trustManagersDescription = rootDescription.getChildDescription("trust-managers");
        ResourceDescription securityDomainDescription = rootDescription.getChildDescription("security-domain");
        ResourceDescription securityPropertyDescription = rootDescription.getChildDescription("security-property");
        ResourceDescription providerLoaderDescription = rootDescription.getChildDescription("provider-loader");
        ResourceDescription aggregateProvidersDescription = rootDescription.getChildDescription("aggregate-providers");

        keystoreView = new GenericStoreView(circuit, keyStoreDescription, securityContext, "Key Store",
                ElytronStore.KEY_STORE_ADDRESS);

        credentialstoreView = new GenericStoreView(circuit, credentialStoreDescription, securityContext, "Credential Store",
                ElytronStore.CREDENTIAL_STORE_ADDRESS);

        filteringKeystoreView = new ElytronGenericResourceView(circuit, filteringKeyStoreDescription, securityContext, "Filtering Key Store",
                ElytronStore.FILTERING_KEY_STORE_ADDRESS);

        ldapKeystoreView = new LdapKeystoreView(circuit, ldapKeyStoreDescription, securityContext, "LDAP Key Store",
                ElytronStore.LDAP_KEY_STORE_ADDRESS);

        keyManagerView = new GenericStoreView(circuit, keyManagerDescription, securityContext, "Key Manager",
                ElytronStore.KEY_MANAGER_ADDRESS);

        serverSSLContextView = new ElytronGenericResourceView(circuit, serverSSLContextDescription, securityContext,
                "Server SSL Context", ElytronStore.SERVER_SSL_CONTEXT_ADDRESS);

        clientSSLContextView = new ElytronGenericResourceView(circuit, clientSSLContextDescription, securityContext,
                "Client SSL Context", ElytronStore.CLIENT_SSL_CONTEXT_ADDRESS);

        trustManagerView = new ElytronGenericResourceView(circuit, trustManagersDescription, securityContext,
                "Trust Manager", ElytronStore.TRUST_MANAGER_ADDRESS);

        securityDomainView = new SecurityDomainView(circuit, securityDomainDescription, securityContext,
                "Security Domain", ElytronStore.SECURITY_DOMAIN_ADDRESS);

        securityPropertyView = new ElytronGenericResourceView(circuit, securityPropertyDescription, securityContext,
                "Security Property", ElytronStore.SECURITY_PROPERTY_ADDRESS);

        providerLoaderView = new ProviderLoaderView(circuit, providerLoaderDescription, securityContext,
                "Provider Loader", ElytronStore.PROVIDER_LOADER_ADDRESS);

        aggregateProvidersView = new ElytronGenericResourceView(circuit, aggregateProvidersDescription, securityContext,
                "Aggregate Providers", ElytronStore.AGGREGATE_PROVIDERS_ADDRESS);

        PagedView panel = new PagedView(true);
        panel.addPage("Key Store", keystoreView.asWidget());
        panel.addPage("Credential Store", credentialstoreView.asWidget());
        panel.addPage("Filtering Key Store", filteringKeystoreView.asWidget());
        panel.addPage("Ldap Key Store", ldapKeystoreView.asWidget());
        panel.addPage("Key Manager", keyManagerView.asWidget());
        panel.addPage("Server SSL Context", serverSSLContextView.asWidget());
        panel.addPage("Client SSL Context", clientSSLContextView.asWidget());
        panel.addPage("Trust Manager", trustManagerView.asWidget());
        panel.addPage("Security Domain", securityDomainView.asWidget());
        panel.addPage("Security Property", securityPropertyView.asWidget());
        panel.addPage("Provider Loader", providerLoaderView.asWidget());
        panel.addPage("Aggregate Providers", aggregateProvidersView.asWidget());
        // default page
        panel.showPage(0);

        return panel.asWidget();
    }

    public void updateKeyStore(List<Property> keyStore) {
        keystoreView.update(keyStore);
    }

    public void updateCredentialStore(List<Property> models) {
        credentialstoreView.update(models);
    }

    public void updateFilteringKeyStore(List<Property> models) {
        filteringKeystoreView.update(models);
    }

    public void updateLdapKeyStore(List<Property> models) {
        ldapKeystoreView.update(models);
    }

    public void updateKeyManager(List<Property> keyManager) {
        keyManagerView.update(keyManager);
    }

    public void updateServerSSLContext(List<Property> serverSSLContext) {
        serverSSLContextView.update(serverSSLContext);
    }

    public void updateClientSSLContext(List<Property> models) {
        clientSSLContextView.update(models);
    }

    public void updateTrustManager(List<Property> trustManager) {
        trustManagerView.update(trustManager);
    }

    public void updateSecurityDomain(List<Property> models) {
        securityDomainView.update(models);
    }

    public void updateSecurityProperty(List<Property> models) {
        securityPropertyView.update(models);
    }

    public void updateProviderLoader(List<Property> models) {
        providerLoaderView.update(models);
    }

    public void updateAggregateProviders(List<Property> models) {
        aggregateProvidersView.update(models);
    }

}
