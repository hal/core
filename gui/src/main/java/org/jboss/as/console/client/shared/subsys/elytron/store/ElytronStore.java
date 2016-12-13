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
package org.jboss.as.console.client.shared.subsys.elytron.store;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.ChangeSupport;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;
import org.useware.kernel.gui.behaviour.StatementContext;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
@Store
public class ElytronStore extends ChangeSupport {

    class ChannelCallback implements CrudOperationDelegate.Callback {

        private final StatementContext context;
        private final Dispatcher.Channel channel;

        ChannelCallback(StatementContext context, final Dispatcher.Channel channel) {
            this.context = context;
            this.channel = channel;
        }

        @Override
        public void onSuccess(final AddressTemplate address, final String name) {
            Console.info(
                    Console.MESSAGES.successfullyModifiedResource(address.resolve(statementContext, name).toString()));
            init(channel);
        }

        @Override
        public void onFailure(final AddressTemplate address, final String name, final Throwable t) {
            Console.error(Console.MESSAGES.failedToModifyResource(address.resolve(statementContext, name).toString()),
                    t.getMessage());
            channel.nack(t);
        }
    }

    public static final String ROOT = "{selected.profile}/subsystem=elytron";
    public static final AddressTemplate ROOT_ADDRESS = AddressTemplate.of(ROOT);

    public static final AddressTemplate KEY_STORE_ADDRESS = AddressTemplate.of(ROOT).append("key-store=*");
    public static final AddressTemplate KEY_MANAGER_ADDRESS = AddressTemplate.of(ROOT).append("key-managers=*");
    public static final AddressTemplate SERVER_SSL_CONTEXT_ADDRESS = AddressTemplate.of(ROOT).append("server-ssl-context=*");
    public static final AddressTemplate CLIENT_SSL_CONTEXT_ADDRESS = AddressTemplate.of(ROOT).append("client-ssl-context=*");
    public static final AddressTemplate TRUST_MANAGER_ADDRESS = AddressTemplate.of(ROOT).append("trust-managers=*");
    public static final AddressTemplate CREDENTIAL_STORE_ADDRESS = AddressTemplate.of(ROOT).append("credential-store=*");
    public static final AddressTemplate FILTERING_KEY_STORE_ADDRESS = AddressTemplate.of(ROOT).append("filtering-key-store=*");
    public static final AddressTemplate LDAP_KEY_STORE_ADDRESS = AddressTemplate.of(ROOT).append("ldap-key-store=*");
    public static final AddressTemplate PROVIDER_LOADER_ADDRESS = AddressTemplate.of(ROOT).append("provider-loader=*");

    public static final AddressTemplate PROPERTIES_REALM_ADDRESS = AddressTemplate.of(ROOT).append("properties-realm=*");
    public static final AddressTemplate FILESYSTEM_REALM_ADDRESS = AddressTemplate.of(ROOT).append("filesystem-realm=*");
    public static final AddressTemplate JDBC_REALM_ADDRESS = AddressTemplate.of(ROOT).append("jdbc-realm=*");
    public static final AddressTemplate LDAP_REALM_ADDRESS = AddressTemplate.of(ROOT).append("ldap-realm=*");
    public static final AddressTemplate KEYSTORE_REALM_ADDRESS = AddressTemplate.of(ROOT).append("key-store-realm=*");
    public static final AddressTemplate AGGREGATE_REALM_ADDRESS = AddressTemplate.of(ROOT).append("aggregate-realm=*");
    public static final AddressTemplate CUSTOM_MODIFIABLE_REALM_ADDRESS = AddressTemplate.of(ROOT).append("custom-modifiable-realm=*");
    public static final AddressTemplate CUSTOM_REALM_ADDRESS = AddressTemplate.of(ROOT).append("custom-realm=*");
    public static final AddressTemplate IDENTITY_REALM_ADDRESS = AddressTemplate.of(ROOT).append("identity-realm=*");
    public static final AddressTemplate TOKEN_REALM_ADDRESS = AddressTemplate.of(ROOT).append("token-realm=*");

    public static final AddressTemplate MAPPED_REGEX_REALM_MAPPER_ADDRESS = AddressTemplate.of(ROOT).append("mapped-regex-realm-mapper=*");
    public static final AddressTemplate SIMPLE_REGEX_REALM_MAPPER_ADDRESS = AddressTemplate.of(ROOT).append("simple-regex-realm-mapper=*");
    public static final AddressTemplate CUSTOM_REALM_MAPPER_ADDRESS = AddressTemplate.of(ROOT).append("custom-realm-mapper=*");
    public static final AddressTemplate CONSTANT_REALM_MAPPER_ADDRESS = AddressTemplate.of(ROOT).append("constant-realm-mapper=*");

    public static final AddressTemplate AGGREGATE_TRANSFORMER_ADDRESS = AddressTemplate.of(ROOT).append("aggregate-principal-transformer=*");
    public static final AddressTemplate CHAINED_TRANSFORMER_ADDRESS = AddressTemplate.of(ROOT).append("chained-principal-transformer=*");
    public static final AddressTemplate CONSTANT_TRANSFORMER_ADDRESS = AddressTemplate.of(ROOT).append("constant-principal-transformer=*");
    public static final AddressTemplate CUSTOM_TRANSFORMER_ADDRESS = AddressTemplate.of(ROOT).append("custom-principal-transformer=*");
    public static final AddressTemplate REGEX_VALIDATING_TRANSFORMER_ADDRESS = AddressTemplate.of(ROOT).append("regex-validating-principal-transformer=*");
    public static final AddressTemplate REGEX_TRANSFORMER_ADDRESS = AddressTemplate.of(ROOT).append("regex-principal-transformer=*");

    public static final AddressTemplate AGGREGATE_HTTP_SERVER_MECHANISM_FACTORY_ADDRESS = AddressTemplate.of(ROOT).append("aggregate-http-server-mechanism-factory=*");
    public static final AddressTemplate AGGREGATE_SASL_SERVER_FACTORY_ADDRESS = AddressTemplate.of(ROOT).append("aggregate-sasl-server-factory=*");
    public static final AddressTemplate CONFIGURABLE_HTTP_SERVER_MECHANISM_FACTORY_ADDRESS = AddressTemplate.of(ROOT).append("configurable-http-server-mechanism-factory=*");
    public static final AddressTemplate CONFIGURABLE_SASL_SERVER_FACTORY_ADDRESS = AddressTemplate.of(ROOT).append("configurable-sasl-server-factory=*");
    public static final AddressTemplate CUSTOM_CREDENTIAL_SECURITY_FACTORY_ADDRESS = AddressTemplate.of(ROOT).append("custom-credential-security-factory=*");
    public static final AddressTemplate HTTP_AUTHENTICATION_FACTORY_ADDRESS = AddressTemplate.of(ROOT).append("http-authentication-factory=*");
    public static final AddressTemplate KERBEROS_SECURITY_FACTORY_ADDRESS = AddressTemplate.of(ROOT).append("kerberos-security-factory=*");
    public static final AddressTemplate MECHANISM_PROVIDER_FILTERING_SASL_SERVER_FACTORY_ADDRESS = AddressTemplate.of(ROOT).append("mechanism-provider-filtering-sasl-server-factory=*");
    public static final AddressTemplate PROVIDER_HTTP_SERVER_MECHANISM_FACTORY_ADDRESS = AddressTemplate.of(ROOT).append("provider-http-server-mechanism-factory=*");
    public static final AddressTemplate PROVIDER_SASL_SERVER_FACTORY_ADDRESS = AddressTemplate.of(ROOT).append("provider-sasl-server-factory=*");
    public static final AddressTemplate SASL_AUTHENTICATION_FACTORY_ADDRESS = AddressTemplate.of(ROOT).append("sasl-authentication-factory=*");
    public static final AddressTemplate SERVICE_LOADER_HTTP_SERVER_MECHANISM_FACTORY_ADDRESS = AddressTemplate.of(ROOT).append("service-loader-http-server-mechanism-factory=*");
    public static final AddressTemplate SERVICE_LOADER_SASL_SERVER_FACTORY_ADDRESS = AddressTemplate.of(ROOT).append("service-loader-sasl-server-factory=*");

    public static final AddressTemplate ADD_PREFIX_ROLE_MAPPER_ADDRESS = AddressTemplate.of(ROOT).append("add-prefix-role-mapper=*");
    public static final AddressTemplate ADD_SUFFIX_ROLE_MAPPER_ADDRESS = AddressTemplate.of(ROOT).append("add-suffix-role-mapper=*");
    public static final AddressTemplate AGGREGATE_ROLE_MAPPER_ADDRESS = AddressTemplate.of(ROOT).append("aggregate-role-mapper=*");
    public static final AddressTemplate CONSTANT_ROLE_MAPPER_ADDRESS = AddressTemplate.of(ROOT).append("constant-role-mapper=*");
    public static final AddressTemplate CUSTOM_ROLE_MAPPER_ADDRESS = AddressTemplate.of(ROOT).append("custom-role-mapper=*");
    public static final AddressTemplate LOGICAL_ROLE_MAPPER_ADDRESS = AddressTemplate.of(ROOT).append("logical-role-mapper=*");

    public static final AddressTemplate SIMPLE_PERMISSION_MAPPER_ADDRESS = AddressTemplate.of(ROOT).append("simple-permission-mapper=*");
    public static final AddressTemplate LOGICAL_PERMISSION_MAPPER_ADDRESS = AddressTemplate.of(ROOT).append("logical-permission-mapper=*");
    public static final AddressTemplate CUSTOM_PERMISSION_MAPPER_ADDRESS = AddressTemplate.of(ROOT).append("custom-permission-mapper=*");
    public static final AddressTemplate CONSTANT_PERMISSION_MAPPER_ADDRESS = AddressTemplate.of(ROOT).append("constant-permission-mapper=*");

    public static final AddressTemplate AGGREGATE_PRINCIPAL_DECODER_ADDRESS = AddressTemplate.of(ROOT).append("aggregate-principal-decoder=*");
    public static final AddressTemplate CONCATENATING_PRINCIPAL_DECODER_ADDRESS = AddressTemplate.of(ROOT).append("concatenating-principal-decoder=*");
    public static final AddressTemplate CONSTANT_PRINCIPAL_DECODER_ADDRESS = AddressTemplate.of(ROOT).append("constant-principal-decoder=*");
    public static final AddressTemplate CUSTOM_PRINCIPAL_DECODER_ADDRESS = AddressTemplate.of(ROOT).append("custom-principal-decoder=*");
    public static final AddressTemplate X500_PRINCIPAL_DECODER_ADDRESS = AddressTemplate.of(ROOT).append("x500-attribute-principal-decoder=*");
    public static final AddressTemplate CUSTOM_ROLE_DECODER_ADDRESS = AddressTemplate.of(ROOT).append("custom-role-decoder=*");
    public static final AddressTemplate SIMPLE_ROLE_DECODER_ADDRESS = AddressTemplate.of(ROOT).append("simple-role-decoder=*");

    public static final AddressTemplate SECURITY_DOMAIN_ADDRESS = AddressTemplate.of(ROOT).append("security-domain=*");
    public static final AddressTemplate SECURITY_PROPERTY_ADDRESS = AddressTemplate.of(ROOT).append("security-property=*");

    public static final AddressTemplate DIR_CONTEXT_ADDRESS = AddressTemplate.of(ROOT).append("dir-context=*");

    private final DispatchAsync dispatcher;
    private final StatementContext statementContext;
    private final CrudOperationDelegate operationDelegate;
    private final List<Property> keyStore;
    private final List<Property> keyManager;
    private final List<Property> serverSSLContext;
    private final List<Property> clientSSLContext;
    private final List<Property> trustManager;
    private final List<Property> credentialStore;
    private final List<Property> filteringKeyStore;
    private final List<Property> ldapKeyStore;

    private final List<Property> propertiesRealm;
    private final List<Property> filesystemRealm;
    private final List<Property> jdbcRealm;
    private final List<Property> ldapRealm;
    private final List<Property> keystoreRealm;
    private final List<Property> aggregateRealm;
    private final List<Property> customModifiableRealm;
    private final List<Property> customRealm;
    private final List<Property> identityRealm;
    private final List<Property> tokenRealm;
    private final List<Property> providerLoader;

    private final List<Property> mappedRegexRealmMapper;
    private final List<Property> simpleRegexRealmMapper;
    private final List<Property> customRealmMapper;
    private final List<Property> constantRealmMapper;

    private final List<Property> aggregatePrincipalTransformer;
    private final List<Property> chainedPrincipalTransformer;
    private final List<Property> constantPrincipalTransformer;
    private final List<Property> customPrincipalTransformer;
    private final List<Property> regexValidatingPrincipalTransformer;
    private final List<Property> regexPrincipalTransformer;

    private final List<Property> aggregateHttpServerMechanismFactory;
    private final List<Property> aggregateSaslServerFactory;
    private final List<Property> configurableHttpServerMechanismFactory;
    private final List<Property> configurableSaslServerFactory;
    private final List<Property> customCredentialSecurityFactory;
    private final List<Property> httpAuthenticationFactory;
    private final List<Property> kerberosSecurityFactory;
    private final List<Property> mechanismProviderFilteringSaslServerFactory;
    private final List<Property> providerHttpServerMechanismFactory;
    private final List<Property> providerSaslServerFactory;
    private final List<Property> saslAuthenticationFactory;
    private final List<Property> serviceLoaderHttpServerMechanismFactory;
    private final List<Property> serviceLoaderSaslServerFactory;

    private final List<Property> addPrefixRoleMapper;
    private final List<Property> addSuffixRoleMapper;
    private final List<Property> aggregateRoleMapper;
    private final List<Property> constantRoleMapper;
    private final List<Property> customRoleMapper;
    private final List<Property> logicalRoleMapper;

    private final List<Property> simplePermissionMapper;
    private final List<Property> logicalPermissionMapper;
    private final List<Property> customPermissionMapper;
    private final List<Property> constantPermissionMapper;

    private final List<Property> aggregatePrincipalDecoder;
    private final List<Property> concatenatingPrincipalDecoder;
    private final List<Property> constantPrincipalDecoder;
    private final List<Property> customPrincipalDecoder;
    private final List<Property> x500PrincipalDecoder;
    private final List<Property> customRoleDecoder;
    private final List<Property> simpleRoleDecoder;

    private final List<Property> securityDomain;
    private final List<Property> securityProperty;
    private final List<Property> dirContext;

    @Inject
    public ElytronStore(final DispatchAsync dispatcher, StatementContext statementContext) {
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
        this.operationDelegate = new CrudOperationDelegate(statementContext, dispatcher);

        this.keyStore = new ArrayList<>();
        this.keyManager = new ArrayList<>();
        this.serverSSLContext = new ArrayList<>();
        this.clientSSLContext = new ArrayList<>();
        this.trustManager = new ArrayList<>();
        this.credentialStore = new ArrayList<>();
        this.filteringKeyStore = new ArrayList<>();
        this.ldapKeyStore = new ArrayList<>();
        this.providerLoader = new ArrayList<>();

        this.propertiesRealm = new ArrayList<>();
        this.filesystemRealm = new ArrayList<>();
        this.jdbcRealm = new ArrayList<>();
        this.ldapRealm = new ArrayList<>();
        this.keystoreRealm = new ArrayList<>();
        this.aggregateRealm = new ArrayList<>();
        this.customModifiableRealm = new ArrayList<>();
        this.customRealm = new ArrayList<>();
        this.identityRealm= new ArrayList<>();
        this.tokenRealm = new ArrayList<>();

        this.mappedRegexRealmMapper = new ArrayList<>();
        this.simpleRegexRealmMapper = new ArrayList<>();
        this.customRealmMapper = new ArrayList<>();
        this.constantRealmMapper = new ArrayList<>();

        this.aggregatePrincipalTransformer = new ArrayList<>();
        this.chainedPrincipalTransformer = new ArrayList<>();
        this.constantPrincipalTransformer = new ArrayList<>();
        this.customPrincipalTransformer = new ArrayList<>();
        this.regexValidatingPrincipalTransformer = new ArrayList<>();
        this.regexPrincipalTransformer = new ArrayList<>();

        this.aggregateHttpServerMechanismFactory = new ArrayList<>();
        this.aggregateSaslServerFactory = new ArrayList<>();
        this.configurableHttpServerMechanismFactory = new ArrayList<>();
        this.configurableSaslServerFactory = new ArrayList<>();
        this.customCredentialSecurityFactory = new ArrayList<>();
        this.httpAuthenticationFactory = new ArrayList<>();
        this.kerberosSecurityFactory = new ArrayList<>();
        this.mechanismProviderFilteringSaslServerFactory = new ArrayList<>();
        this.providerHttpServerMechanismFactory = new ArrayList<>();
        this.providerSaslServerFactory = new ArrayList<>();
        this.saslAuthenticationFactory = new ArrayList<>();
        this.serviceLoaderHttpServerMechanismFactory = new ArrayList<>();
        this.serviceLoaderSaslServerFactory = new ArrayList<>();

        addPrefixRoleMapper = new ArrayList<>();
        addSuffixRoleMapper = new ArrayList<>();
        aggregateRoleMapper = new ArrayList<>();
        constantRoleMapper = new ArrayList<>();
        customRoleMapper = new ArrayList<>();
        logicalRoleMapper = new ArrayList<>();

        simplePermissionMapper = new ArrayList<>();
        logicalPermissionMapper = new ArrayList<>();
        customPermissionMapper = new ArrayList<>();
        constantPermissionMapper = new ArrayList<>();

        aggregatePrincipalDecoder = new ArrayList<>();
        concatenatingPrincipalDecoder = new ArrayList<>();
        constantPrincipalDecoder = new ArrayList<>();
        customPrincipalDecoder = new ArrayList<>();
        x500PrincipalDecoder = new ArrayList<>();
        customRoleDecoder = new ArrayList<>();
        simpleRoleDecoder = new ArrayList<>();

        securityDomain = new ArrayList<>();
        securityProperty = new ArrayList<>();

        dirContext = new ArrayList<>();
    }


    // ------------------------------------------------------ init

    @Process(actionType = InitElytron.class)
    public void init(final Dispatcher.Channel channel) {
        ResourceAddress rootAddress = ROOT_ADDRESS.resolve(statementContext);

        Operation opRealElytron = new Operation.Builder(READ_RESOURCE_OPERATION, rootAddress)
                .param(RECURSIVE, true)
                .build();

        dispatcher.execute(new DMRAction(opRealElytron), new AsyncCallback<DMRResponse>() {

            @Override
            public void onFailure(final Throwable caught) {
                channel.nack(caught);
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (result.isFailure()) {
                    channel.nack(result.getFailureDescription());
                } else {
                    ModelNode payload = result.get(RESULT);

                    populate(payload, "key-store", keyStore);
                    populate(payload, "key-managers", keyManager);
                    populate(payload, "server-ssl-context", serverSSLContext);
                    populate(payload, "client-ssl-context", clientSSLContext);
                    populate(payload, "trust-managers", trustManager);
                    populate(payload, "credential-store", credentialStore);
                    populate(payload, "filtering-key-store", filteringKeyStore);
                    populate(payload, "ldap-key-store", ldapKeyStore);
                    populate(payload, "provider-loader", providerLoader);

                    populate(payload, "properties-realm", propertiesRealm);
                    populate(payload, "filesystem-realm", filesystemRealm);
                    populate(payload, "jdbc-realm", jdbcRealm);
                    populate(payload, "ldap-realm", ldapRealm);
                    populate(payload, "key-store-realm", keystoreRealm);
                    populate(payload, "aggregate-realm", aggregateRealm);
                    populate(payload, "custom-modifiable-realm", customModifiableRealm);
                    populate(payload, "custom-realm", customRealm);
                    populate(payload, "identity-realm", identityRealm);
                    populate(payload, "token-realm", tokenRealm);

                    populate(payload, "mapped-regex-realm-mapper", mappedRegexRealmMapper);
                    populate(payload, "simple-regex-realm-mapper", simpleRegexRealmMapper);
                    populate(payload, "custom-realm-mapper", customRealmMapper);
                    populate(payload, "constant-realm-mapper", constantRealmMapper);

                    populate(payload, "aggregate-principal-transformer", aggregatePrincipalTransformer);
                    populate(payload, "chained-principal-transformer", chainedPrincipalTransformer);
                    populate(payload, "constant-principal-transformer", constantPrincipalTransformer);
                    populate(payload, "custom-principal-transformer", customPrincipalTransformer);
                    populate(payload, "regex-validating-principal-transformer", regexValidatingPrincipalTransformer);
                    populate(payload, "regex-principal-transformer", regexPrincipalTransformer);

                    populate(payload, "aggregate-http-server-mechanism-factory", aggregateHttpServerMechanismFactory);
                    populate(payload, "aggregate-sasl-server-factory", aggregateSaslServerFactory);
                    populate(payload, "configurable-http-server-mechanism-factory",
                            configurableHttpServerMechanismFactory);
                    populate(payload, "configurable-sasl-server-factory", configurableSaslServerFactory);
                    populate(payload, "custom-credential-security-factory", customCredentialSecurityFactory);
                    populate(payload, "http-authentication-factory", httpAuthenticationFactory);
                    populate(payload, "kerberos-security-factory", kerberosSecurityFactory);
                    populate(payload, "mechanism-provider-filtering-sasl-server-factory",
                            mechanismProviderFilteringSaslServerFactory);
                    populate(payload, "provider-http-server-mechanism-factory", providerHttpServerMechanismFactory);
                    populate(payload, "provider-sasl-server-factory", providerSaslServerFactory);
                    populate(payload, "sasl-authentication-factory", saslAuthenticationFactory);
                    populate(payload, "service-loader-http-server-mechanism-factory",
                            serviceLoaderHttpServerMechanismFactory);
                    populate(payload, "service-loader-sasl-server-factory", serviceLoaderSaslServerFactory);

                    populate(payload, "add-prefix-role-mapper", addPrefixRoleMapper);
                    populate(payload, "add-suffix-role-mapper", addSuffixRoleMapper);
                    populate(payload, "aggregate-role-mapper", aggregateRoleMapper);
                    populate(payload, "constant-role-mapper", constantRoleMapper);
                    populate(payload, "custom-role-mapper", customRoleMapper);
                    populate(payload, "logical-role-mapper", logicalRoleMapper);

                    populate(payload, "simple-permission-mapper", simplePermissionMapper);
                    populate(payload, "logical-permission-mapper", logicalPermissionMapper);
                    populate(payload, "custom-permission-mapper", customPermissionMapper);
                    populate(payload, "constant-permission-mapper", constantPermissionMapper);

                    populate(payload, "aggregate-principal-decoder", aggregatePrincipalDecoder);
                    populate(payload, "concatenating-principal-decoder", concatenatingPrincipalDecoder);
                    populate(payload, "constant-principal-decoder", constantPrincipalDecoder);
                    populate(payload, "custom-principal-decoder", customPrincipalDecoder);
                    populate(payload, "x500-attribute-principal-decoder", x500PrincipalDecoder);
                    populate(payload, "custom-role-decoder", customRoleDecoder);
                    populate(payload, "simple-role-decoder", simpleRoleDecoder);

                    populate(payload, "security-domain", securityDomain);
                    populate(payload, "security-property", securityProperty);

                    populate(payload, "dir-context", dirContext);

                    channel.ack();
                }
            }
        });
    }

    private void populate(ModelNode payload, String resourceName, List<Property> listToPopulate) {
        listToPopulate.clear();
        if (payload.hasDefined(resourceName)) {
            listToPopulate.addAll(payload.get(resourceName).asPropertyList());
        }
    }

    // ------------------------------------------------------ CRUD for generic resources

    @Process(actionType = AddResourceGeneric.class)
    public void addResourceGeneric(final AddResourceGeneric action, final Dispatcher.Channel channel) {
        operationDelegate.onCreateResource(action.getAddress(),
                action.getProperty().getName(), action.getProperty().getValue(),
                new ChannelCallback(statementContext, channel));
    }

    @Process(actionType = ModifyResourceGeneric.class)
    public void modifyResourceGeneric(final ModifyResourceGeneric action, final Dispatcher.Channel channel) {
        operationDelegate.onSaveResource(action.getAddress(), action.getName(), action.getChangedValues(),
                new ChannelCallback(statementContext, channel));
    }

    @Process(actionType = RemoveResourceGeneric.class)
    public void removeResourceGeneric(final RemoveResourceGeneric action, final Dispatcher.Channel channel) {
        operationDelegate.onRemoveResource(action.getAddress(), action.getName(),
                new ChannelCallback(statementContext, channel));
    }

    @Process(actionType = ModifyComplexAttribute.class)
    public void modifyComplexAttribute(final ModifyComplexAttribute action, final Dispatcher.Channel channel) {
        ResourceAddress address = action.getAddress().resolve(statementContext, action.getName());
        Operation operation = new Operation.Builder(WRITE_ATTRIBUTE_OPERATION, address)
                .param(NAME, action.getComplexAttributeName())
                .param(VALUE, action.getPayload())
                .build();
        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                new ChannelCallback(statementContext, channel)
                        .onFailure(action.getAddress(), action.getComplexAttributeName(), caught);
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                new ChannelCallback(statementContext, channel).onSuccess(action.getAddress(), action.getComplexAttributeName());
            }
        });
    }

    @Process(actionType = AddListAttribute.class)
    public void addItemToAttributeList(final AddListAttribute action, final Dispatcher.Channel channel) {
        ResourceAddress address = action.getAddress().resolve(statementContext, action.getName());
        Operation operation = new Operation.Builder(LIST_ADD_OPERATION, address)
                .param(NAME, action.getAttributeName())
                .param(VALUE, action.getPayload())
                .build();
        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                new ChannelCallback(statementContext, channel)
                        .onFailure(action.getAddress(), action.getAttributeName(), caught);
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                new ChannelCallback(statementContext, channel).onSuccess(action.getAddress(), action.getAttributeName());
            }
        });
    }

    @Process(actionType = RemoveListAttribute.class)
    public void removeItemFromAttributeList(final RemoveListAttribute action, final Dispatcher.Channel channel) {
        ResourceAddress address = action.getAddress().resolve(statementContext, action.getResourceName());
        Operation operation = new Operation.Builder(LIST_REMOVE_OPERATION, address)
                .param(NAME, action.getAttributeName())
                .param(VALUE, action.getPayload())
                .build();
        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                new ChannelCallback(statementContext, channel)
                        .onFailure(action.getAddress(), action.getAttributeName(), caught);
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                new ChannelCallback(statementContext, channel).onSuccess(action.getAddress(), action.getAttributeName());
            }
        });
    }


    public List<Property> getKeyStore() {
        return keyStore;
    }

    public List<Property> getCredentialStore() {
        return credentialStore;
    }

    public List<Property> getFilteringKeyStore() {
        return filteringKeyStore;
    }

    public List<Property> getLdapKeyStore() {
        return ldapKeyStore;
    }

    public List<Property> getKeyManager() {
        return keyManager;
    }

    public List<Property> getServerSSLContext() {
        return serverSSLContext;
    }

    public List<Property> getClientSSLContext() {
        return clientSSLContext;
    }

    public List<Property> getTrustManager() {
        return trustManager;
    }

    public List<Property> getPropertiesRealm() {
        return propertiesRealm;
    }

    public List<Property> getFilesystemRealm() {
        return filesystemRealm;
    }

    public List<Property> getJdbcRealm() {
        return jdbcRealm;
    }

    public List<Property> getLdapRealm() {
        return ldapRealm;
    }

    public List<Property> getKeystoreRealm() {
        return keystoreRealm;
    }

    public List<Property> getAggregateRealm() {
        return aggregateRealm;
    }

    public List<Property> getCustomModifiableRealm() {
        return customModifiableRealm;
    }

    public List<Property> getCustomRealm() {
        return customRealm;
    }

    public List<Property> getIdentityRealm() {
        return identityRealm;
    }

    public List<Property> getTokenRealm() {
        return tokenRealm;
    }

    public List<Property> getMappedRegexRealmMapper() {
        return mappedRegexRealmMapper;
    }

    public List<Property> getSimpleRegexRealmMapper() {
        return simpleRegexRealmMapper;
    }

    public List<Property> getCustomRealmMapper() {
        return customRealmMapper;
    }

    public List<Property> getConstantRealmMapper() {
        return constantRealmMapper;
    }

    public List<Property> getAggregatePrincipalTransformer() {
        return aggregatePrincipalTransformer;
    }

    public List<Property> getChainedPrincipalTransformer() {
        return chainedPrincipalTransformer;
    }

    public List<Property> getConstantPrincipalTransformer() {
        return constantPrincipalTransformer;
    }

    public List<Property> getCustomPrincipalTransformer() {
        return customPrincipalTransformer;
    }

    public List<Property> getRegexValidatingPrincipalTransformer() {
        return regexValidatingPrincipalTransformer;
    }

    public List<Property> getRegexPrincipalTransformer() {
        return regexPrincipalTransformer;
    }

    public List<Property> getServiceLoaderSaslServerFactory() {
        return serviceLoaderSaslServerFactory;
    }

    public List<Property> getServiceLoaderHttpServerMechanismFactory() {
        return serviceLoaderHttpServerMechanismFactory;
    }

    public List<Property> getSaslAuthenticationFactory() {
        return saslAuthenticationFactory;
    }

    public List<Property> getProviderSaslServerFactory() {
        return providerSaslServerFactory;
    }

    public List<Property> getProviderHttpServerMechanismFactory() {
        return providerHttpServerMechanismFactory;
    }

    public List<Property> getMechanismProviderFilteringSaslServerFactory() {
        return mechanismProviderFilteringSaslServerFactory;
    }

    public List<Property> getKerberosSecurityFactory() {
        return kerberosSecurityFactory;
    }

    public List<Property> getHttpAuthenticationFactory() {
        return httpAuthenticationFactory;
    }

    public List<Property> getCustomCredentialSecurityFactory() {
        return customCredentialSecurityFactory;
    }

    public List<Property> getConfigurableSaslServerFactory() {
        return configurableSaslServerFactory;
    }

    public List<Property> getConfigurableHttpServerMechanismFactory() {
        return configurableHttpServerMechanismFactory;
    }

    public List<Property> getAggregateSaslServerFactory() {
        return aggregateSaslServerFactory;
    }

    public List<Property> getAggregateHttpServerMechanismFactory() {
        return aggregateHttpServerMechanismFactory;
    }

    public List<Property> getSimpleRoleDecoder() {
        return simpleRoleDecoder;
    }

    public List<Property> getCustomRoleDecoder() {
        return customRoleDecoder;
    }

    public List<Property> getX500PrincipalDecoder() {
        return x500PrincipalDecoder;
    }

    public List<Property> getCustomPrincipalDecoder() {
        return customPrincipalDecoder;
    }

    public List<Property> getConstantPrincipalDecoder() {
        return constantPrincipalDecoder;
    }

    public List<Property> getConcatenatingPrincipalDecoder() {
        return concatenatingPrincipalDecoder;
    }

    public List<Property> getAggregatePrincipalDecoder() {
        return aggregatePrincipalDecoder;
    }

    public List<Property> getCustomPermissionMapper() {
        return customPermissionMapper;
    }

    public List<Property> getLogicalPermissionMapper() {
        return logicalPermissionMapper;
    }

    public List<Property> getSimplePermissionMapper() {
        return simplePermissionMapper;
    }

    public List<Property> getConstantPermissionMapper() {
        return constantPermissionMapper;
    }

    public List<Property> getLogicalRoleMapper() {
        return logicalRoleMapper;
    }

    public List<Property> getCustomRoleMapper() {
        return customRoleMapper;
    }

    public List<Property> getConstantRoleMapper() {
        return constantRoleMapper;
    }

    public List<Property> getAggregateRoleMapper() {
        return aggregateRoleMapper;
    }

    public List<Property> getAddSuffixRoleMapper() {
        return addSuffixRoleMapper;
    }

    public List<Property> getAddPrefixRoleMapper() {
        return addPrefixRoleMapper;
    }

    public List<Property> getSecurityDomain() {
        return securityDomain;
    }

    public List<Property> getSecurityProperty() {
        return securityProperty;
    }

    public List<Property> getDirContext() {
        return dirContext;
    }

    public List<Property> getProviderLoader() {
        return providerLoader;
    }
}
