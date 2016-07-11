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

package org.jboss.as.console.client.core.gin;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.inject.client.AsyncProvider;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.annotations.DefaultGatekeeper;
import com.gwtplatform.mvp.client.googleanalytics.GoogleAnalytics;
import com.gwtplatform.mvp.client.proxy.Gatekeeper;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.shared.proxy.TokenFormatter;
import org.jboss.as.console.client.administration.accesscontrol.AccessControlFinder;
import org.jboss.as.console.client.administration.audit.AuditLogPresenter;
import org.jboss.as.console.client.analytics.NavigationTracker;
import org.jboss.as.console.client.auth.CurrentUser;
import org.jboss.as.console.client.auth.SignInPagePresenter;
import org.jboss.as.console.client.core.ApplicationProperties;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.FeatureSet;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.RequiredResourcesProcessor;
import org.jboss.as.console.client.core.bootstrap.Bootstrapper;
import org.jboss.as.console.client.core.message.MessageBar;
import org.jboss.as.console.client.core.message.MessageCenter;
import org.jboss.as.console.client.core.message.MessageCenterView;
import org.jboss.as.console.client.core.settings.ModelVersions;
import org.jboss.as.console.client.core.settings.SettingsPresenter;
import org.jboss.as.console.client.core.settings.SettingsPresenterWidget;
import org.jboss.as.console.client.domain.groups.ServerGroupPresenter;
import org.jboss.as.console.client.domain.hosts.HostMgmtPresenter;
import org.jboss.as.console.client.domain.hosts.HostVMMetricPresenter;
import org.jboss.as.console.client.domain.hosts.ServerConfigPresenter;
import org.jboss.as.console.client.domain.hosts.general.HostInterfacesPresenter;
import org.jboss.as.console.client.domain.hosts.general.HostJVMPresenter;
import org.jboss.as.console.client.domain.hosts.general.HostPropertiesPresenter;
import org.jboss.as.console.client.domain.model.HostInformationStore;
import org.jboss.as.console.client.domain.model.ProfileDAO;
import org.jboss.as.console.client.domain.model.ServerGroupDAO;
import org.jboss.as.console.client.domain.profiles.CurrentProfileSelection;
import org.jboss.as.console.client.domain.profiles.ProfileMgmtPresenter;
import org.jboss.as.console.client.domain.runtime.DomainRuntimePresenter;
import org.jboss.as.console.client.domain.runtime.DomainRuntimegateKeeper;
import org.jboss.as.console.client.domain.runtime.NoServerPresenter;
import org.jboss.as.console.client.domain.topology.TopologyPresenter;
import org.jboss.as.console.client.meta.Capabilities;
import org.jboss.as.console.client.plugins.RequiredResourcesRegistry;
import org.jboss.as.console.client.plugins.RuntimeExtensionRegistry;
import org.jboss.as.console.client.plugins.SearchIndexRegistry;
import org.jboss.as.console.client.plugins.SubsystemRegistry;
import org.jboss.as.console.client.preview.PreviewContentFactory;
import org.jboss.as.console.client.rbac.HostManagementGatekeeper;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.rbac.UnauthorisedPresenter;
import org.jboss.as.console.client.search.Harvest;
import org.jboss.as.console.client.search.Index;
import org.jboss.as.console.client.shared.DialogPresenter;
import org.jboss.as.console.client.shared.expr.ExpressionResolver;
import org.jboss.as.console.client.shared.general.InterfacePresenter;
import org.jboss.as.console.client.shared.general.PathManagementPresenter;
import org.jboss.as.console.client.shared.general.PropertiesPresenter;
import org.jboss.as.console.client.shared.general.SocketBindingPresenter;
import org.jboss.as.console.client.shared.help.HelpSystem;
import org.jboss.as.console.client.shared.homepage.HomepagePresenter;
import org.jboss.as.console.client.shared.model.SubsystemLoader;
import org.jboss.as.console.client.shared.patching.PatchManagementPresenter;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.runtime.activemq.ActivemqMetricPresenter;
import org.jboss.as.console.client.shared.runtime.ds.DataSourceMetricPresenter;
import org.jboss.as.console.client.shared.runtime.env.EnvironmentPresenter;
import org.jboss.as.console.client.shared.runtime.jms.JMSMetricPresenter;
import org.jboss.as.console.client.shared.runtime.jpa.JPAMetricPresenter;
import org.jboss.as.console.client.shared.runtime.logging.files.LogFilesPresenter;
import org.jboss.as.console.client.shared.runtime.logging.viewer.LogViewerPresenter;
import org.jboss.as.console.client.shared.runtime.naming.JndiPresenter;
import org.jboss.as.console.client.shared.runtime.tx.TXLogPresenter;
import org.jboss.as.console.client.shared.runtime.tx.TXMetricPresenter;
import org.jboss.as.console.client.shared.runtime.web.WebMetricPresenter;
import org.jboss.as.console.client.shared.runtime.ws.WebServiceRuntimePresenter;
import org.jboss.as.console.client.shared.state.ReloadState;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.activemq.ActivemqFinder;
import org.jboss.as.console.client.shared.subsys.activemq.JMSBridgePresenter;
import org.jboss.as.console.client.shared.subsys.batch.BatchPresenter;
import org.jboss.as.console.client.shared.subsys.configadmin.ConfigAdminPresenter;
import org.jboss.as.console.client.shared.subsys.ejb3.EEPresenter;
import org.jboss.as.console.client.shared.subsys.ejb3.EJB3Presenter;
import org.jboss.as.console.client.shared.subsys.generic.GenericSubsystemPresenter;
import org.jboss.as.console.client.shared.subsys.iiopopenjdk.IiopOpenJdkPresenter;
import org.jboss.as.console.client.shared.subsys.infinispan.v3.CacheFinderPresenter;
import org.jboss.as.console.client.shared.subsys.infinispan.v3.CachesPresenter;
import org.jboss.as.console.client.shared.subsys.io.IOPresenter;
import org.jboss.as.console.client.shared.subsys.jberet.JberetMetricsPresenter;
import org.jboss.as.console.client.shared.subsys.jberet.JberetPresenter;
import org.jboss.as.console.client.shared.subsys.jca.DataSourceFinder;
import org.jboss.as.console.client.shared.subsys.jca.DataSourcePresenter;
import org.jboss.as.console.client.shared.subsys.jca.JcaPresenter;
import org.jboss.as.console.client.shared.subsys.jca.ResourceAdapterFinder;
import org.jboss.as.console.client.shared.subsys.jca.ResourceAdapterPresenter;
import org.jboss.as.console.client.shared.subsys.jca.XADataSourcePresenter;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceStore;
import org.jboss.as.console.client.shared.subsys.jca.model.DomainDriverStrategy;
import org.jboss.as.console.client.shared.subsys.jca.model.DriverRegistry;
import org.jboss.as.console.client.shared.subsys.jca.model.StandaloneDriverStrategy;
import org.jboss.as.console.client.shared.subsys.jgroups.JGroupsPresenter;
import org.jboss.as.console.client.shared.subsys.jmx.JMXPresenter;
import org.jboss.as.console.client.shared.subsys.jpa.JpaPresenter;
import org.jboss.as.console.client.shared.subsys.logger.LoggerPresenter;
import org.jboss.as.console.client.shared.subsys.mail.MailFinder;
import org.jboss.as.console.client.shared.subsys.mail.MailPresenter;
import org.jboss.as.console.client.shared.subsys.messaging.HornetqFinder;
import org.jboss.as.console.client.shared.subsys.modcluster.ModclusterPresenter;
import org.jboss.as.console.client.shared.subsys.picketlink.FederationPresenter;
import org.jboss.as.console.client.shared.subsys.picketlink.PicketLinkFinder;
import org.jboss.as.console.client.shared.subsys.picketlink.ServiceProviderPresenter;
import org.jboss.as.console.client.shared.subsys.remoting.RemotingPresenter;
import org.jboss.as.console.client.shared.subsys.security.v3.SecDomainFinder;
import org.jboss.as.console.client.shared.subsys.security.v3.SecDomainPresenter;
import org.jboss.as.console.client.shared.subsys.undertow.FilterPresenter;
import org.jboss.as.console.client.shared.subsys.undertow.HttpMetricPresenter;
import org.jboss.as.console.client.shared.subsys.undertow.HttpPresenter;
import org.jboss.as.console.client.shared.subsys.undertow.ServletPresenter;
import org.jboss.as.console.client.shared.subsys.undertow.UndertowFinder;
import org.jboss.as.console.client.shared.subsys.undertow.UndertowPresenter;
import org.jboss.as.console.client.shared.subsys.web.WebPresenter;
import org.jboss.as.console.client.shared.subsys.ws.DomainEndpointStrategy;
import org.jboss.as.console.client.shared.subsys.ws.EndpointRegistry;
import org.jboss.as.console.client.shared.subsys.ws.StandaloneEndpointStrategy;
import org.jboss.as.console.client.shared.subsys.ws.WebServicePresenter;
import org.jboss.as.console.client.standalone.ServerMgmtApplicationPresenter;
import org.jboss.as.console.client.standalone.StandaloneServerPresenter;
import org.jboss.as.console.client.standalone.deploymentscanner.DeploymentScannerPresenter;
import org.jboss.as.console.client.standalone.runtime.StandaloneRuntimePresenter;
import org.jboss.as.console.client.standalone.runtime.VMMetricsPresenter;
import org.jboss.as.console.client.tools.ToolsPresenter;
import org.jboss.as.console.client.tools.modelling.workbench.repository.RepositoryPresenter;
import org.jboss.as.console.client.v3.deployment.DeploymentDetailsPresenter;
import org.jboss.as.console.client.v3.deployment.DomainDeploymentFinder;
import org.jboss.as.console.client.v3.deployment.StandaloneDeploymentFinder;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.spi.GinExtension;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.HandlerMapping;
import org.jboss.dmr.client.dispatch.impl.DMRHandler;
import org.jboss.dmr.client.dispatch.impl.UploadHandler;
import org.jboss.gwt.circuit.Dispatcher;


/**
 * Overall module configuration.
 *
 * @see CoreUIModule
 *
 * @author Heiko Braun
 * @date 1/31/11
 */
@GinExtension
public interface CoreUI {


    SubsystemRegistry getSubsystemRegistry();
    RuntimeExtensionRegistry getRuntimeLHSItemExtensionRegistry();

    PlaceManager getPlaceManager();
    EventBus getEventBus();
    //ProxyFailureHandler getProxyFailureHandler();
    TokenFormatter getTokenFormatter();

    @DefaultGatekeeper
    Gatekeeper getRBACGatekeeper();

    HostManagementGatekeeper getHostManagementGatekeeper();
    DomainRuntimegateKeeper getDomainRuntimegateKeeper();

    CurrentUser getCurrentUser();
    BootstrapContext getBootstrapContext();
    ApplicationProperties getAppProperties();

    GoogleAnalytics getAnalytics();
    NavigationTracker getTracker();

    Harvest getHarvest();
    Index getIndex();
    FeatureSet getFeatureSet();

    Scheduler getScheduler();

    // ----------------------------------------------------------------------

    Header getHeader();
    Footer getFooter();

    MessageBar getMessageBar();
    MessageCenter getMessageCenter();
    MessageCenterView getMessageCenterView();

    HelpSystem getHelpSystem();

    ExpressionResolver getExpressionManager();
    Baseadress getBaseadress();
    RuntimeBaseAddress getRuntimeBaseAddress();

    ModelVersions modelVersions();

    // ----------------------------------------------------------------------

    DispatchAsync getDispatchAsync();
    HandlerMapping getDispatcherHandlerRegistry();
    DMRHandler getDMRHandler();
    UploadHandler getUploadHHandler();

    ApplicationMetaData getApplicationMetaData();
    Capabilities getCapabilities();

    // ----------------------------------------------------------------------
    AsyncProvider<HomepagePresenter> getHomepagePresenter();

    Provider<SignInPagePresenter> getSignInPagePresenter();
    AsyncProvider<MainLayoutPresenter> getMainLayoutPresenter();
    AsyncProvider<ToolsPresenter> getToolsPresenter();

    //AsyncProvider<DebugPresenter> getDebugPresenter();

    AsyncProvider<SettingsPresenter> getSettingsPresenter();
    AsyncProvider<SettingsPresenterWidget> getSettingsPresenterWidget();


    // ----------------------------------------------------------------------
    AsyncProvider<ServerMgmtApplicationPresenter> getServerManagementAppPresenter();


    // ----------------------------------------------------------------------
    // domain config below
    AsyncProvider<ProfileMgmtPresenter> getProfileMgmtPresenter();
    CurrentProfileSelection getCurrentSelectedProfile();
    ReloadState getReloadState();

    AsyncProvider<TopologyPresenter> getServerGroupHostMatrixPresenter();
    AsyncProvider<ServerGroupPresenter> getServerGroupsPresenter();

    ProfileDAO getProfileDAO();
    SubsystemLoader getSubsystemLoader();
    ServerGroupDAO getServerGroupDAO();
    HostInformationStore getHostInfoStore();


    AsyncProvider<HostMgmtPresenter> getHostMgmtPresenter();
    AsyncProvider<ServerConfigPresenter> getServerPresenter();

    // ----------------------------------------------------------------------
    // shared subsystems
    AsyncProvider<DataSourcePresenter> getDataSourcePresenter();
    AsyncProvider<XADataSourcePresenter> getXADataSourcePresenter();

    DataSourceStore getDataSourceStore();

    DomainDriverStrategy getDomainDriverStrategy();
    StandaloneDriverStrategy getStandloneDriverStrategy();
    DriverRegistry getDriverRegistry();

    AsyncProvider<org.jboss.as.console.client.shared.subsys.messaging.MsgDestinationsPresenter> getMsgDestinationsPresenter();
    AsyncProvider<org.jboss.as.console.client.shared.subsys.activemq.MsgDestinationsPresenter> getActivemqMsgDestinationsPresenter();
    AsyncProvider<org.jboss.as.console.client.shared.subsys.messaging.connections.MsgConnectionsPresenter> getMsgConnectionsPresenter();
    AsyncProvider<org.jboss.as.console.client.shared.subsys.activemq.connections.MsgConnectionsPresenter> getActivemqMsgConnectionsPresenter();
    AsyncProvider<org.jboss.as.console.client.shared.subsys.messaging.cluster.MsgClusteringPresenter> getMsgClusteringPresenter();
    AsyncProvider<org.jboss.as.console.client.shared.subsys.activemq.cluster.MsgClusteringPresenter> getActivemqMsgClusteringPresenter();
    AsyncProvider<JMSBridgePresenter> getJMSBridgePresenter();

    AsyncProvider<LogFilesPresenter> getLogFilesPresenter();
    AsyncProvider<LogViewerPresenter> getLogViewerPresenter();

    AsyncProvider<ConfigAdminPresenter> getConfigAdminPresenter();
    AsyncProvider<SocketBindingPresenter> getSocketBindingPresenter();

    AsyncProvider<WebPresenter> getWebPresenter();

    AsyncProvider<InterfacePresenter> getInterfacePresenter();
    AsyncProvider<PropertiesPresenter> getDomainPropertiesPresenter();

    AsyncProvider<HostPropertiesPresenter> getHostPropertiesPresenter();
    AsyncProvider<HostJVMPresenter> getHostJVMPresenter();
    AsyncProvider<HostInterfacesPresenter> getHostInterfacesPresenter();

    AsyncProvider<StandaloneServerPresenter> getStandaloneServerPresenter();

    AsyncProvider<WebServicePresenter> getWebServicePresenter();
    AsyncProvider<WebServiceRuntimePresenter> getWebServiceRuntimePresenter();

    EndpointRegistry getEndpointRegistry();
    DomainEndpointStrategy getDomainEndpointStrategy();
    StandaloneEndpointStrategy getStandaloneEndpointStrategy();

    AsyncProvider<ResourceAdapterPresenter> getResourceAdapterPresenter();
    AsyncProvider<ResourceAdapterFinder> getResourceAdapterFinder();
    AsyncProvider<JndiPresenter> getJndiPresenter();

    AsyncProvider<VMMetricsPresenter> getVMMetricsPresenter();
    AsyncProvider<HostVMMetricPresenter> getServerVMMetricPresenter();


    AsyncProvider<StandaloneRuntimePresenter> getRuntimePresenter();
    AsyncProvider<DomainRuntimePresenter> getDomainRuntimePresenter();
    AsyncProvider<TXMetricPresenter> getTXMetricPresenter();
    AsyncProvider<TXLogPresenter> getTXLogPresenter();

    AsyncProvider<JpaPresenter> getJpaPresenter();
    AsyncProvider<MailPresenter> getMailPresenter();
    AsyncProvider<MailFinder> getMailFinder();
    AsyncProvider<ModclusterPresenter> getModclusterPresenter();
    AsyncProvider<JMXPresenter> getJMXPresenter();
    AsyncProvider<EEPresenter> getEEPresenter();
    AsyncProvider<RemotingPresenter> getRemotingPresenter();

    AsyncProvider<JcaPresenter> getJcaPresenter();

    AsyncProvider<WebMetricPresenter> WebMetricPresenter();

    AsyncProvider<JMSMetricPresenter> JMSMetricPresenter();
    AsyncProvider<ActivemqMetricPresenter> ActivemqMetricPresenter();

    AsyncProvider<DataSourceMetricPresenter> DataSourceMetricPresenter();

    AsyncProvider<JPAMetricPresenter> JPAMetricPresenter();

    AsyncProvider<JGroupsPresenter> JGroupsPresenter();

    AsyncProvider<PathManagementPresenter> PathManagementPresenter();

    AsyncProvider<EnvironmentPresenter> EnvironmentPresenter();

    AsyncProvider<PatchManagementPresenter> getPatchManagerProvider();

    AsyncProvider<BatchPresenter> getBatchPresenter();
    AsyncProvider<JberetPresenter> getJberetPresenter();
    AsyncProvider<JberetMetricsPresenter> getJberetMetricsPresenter();

    AsyncProvider<IOPresenter> getIOPresenter();

    AsyncProvider<PicketLinkFinder> getPicketLinkFinder();
    AsyncProvider<FederationPresenter> getFederationPresenter();
    AsyncProvider<ServiceProviderPresenter> getServiceProviderPresenter();

    // Administration
    AsyncProvider<AccessControlFinder> getRbacFinder();
    AsyncProvider<AuditLogPresenter> getAuditLogPresenter();

    // mbui workbench
    Provider<RepositoryPresenter> getRepositoryPresenter();

    RequiredResourcesRegistry getRequiredResourcesRegistry();

    SearchIndexRegistry getSearchIndexRegistry();

    SecurityFramework getSecurityFramework();

    UnauthorisedPresenter getUnauthorisedPresenter();

    AsyncProvider<DialogPresenter> getDialogPresenter();

    AsyncProvider<NoServerPresenter> getNoServerPresenter();

    AsyncProvider<IiopOpenJdkPresenter> getIiopOpenJdkPresenter();
    AsyncProvider<DomainDeploymentFinder> getDomainDeploymentFinder();
    AsyncProvider<StandaloneDeploymentFinder> getStandaloneDeploymentFinder();
    AsyncProvider<DeploymentDetailsPresenter> getDeplymentDetailsPresenter();
    AsyncProvider<DeploymentScannerPresenter> getDeploymentScannerPresenter();

    Dispatcher getCircuitDispatcher();

    CoreGUIContext getCoreGUIContext();

    AsyncProvider<HttpPresenter> getHttpPresenter();
    AsyncProvider<ServletPresenter> getServletPresenter();
    AsyncProvider<UndertowPresenter> getUndertowPresenter();
    AsyncProvider<FilterPresenter> getFilterPresenter();
    AsyncProvider<HttpMetricPresenter> getHttpMetricPresenter();


    RequiredResourcesProcessor getRequiredResourcesProcessor();

    PreviewContentFactory getPreviewContentFactory();

    Bootstrapper getBootstrapper();

    AsyncProvider<CacheFinderPresenter> getCachePresenter();
    AsyncProvider<CachesPresenter> getCachesPresenter();

    AsyncProvider<HornetqFinder> getHornetqFinderPresenter();
    AsyncProvider<ActivemqFinder> getActiveMQFinderPresenter();

    AsyncProvider<EJB3Presenter> getEJBPresenter();

    AsyncProvider<LoggerPresenter> getLoggerPresenter();

    AsyncProvider<SecDomainFinder> getSecDomainFinder();

    AsyncProvider<SecDomainPresenter> getSecDomainPresenter();

    AsyncProvider<UndertowFinder> getUndertowFinder();

    AsyncProvider<DataSourceFinder> getDataSourceFinder();
    AsyncProvider<GenericSubsystemPresenter> getGenericSubsystemPresenter();
}
