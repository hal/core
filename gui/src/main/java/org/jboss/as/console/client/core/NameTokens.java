/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

package org.jboss.as.console.client.core;

/**
 * @author Heiko Braun
 * @date 2/4/11
 */
public class NameTokens {

    public static final String mainLayout = "main";

    public static final String Batch = "batch";
    public static final String BatchJberet = "batch-jberet";
    public static final String BatchJberetMetrics = "batch-jberet-metrics";
    public static final String IO = "io";
    public static final String HomepagePresenter = "home";
    public static final String BoundedQueueThreadPoolPresenter = "threads";
    public static final String ConfigAdminPresenter = "configadmin";
    public static final String DataSourcePresenter = "datasources";
    public static final String DebugToolsPresenter = "debug-tools";
    public static final String EJB3Presenter = "ejb3";
    public static final String HostInterfacesPresenter = "host-interfaces";
    public static final String HostJVMPresenter = "host-jvms";
    public static final String HostPropertiesPresenter = "host-properties";
    public static final String Infinispan = "infinispan";
    public static final String InterfacePresenter = "interfaces";
    public static final String JMSPresenter = "jms";
    public static final String JndiPresenter = "naming";
    public static final String LogViewer = "logviewer";
    public static final String LogFiles = "logfiles";
    public static final String LogHandler = "log-handler";
    public static final String MessagingPresenter = "messaging";
    public static final String ActivemqMessagingPresenter = "activemq-messaging";
    public static final String MetricsPresenter = "invocation-metrics";
    public static final String ModelBrowserPresenter = "model-browser";
    public static final String PicketLinkFinder = "picketlink-finder";
    public static final String PicketLinkFederation = "picketlink-federation";
    public static final String PicketLinkServiceProvider = "picketlink-service-provider";
    public static final String PropertiesPresenter = "properties";
    public static final String ResourceAdapterPresenter ="resource-adapters";
    public static final String Remoting = "remoting";
    public static final String IiopOpenJdk = "iiop-openjdk";
    public static final String SettingsPresenter = "settings";
    public static final String SocketBindingPresenter = "socket-bindings";
    public static final String StandaloneServerPresenter = "server-overview";
    public static final String WebPresenter = "web";
    public static final String WebServicePresenter = "webservices";
    public static final String VirtualMachine = "vm";
    public static final String HostVMMetricPresenter = "host-vm";
    public static final String TransactionPresenter = "transactions";
    public static final String StandaloneRuntimePresenter = "standalone-runtime";
    public static final String DomainRuntimePresenter = "domain-runtime";
    public static final String PatchingPresenter = "patching";

    public static final String TXMetrics = "tx-metrics";
    public static final String TXLogs = "tx-logs";
    public static final String JpaPresenter = "jpa";
    public static final String MailPresenter = "mail";
    public static final String JMXPresenter = "jmx";
    public static final String EEPresenter = "ee";
    public static final String JcaPresenter = "jca";
    public static final String WebMetricPresenter = "web-metrics";
    public static final String JmsMetricPresenter = "jms-metrics";
    public static final String ActivemqMetricPresenter = "activemq-metrics";
    public static final String DataSourceMetricPresenter  = "ds-metrics";
    public static final String EnvironmentPresenter = "environment";
    public static final String ExtensionsPresenter = "extension";
    public static final String JPAMetricPresenter = "jpa-metrics";
    public static final String WebServiceRuntimePresenter = "webservice-runtime";
    public static final String JGroupsPresenter = "jgroups";
    public static final String ModclusterPresenter = "modcluster";
    public static final String MsgConnectionsPresenter = "messaging-connections";
    public static final String ActivemqMsgConnectionsPresenter = "activemq-messaging-connections";
    public static final String MsgClusteringPresenter  =  "messaging-cluster";
    public static final String ActivemqMsgClusteringPresenter =  "activemq-messaging-cluster";
    public static final String ToolsPresenter = "tools";
    public static final String PathManagementPresenter = "path" ;

    public static final String DialogPresenter = "mbui";
    public static final String DomainPresenter = "domain";
    public static final String NoServer = "no-server";
    public static final String HttpPresenter = "http";
    public static final String ServletPresenter = "servlet";
    public static final String UndertowFilters= "filters";
    public static final String UndertowPresenter = "undertow";
    public static final String MailFinder = "mail-sessions";
    public static final String HttpMetrics = "http-metrics";
    public static final String CacheFinderPresenter = "cache-container";
    public static final String CachesPresenter = "caches";

    public static final String HornetqFinder = "hornetq";
    public static final String ActivemqFinder = "activemq";

    public static final String signInPage = "login";
    public static final String errorPage = "err";
    public static final String ServerProfile = "profile";

    public static final String systemApp = "system";

    public final static String InterfaceToolPresenter = "server-interfaces";

    public final static String PathToolPresenter = "server-paths";

    public final static String SubsystemToolPresenter = "subsys";

    public static final String ThreadManagementPresenter = "threading";

    // ------------------------------------------------------
    // domain tokens below

    public static final String ProfileMgmtPresenter = "profiles";

    public static final String Topology = "topology";

    public static final String ServerGroupPresenter = "server-groups";

    public static final String DeploymentsPresenter  = "domain-deployments-old";

    public static final String HostMgmtPresenter = "hosts";

    public final static String ServerPresenter = "server-config";

    public static final String DomainDeploymentFinder = "domain-deployments";
    public static final String StandaloneDeploymentFinder = "standalone-deployments";
    public static final String DeploymentDetails = "deployments-details";
    public static final String DeploymentScanner = "deployment-scanner";


    // ------------------------------------------------------
    // administration tokens below

    public static final String AuditLogPresenter = "audit-log";
    public static final String Logging = "logging";
    public static final String SecDomains = "security_domains";
    public static final String SecDomain = "security_domain";
    public static final String UndertowFinder = "undertow-subsystem";
    public static final String AccessControlFinder = "rbac";
    public static final String DataSourceFinder = "ds-finder";
    public static final String XADataSourcePresenter = "xads";
    public static final String ResourceAdapterFinder = "ra-finder";
    public static final String GenericSubsystem = "generic-subsystem";
}


