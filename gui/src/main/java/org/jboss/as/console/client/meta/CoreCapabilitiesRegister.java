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

import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.bootstrap.hal.BootstrapStep;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.gwt.flow.client.Control;

/**
 * Registers core capabilities from https://github.com/wildfly/wildfly-capabilities and other well-known capabilities.
 *
 * @author Harald Pehl
 */
public class CoreCapabilitiesRegister implements BootstrapStep {

    // address used by components that doesn't set capability-reference
    public static final String NETWORK_OUTBOUND_SOCKET_BINDING = "org.wildfly.network.outbound-socket-binding";
    public static final String NETWORK_SOCKET_BINDING = "org.wildfly.network.socket-binding";
    public static final String DATASOURCE = "org.wildfly.data-source";
    public static final String SECURITY_DOMAIN = "org.wildfly.security.security-domain";
    public static final String STATELESS_SESSION_BEAN_POOL = "org.wildfly.ejb3.slsb-pool";
    public static final String EJB_THREAD_POOL = "org.wildfly.ejb3.thread-pool";
    public static final String EJB_CACHE = "org.wildfly.ejb3.cache";
    public static final String EJB_PASSIVATION_STORE = "org.wildfly.ejb3.passivation-store";
    public static final String CACHE_CONTAINER = "org.wildfly.infinispan.cache-container";
    public static final String REMOTING_HTTP_CONNECTOR = "org.wildfly.remoting.http-connector";
    public static final String LOGGING_FORMATTER = "org.wildfly.logging.formatter";
    public static final String JGROUPS_STACK = "org.wildfly.jgroups.stack";
    public static final String JGROUPS_CHANNEL = "org.wildfly.jgroups.channel";
    public static final String UNDERTOW_CONNECTOR = "org.wildfly.undertow.connector";
    public static final String UNDERTOW_SERVER = "org.wildfly.undertow.server";
    public static final String UNDERTOW_HOST = "org.wildfly.undertow.host";
    public static final String UNDERTOW_SERVLET_CONTAINER = "org.wildfly.undertow.servlet-container";
    public static final String UNDERTOW_RESPONSE_FILTER = "org.wildfly.undertow.filter";


    private final Capabilities capabilities;

    @Inject
    public CoreCapabilitiesRegister(Capabilities capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    //@SuppressWarnings({"HardCodedStringLiteral", "DuplicateStringLiteralInspection"})
    public void execute(final Control<BootstrapContext> control) {

        // Selected capabilities from https://github.com/wildfly/wildfly-capabilities
        capabilities.register(DATASOURCE, true,
                AddressTemplate.of("/{selected.profile}/subsystem=datasources/data-source=*"));
        
        capabilities.register("org.wildfly.io.buffer-pool", true,
                AddressTemplate.of("/{selected.profile}/subsystem=io/buffer-pool=*"));
        
        capabilities.register("org.wildfly.io.worker", true,
                AddressTemplate.of("/{selected.profile}/subsystem=io/worker=*"));
        
        // Lists local and remote outbound socket bindings of all socket binding groups
        capabilities.register(NETWORK_OUTBOUND_SOCKET_BINDING, true,
                AddressTemplate.of("/socket-binding-group=*/local-destination-outbound-socket-binding=*"),
                AddressTemplate.of("/socket-binding-group=*/remote-destination-outbound-socket-binding=*"));

        // Lists all socket bindings of all socket binding groups
        capabilities.register(NETWORK_SOCKET_BINDING, true,
                AddressTemplate.of("/socket-binding-group=*/socket-binding=*"));

        // Well-known capabilities not (yet) on https://github.com/wildfly/wildfly-capabilities
        capabilities.register("org.wildfly.network.interface", true, AddressTemplate.of("/interface=*"));
        
        capabilities.register("org.wildfly.domain.profile", true, AddressTemplate.of("/profile=*"));
        
        capabilities.register("org.wildfly.domain.server-group", true, AddressTemplate.of("/server-group=*"));
        
        capabilities.register("org.wildfly.domain.socket-binding-group", true,
                AddressTemplate.of("/socket-binding-group=*"));

        capabilities.register("org.wildfly.batch.job.repository", true,
                AddressTemplate.of("/{selected.profile}/subsystem=batch-jberet/in-memory-job-repository=*"),
                AddressTemplate.of("/{selected.profile}/subsystem=batch-jberet/jdbc-job-repository=*"));

        capabilities.register("org.wildfly.batch.thread.pool", true,
                AddressTemplate.of("/{selected.profile}/subsystem=batch-jberet/thread-pool=*"));

        capabilities.register("org.wildfly.clustering.singleton.policy", false,
                AddressTemplate.of("/{selected.profile}/subsystem=singleton/singleton-policy=*"));

        // There are no capabilities registered for the following addresses, so this is an emulation 
        // unfortunately, there is no capability-reference also, so, each attribute that wants to have 
        // the auto-complete, must add the SuggestionResource as a factory.
        capabilities.register(SECURITY_DOMAIN, true, 
                AddressTemplate.of("/{selected.profile}/subsystem=security/security-domain=*"));

        capabilities.register(STATELESS_SESSION_BEAN_POOL, true, 
                AddressTemplate.of("/{selected.profile}/subsystem=ejb3/strict-max-bean-instance-pool=*"));
        
        capabilities.register(EJB_CACHE, true, 
                AddressTemplate.of("/{selected.profile}/subsystem=ejb3/cache=*"));

        capabilities.register(EJB_PASSIVATION_STORE, true, 
                AddressTemplate.of("/{selected.profile}/subsystem=ejb3/passivation-store=*"));

        capabilities.register(CACHE_CONTAINER, true, 
                AddressTemplate.of("/{selected.profile}/subsystem=infinispan/cache-container=*"));

        capabilities.register(EJB_THREAD_POOL, true, 
                AddressTemplate.of("/{selected.profile}/subsystem=ejb3/thread-pool=*"));

        capabilities.register(REMOTING_HTTP_CONNECTOR, true, 
                AddressTemplate.of("/{selected.profile}/subsystem=remoting/http-connector=*"));

        capabilities.register(LOGGING_FORMATTER, true, 
                AddressTemplate.of("/{selected.profile}/subsystem=logging/pattern-formatter=*"),
                AddressTemplate.of("/{selected.profile}/subsystem=logging/custom-formatter=*"));

        capabilities.register(JGROUPS_STACK, true, 
                AddressTemplate.of("/{selected.profile}/subsystem=jgroups/stack=*"));

        capabilities.register(JGROUPS_CHANNEL, true, 
                AddressTemplate.of("/{selected.profile}/subsystem=jgroups/channel=*"));

        capabilities.register(UNDERTOW_CONNECTOR, true, 
                AddressTemplate.of("/{selected.profile}/subsystem=undertow/server=*/ajp-listener=*"),
                AddressTemplate.of("/{selected.profile}/subsystem=undertow/server=*/http-listener=*"),
                AddressTemplate.of("/{selected.profile}/subsystem=undertow/server=*/https-listener=*"));

        capabilities.register(UNDERTOW_SERVER, true, 
                AddressTemplate.of("/{selected.profile}/subsystem=undertow/server=*"));

        capabilities.register(UNDERTOW_HOST, true, 
                AddressTemplate.of("/{selected.profile}/subsystem=undertow/server=*/host=*"));

        capabilities.register(UNDERTOW_SERVLET_CONTAINER, true, 
                AddressTemplate.of("/{selected.profile}/subsystem=undertow/servlet-container=*"));

        capabilities.register(UNDERTOW_RESPONSE_FILTER, true, 
                AddressTemplate.of("/{selected.profile}/subsystem=undertow/configuration=filter/response-header=*"));

        control.proceed();
    }

}
