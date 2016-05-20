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

package org.jboss.as.console.client.shared;

//import com.google.web.bindery.autobean.shared.AutoBean;
//import com.google.web.bindery.autobean.shared.AutoBeanFactory.Category;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import org.jboss.as.console.client.administration.audit.AuditLogItem;
import org.jboss.as.console.client.core.bootstrap.cors.BootstrapServer;
import org.jboss.as.console.client.core.settings.CommonSettings;
import org.jboss.as.console.client.domain.model.Host;
import org.jboss.as.console.client.domain.model.ProfileRecord;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.model.ServerInstance;
import org.jboss.as.console.client.search.Document;
import org.jboss.as.console.client.shared.general.model.Interface;
import org.jboss.as.console.client.shared.general.model.LocalSocketBinding;
import org.jboss.as.console.client.shared.general.model.Path;
import org.jboss.as.console.client.shared.general.model.RemoteSocketBinding;
import org.jboss.as.console.client.shared.general.model.SocketBinding;
import org.jboss.as.console.client.shared.general.model.SocketGroup;
import org.jboss.as.console.client.shared.jvm.Jvm;
import org.jboss.as.console.client.shared.jvm.model.HeapMetric;
import org.jboss.as.console.client.shared.jvm.model.OSMetric;
import org.jboss.as.console.client.shared.jvm.model.RuntimeMetric;
import org.jboss.as.console.client.shared.jvm.model.ThreadMetric;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.shared.patching.PatchInfo;
import org.jboss.as.console.client.shared.patching.RollbackOptions;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.properties.PropertyRecordCategory;
import org.jboss.as.console.client.shared.runtime.ext.Extension;
import org.jboss.as.console.client.shared.runtime.jpa.model.JPADeployment;
import org.jboss.as.console.client.shared.runtime.tx.TXParticipant;
import org.jboss.as.console.client.shared.runtime.tx.TXRecord;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqAcceptor;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqAddressingPattern;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqBridge;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqBroadcastGroup;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqClusterConnection;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnectionFactory;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnector;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnectorService;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqCoreQueue;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqDiscoveryGroup;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqDivert;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqJMSQueue;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqJMSTopic;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqMessagingProvider;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqSecurityPattern;
import org.jboss.as.console.client.shared.subsys.configadmin.model.ConfigAdminData;
import org.jboss.as.console.client.shared.subsys.ejb3.model.Module;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.shared.subsys.jca.model.JDBCDriver;
import org.jboss.as.console.client.shared.subsys.jca.model.JcaBootstrapContext;
import org.jboss.as.console.client.shared.subsys.jca.model.JcaWorkmanager;
import org.jboss.as.console.client.shared.subsys.jca.model.PoolConfig;
import org.jboss.as.console.client.shared.subsys.jca.model.WorkmanagerPool;
import org.jboss.as.console.client.shared.subsys.jca.model.XADataSource;
import org.jboss.as.console.client.shared.subsys.jgroups.JGroupsProtocol;
import org.jboss.as.console.client.shared.subsys.jgroups.JGroupsStack;
import org.jboss.as.console.client.shared.subsys.jgroups.JGroupsTransport;
import org.jboss.as.console.client.shared.subsys.jmx.model.JMXSubsystem;
import org.jboss.as.console.client.shared.subsys.jpa.model.JpaSubsystem;
import org.jboss.as.console.client.shared.subsys.mail.MailServerDefinition;
import org.jboss.as.console.client.shared.subsys.mail.MailSession;
import org.jboss.as.console.client.shared.subsys.messaging.model.Acceptor;
import org.jboss.as.console.client.shared.subsys.messaging.model.AddressingPattern;
import org.jboss.as.console.client.shared.subsys.messaging.model.Bridge;
import org.jboss.as.console.client.shared.subsys.messaging.model.BroadcastGroup;
import org.jboss.as.console.client.shared.subsys.messaging.model.ClusterConnection;
import org.jboss.as.console.client.shared.subsys.messaging.model.ConnectionFactory;
import org.jboss.as.console.client.shared.subsys.messaging.model.Connector;
import org.jboss.as.console.client.shared.subsys.messaging.model.ConnectorService;
import org.jboss.as.console.client.shared.subsys.messaging.model.DiscoveryGroup;
import org.jboss.as.console.client.shared.subsys.messaging.model.Divert;
import org.jboss.as.console.client.shared.subsys.messaging.model.MessagingProvider;
import org.jboss.as.console.client.shared.subsys.messaging.model.Queue;
import org.jboss.as.console.client.shared.subsys.messaging.model.SecurityPattern;
import org.jboss.as.console.client.shared.subsys.messaging.model.Topic;
import org.jboss.as.console.client.shared.subsys.modcluster.model.Modcluster;
import org.jboss.as.console.client.shared.subsys.modcluster.model.SSLConfig;
import org.jboss.as.console.client.shared.subsys.web.model.HttpConnector;
import org.jboss.as.console.client.shared.subsys.web.model.JSPContainerConfiguration;
import org.jboss.as.console.client.shared.subsys.web.model.VirtualServer;
import org.jboss.as.console.client.shared.subsys.ws.model.WebServiceEndpoint;
import org.jboss.as.console.client.shared.subsys.ws.model.WebServiceProvider;
import org.jboss.as.console.client.standalone.StandaloneServer;
import org.jboss.as.console.spi.BeanFactoryExtension;


/**
 * @author Heiko Braun
 * @date 2/22/11
 */
@BeanFactoryExtension
@AutoBeanFactory.Category(PropertyRecordCategory.class)
public interface CoreBeanFactory {

    AutoBean<BootstrapServer> bootstrapServer();
    AutoBean<ProfileRecord> profile();
    AutoBean<SubsystemRecord> subsystem();
    AutoBean<ServerGroupRecord> serverGroup();
    AutoBean<PropertyRecord> property();
    AutoBean<Host> host();
    AutoBean<Server> server();
    AutoBean<Jvm> jvm();
    AutoBean<ServerInstance> serverInstance();
    AutoBean<PatchInfo> patchInfo();
    AutoBean<RollbackOptions> rollbackOptions();

    AutoBean<DataSource> dataSource();
    AutoBean<XADataSource> xaDataSource();
    AutoBean<PoolConfig> poolConfig();

    // method names must reflect the type names. Hence the ActiveMQ autobeans are suffixed with 'Activemq'
    AutoBean<ActivemqAcceptor> activemqAcceptor();
    AutoBean<ActivemqAddressingPattern> activemqMessagingAddress();
    AutoBean<ActivemqBridge> activemqBridge();
    AutoBean<ActivemqBroadcastGroup> activemqBroadcastGroup();
    AutoBean<ActivemqClusterConnection> activemqClusterConnection();
    AutoBean<ActivemqConnectionFactory> activemqConnectionFactory();
    AutoBean<ActivemqConnector> activemqConnector();
    AutoBean<ActivemqConnectorService> activemqConnectorService();
    AutoBean<ActivemqDiscoveryGroup> activemqDiscoveryGroup();
    AutoBean<ActivemqDivert> activemqDivert();
    AutoBean<ActivemqMessagingProvider> activemqMessagingProvider();
    AutoBean<ActivemqJMSQueue> activemqQueue();
    AutoBean<ActivemqSecurityPattern> activemqMessagingSecurity();
    AutoBean<ActivemqJMSTopic> activemqTopic();
    AutoBean<ActivemqCoreQueue> activemqCoreQueue();

    AutoBean<Acceptor> acceptor();
    AutoBean<AddressingPattern> messagingAddress();
    AutoBean<Bridge> bridge();
    AutoBean<BroadcastGroup> broadcastGroup();
    AutoBean<ClusterConnection> clusterConnection();
    AutoBean<Connector> connector();
    AutoBean<ConnectionFactory> connectionFactory();
    AutoBean<ConnectorService> connectorService();
    AutoBean<DiscoveryGroup> discoveryGroup();
    AutoBean<Divert> divert();
    AutoBean<MessagingProvider> messagingProvider();
    AutoBean<Queue> hornetqQueue();
    AutoBean<SecurityPattern> messagingSecurity();
    AutoBean<Topic> topic();

    AutoBean<SocketBinding> socketBinding();
    AutoBean<RemoteSocketBinding> RemoteSocketBinding();
    AutoBean<LocalSocketBinding> LocalSocketBinding();
    AutoBean<SocketGroup> socketGroup();

    AutoBean<CommonSettings> settings();
    AutoBean<Document> indexDocument();

    AutoBean<HttpConnector> httpConnector();
    AutoBean<JSPContainerConfiguration> jspConfig();
    AutoBean<VirtualServer> virtualServer();

    AutoBean<Interface> interfaceDeclaration();
    AutoBean<JDBCDriver> jdbcDriver();

    AutoBean<StandaloneServer> standaloneServer();
    AutoBean<WebServiceEndpoint> webServiceEndpoint();
    AutoBean<WebServiceProvider> WebServiceProvider();

    AutoBean<ConfigAdminData> configAdminData();

    AutoBean<HeapMetric> heapMetric();
    AutoBean<ThreadMetric> threadMetric();
    AutoBean<RuntimeMetric> runtime();
    AutoBean<OSMetric> osmetric();

    AutoBean<JpaSubsystem> jpaSubystem();
    AutoBean<MailSession> mailSession();
    AutoBean<MailServerDefinition> mailServerDefinition();
    AutoBean<Modcluster> modcluster();
    AutoBean<SSLConfig> SSLConfig();
    AutoBean<JMXSubsystem> jmxSubsystem();
    AutoBean<Module> eeModuleRef();

    AutoBean<JcaBootstrapContext> JcaBootstrapContext();
    AutoBean<JcaWorkmanager> JcaWorkmanager();
    AutoBean<WorkmanagerPool> WorkmanagerPool();

    AutoBean<JPADeployment> jpaDeployment();

    AutoBean<JGroupsStack> jGroupsStack();
    AutoBean<JGroupsProtocol> jGroupsProtocol();
    AutoBean<JGroupsTransport> jGroupsTransport();

    AutoBean<Path> path();

    AutoBean<Extension> extension();

    AutoBean<TXRecord> txRecord();
    AutoBean<TXParticipant> txParticipant();

    // RBAC and related
    AutoBean<AuditLogItem> auditLogItem();
}
