/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.core;

import com.google.gwt.i18n.client.ConstantsWithLookup;

/**
 * A short descriptions for the tokens used in the console
 *
 * @author Harald Pehl
 */
@SuppressWarnings({"UnusedDeclaration", "SpellCheckingInspection"})
public interface UITokens extends ConstantsWithLookup {

    String audit_log();

    String batch();

    String configadmin();

    String datasources();

    String deployment_scanner();

    String distributed_cache();

    String domain_deployments();

    String ds_metrics();

    String ee();

    String ejb3();

    String environment();

    String host_interfaces();

    String host_jvms();

    String host_properties();

    String host_vm();

    String http();

    String infinispan();

    String interfaces();

    String invalidation_cache();

    String io();

    String jacorb();

    String jca();

    String jgroups();

    String jms_metrics();

    String jmx();

    String jpa();

    String jpa_metrics();

    String local_cache();

    String logging();

    String logfiles();

    String logviewer();

    String mail();

    String messaging();

    String messaging_cluster();

    String messaging_connections();

    String modcluster();

    String naming();

    String patching();

    String path();

    String properties();

    String replicated_cache();

    String resource_adapters();

    String role_assignment();

    String security();

    String security_domains();

    String server_config();

    String server_groups();

    String servlet();

    String socket_bindings();

    String threads();

    String topology();

    String transactions();

    String tx_logs();

    String tx_metrics();

    String web();

    String web_metrics();

    String webservice_runtime();

    String webservices();

    String vm();
}
