/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.console.client.meta;

import javax.inject.Inject;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.bootstrap.hal.BootstrapStep;
import org.jboss.as.console.client.semver.ManagementModel;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Control;

import static org.jboss.dmr.client.ModelDescriptionConstants.HOST;
import static org.jboss.dmr.client.ModelDescriptionConstants.NAME;
import static org.jboss.dmr.client.ModelDescriptionConstants.QUERY_OPERATION;
import static org.jboss.dmr.client.ModelDescriptionConstants.RESULT;

/**
 * Registers core capabilities from https://github.com/wildfly/wildfly-capabilities and other well-known capabilities.
 *
 * @author Claudio Miranda
 */
public class CoreCapabilitiesRegister implements BootstrapStep {

    // address used by components that doesn't set capability-reference
    public static final String NETWORK_OUTBOUND_SOCKET_BINDING = "org.wildfly.network.outbound-socket-binding";
    public static final String NETWORK_SOCKET_BINDING = "org.wildfly.network.socket-binding";
    public static final String DATASOURCE = "org.wildfly.data-source";
    public static final String SECURITY_DOMAIN = "org.wildfly.security.legacy-security-domain";
    public static final String SECURITY_SSL_CONTEXT = "org.wildfly.security.ssl-context";
    public static final String STATELESS_SESSION_BEAN_POOL = "org.wildfly.ejb3.slsb-pool";
    public static final String EJB_CACHE = "org.wildfly.ejb3.cache";
    public static final String EJB_PASSIVATION_STORE = "org.wildfly.ejb3.passivation-store";
    public static final String CACHE_CONTAINER = "org.wildfly.clustering.infinispan.cache-container";
    public static final String EJB_THREAD_POOL = "org.wildfly.ejb3.thread-pool";
    public static final String EJB_APPLICATION_SECURITY_DOMAIN = "org.wildfly.ejb3.application-security-domain";
    public static final String REMOTING_HTTP_CONNECTOR = "org.wildfly.remoting.http-connector";
    public static final String LOGGING_FORMATTER = "org.wildfly.logging.formatter";
    public static final String JGROUPS_STACK = "org.wildfly.jgroups.stack";
    public static final String JGROUPS_CHANNEL = "org.wildfly.clustering.jgroups.channel";
    public static final String UNDERTOW_LISTENER = "org.wildfly.undertow.listener";
    public static final String UNDERTOW_SERVER = "org.wildfly.undertow.server";
    public static final String UNDERTOW_HOST = "org.wildfly.undertow.host";
    public static final String UNDERTOW_SERVLET_CONTAINER = "org.wildfly.undertow.servlet-container";
    public static final String UNDERTOW_RESPONSE_FILTER = "org.wildfly.undertow.filter";

    private DispatchAsync dispatcher;
    private HostStore hostStore;
    private final Capabilities capabilities;

    @Inject
    public CoreCapabilitiesRegister(DispatchAsync dispatcher, HostStore hostStore, Capabilities capabilities) {
        this.dispatcher = dispatcher;
        this.hostStore = hostStore;
        this.capabilities = capabilities;
    }

    @Override
    public void execute(final Control<BootstrapContext> control) {
        final BootstrapContext context = control.getContext();
        if (ManagementModel.supportsCapabilitiesRegistry(context.getManagementVersion())) {
            ResourceAddress address = control.getContext().isStandalone()
                    ? new ResourceAddress()
                    : new ResourceAddress().add(HOST, hostStore.getSelectedHost());
            address.add("core-service", "capability-registry");

            ModelNode possibleCapabilitiesKey = new ModelNode();
            possibleCapabilitiesKey.add("possible-capabilities");

            Operation operation = new Operation.Builder(QUERY_OPERATION, address)
                    .param("select", possibleCapabilitiesKey)
                    .build();

            dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {

                @Override
                public void onFailure(Throwable caught) {
                    registerManualCapabilities(); // fallback
                    context.setlastError(caught);
                    control.abort();
                }

                @Override
                public void onSuccess(DMRResponse result) {
                    ModelNode response = result.get();

                    if (response.isFailure()) {
                        registerManualCapabilities(); // fallback
                        context.setlastError(new RuntimeException(response.getFailureDescription()));
                        control.abort();
                    } else {
                        for (ModelNode node : response.get(RESULT).get("possible-capabilities").asList()) {

                            Capability capability = new Capability(node.get(NAME).asString(),
                                    node.get("dynamic").asBoolean());
                            for (ModelNode registrationAddress : node.get("registration-points").asList()) {
                                String resAddress = registrationAddress.asString();
                                // this corner case is to enable the use of capability org.wildfly.domain.profile
                                // to ask for all profiles. The other cases, is specific to HAL to use the {selected.profile}
                                // keyword to search resources under a specific profile
                                boolean notProfileCapability = !"org.wildfly.domain.profile".equals(
                                        capability.getName());
                                if (notProfileCapability) {
                                    resAddress = resAddress.replace("profile=*", "{selected.profile}");
                                }
                                boolean startsWithHost = "/host=".equals(resAddress.substring(0, 6));
                                if (startsWithHost) {
                                    resAddress = resAddress.replaceAll("^/host=\\w*/", "/{selected.host}/");
                                }
                                capability.addTemplate(AddressTemplate.of(resAddress));
                            }
                            capabilities.register(capability);

                        }
                        registerManualCapabilities();
                        control.proceed();
                    }

                }
            });
        } else {
            registerManualCapabilities();
            control.proceed();
        }
    }

    /*
         There are no capabilities registered for the following addresses, so this is an emulation
         unfortunately, there is no capability-reference also, so, each attribute that wants to have
         the auto-complete, must add the SuggestionResource as a factory.
     */
    private void registerManualCapabilities() {

        capabilities.register(SECURITY_DOMAIN, true,
                AddressTemplate.of("/{selected.profile}/subsystem=security/security-domain=*"));

        capabilities.register(STATELESS_SESSION_BEAN_POOL, true,
                AddressTemplate.of("/{selected.profile}/subsystem=ejb3/strict-max-bean-instance-pool=*"));

        capabilities.register(EJB_CACHE, true,
                AddressTemplate.of("/{selected.profile}/subsystem=ejb3/cache=*"));

        capabilities.register(EJB_PASSIVATION_STORE, true,
                AddressTemplate.of("/{selected.profile}/subsystem=ejb3/passivation-store=*"));

        capabilities.register(EJB_THREAD_POOL, true,
                AddressTemplate.of("/{selected.profile}/subsystem=ejb3/thread-pool=*"));

        capabilities.register(REMOTING_HTTP_CONNECTOR, true,
                AddressTemplate.of("/{selected.profile}/subsystem=remoting/http-connector=*"));

        capabilities.register(LOGGING_FORMATTER, true,
                AddressTemplate.of("/{selected.profile}/subsystem=logging/pattern-formatter=*"),
                AddressTemplate.of("/{selected.profile}/subsystem=logging/custom-formatter=*"));

        capabilities.register(JGROUPS_STACK, true,
                AddressTemplate.of("/{selected.profile}/subsystem=jgroups/stack=*"));

        capabilities.register(UNDERTOW_SERVER, true,
                AddressTemplate.of("/{selected.profile}/subsystem=undertow/server=*"));

        capabilities.register(UNDERTOW_HOST, true,
                AddressTemplate.of("/{selected.profile}/subsystem=undertow/server=*/host=*"));

        capabilities.register(UNDERTOW_SERVLET_CONTAINER, true,
                AddressTemplate.of("/{selected.profile}/subsystem=undertow/servlet-container=*"));

        capabilities.register(UNDERTOW_RESPONSE_FILTER, true,
                AddressTemplate.of("/{selected.profile}/subsystem=undertow/configuration=filter/response-header=*"));

    }

}
