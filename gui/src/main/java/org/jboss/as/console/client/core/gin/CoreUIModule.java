
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
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.RootPresenter;
import com.gwtplatform.mvp.client.annotations.GaAccount;
import com.gwtplatform.mvp.client.gin.AbstractPresenterModule;
import com.gwtplatform.mvp.client.googleanalytics.GoogleAnalytics;
import com.gwtplatform.mvp.client.proxy.Gatekeeper;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.shared.proxy.TokenFormatter;
import org.jboss.as.console.client.ResourceLoader;
import org.jboss.as.console.client.administration.AdministrationPresenter;
import org.jboss.as.console.client.administration.AdministrationView;
import org.jboss.as.console.client.administration.audit.AuditLogPresenter;
import org.jboss.as.console.client.administration.audit.AuditLogView;
import org.jboss.as.console.client.administration.role.RoleAssignmentPresenter;
import org.jboss.as.console.client.administration.role.ui.RoleAssignementView;
import org.jboss.as.console.client.analytics.AnalyticsProvider;
import org.jboss.as.console.client.analytics.NavigationTracker;
import org.jboss.as.console.client.auth.CurrentUser;
import org.jboss.as.console.client.auth.SignInPagePresenter;
import org.jboss.as.console.client.auth.SignInPageView;
import org.jboss.as.console.client.core.ApplicationProperties;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.DefaultPlaceManager;
import org.jboss.as.console.client.core.FeatureSet;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.MainLayoutViewImpl;
import org.jboss.as.console.client.core.NameTokenRegistry;
import org.jboss.as.console.client.core.NewTokenFormatter;
import org.jboss.as.console.client.core.RequiredResourcesProcessor;
import org.jboss.as.console.client.core.ToplevelTabs;
import org.jboss.as.console.client.core.bootstrap.Bootstrapper;
import org.jboss.as.console.client.core.bootstrap.cors.BootstrapServerSetup;
import org.jboss.as.console.client.core.bootstrap.hal.BootstrapSteps;
import org.jboss.as.console.client.core.bootstrap.hal.EagerLoadGroups;
import org.jboss.as.console.client.core.bootstrap.hal.EagerLoadProfiles;
import org.jboss.as.console.client.core.bootstrap.hal.ExecutionMode;
import org.jboss.as.console.client.core.bootstrap.hal.HostStoreInit;
import org.jboss.as.console.client.core.bootstrap.hal.LoadCompatMatrix;
import org.jboss.as.console.client.core.bootstrap.hal.LoadGoogleViz;
import org.jboss.as.console.client.core.bootstrap.hal.RegisterSubsystems;
import org.jboss.as.console.client.core.bootstrap.hal.ServerStoreInit;
import org.jboss.as.console.client.core.bootstrap.hal.TrackExecutionMode;
import org.jboss.as.console.client.core.message.MessageBar;
import org.jboss.as.console.client.core.message.MessageCenter;
import org.jboss.as.console.client.core.message.MessageCenterImpl;
import org.jboss.as.console.client.core.message.MessageCenterView;
import org.jboss.as.console.client.core.settings.ModelVersions;
import org.jboss.as.console.client.core.settings.SettingsPresenter;
import org.jboss.as.console.client.core.settings.SettingsPresenterViewImpl;
import org.jboss.as.console.client.core.settings.SettingsPresenterWidget;
import org.jboss.as.console.client.core.settings.SettingsView;
import org.jboss.as.console.client.csp.CSPPresenter;
import org.jboss.as.console.client.csp.CSPView;
import org.jboss.as.console.client.domain.groups.ServerGroupPresenter;
import org.jboss.as.console.client.domain.groups.ServerGroupView;
import org.jboss.as.console.client.domain.groups.deployment.DomainDeploymentPresenter;
import org.jboss.as.console.client.domain.groups.deployment.DomainDeploymentView;
import org.jboss.as.console.client.domain.hosts.ColumnHostView;
import org.jboss.as.console.client.domain.hosts.HostMgmtPresenter;
import org.jboss.as.console.client.domain.hosts.HostVMMetricPresenter;
import org.jboss.as.console.client.domain.hosts.HostVMMetricView;
import org.jboss.as.console.client.domain.hosts.ServerConfigPresenter;
import org.jboss.as.console.client.domain.hosts.ServerConfigView;
import org.jboss.as.console.client.domain.hosts.general.HostInterfacesPresenter;
import org.jboss.as.console.client.domain.hosts.general.HostInterfacesView;
import org.jboss.as.console.client.domain.hosts.general.HostJVMPresenter;
import org.jboss.as.console.client.domain.hosts.general.HostJVMView;
import org.jboss.as.console.client.domain.hosts.general.HostPropertiesPresenter;
import org.jboss.as.console.client.domain.hosts.general.HostPropertiesView;
import org.jboss.as.console.client.domain.model.HostInformationStore;
import org.jboss.as.console.client.domain.model.ProfileDAO;
import org.jboss.as.console.client.domain.model.ServerGroupDAO;
import org.jboss.as.console.client.domain.model.impl.HostInfoStoreImpl;
import org.jboss.as.console.client.domain.model.impl.ProfileDAOImpl;
import org.jboss.as.console.client.domain.model.impl.ServerGroupDAOImpl;
import org.jboss.as.console.client.domain.profiles.ColumnProfileView;
import org.jboss.as.console.client.domain.profiles.CurrentProfileSelection;
import org.jboss.as.console.client.domain.profiles.ProfileMgmtPresenter;
import org.jboss.as.console.client.domain.runtime.DomainRuntimePresenter;
import org.jboss.as.console.client.domain.runtime.DomainRuntimeView;
import org.jboss.as.console.client.domain.runtime.DomainRuntimegateKeeper;
import org.jboss.as.console.client.domain.runtime.NoServerPresenter;
import org.jboss.as.console.client.domain.runtime.NoServerView;
import org.jboss.as.console.client.domain.topology.TopologyPresenter;
import org.jboss.as.console.client.domain.topology.TopologyView;
import org.jboss.as.console.client.plugins.RequiredResourcesRegistry;
import org.jboss.as.console.client.plugins.RequiredResourcesRegistryImpl;
import org.jboss.as.console.client.plugins.RuntimeExtensionRegistry;
import org.jboss.as.console.client.plugins.RuntimeLHSItemExtensionRegistryImpl;
import org.jboss.as.console.client.plugins.SearchIndexRegistry;
import org.jboss.as.console.client.plugins.SearchIndexRegistryImpl;
import org.jboss.as.console.client.plugins.SubsystemRegistry;
import org.jboss.as.console.client.plugins.SubsystemRegistryImpl;
import org.jboss.as.console.client.preview.PreviewContentFactory;
import org.jboss.as.console.client.preview.PreviewContentFactoryImpl;
import org.jboss.as.console.client.rbac.HostManagementGatekeeper;
import org.jboss.as.console.client.rbac.PlaceRequestSecurityFramework;
import org.jboss.as.console.client.rbac.RBACGatekeeper;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.rbac.SecurityFrameworkImpl;
import org.jboss.as.console.client.rbac.UnauthorisedPresenter;
import org.jboss.as.console.client.rbac.UnauthorisedView;
import org.jboss.as.console.client.search.Harvest;
import org.jboss.as.console.client.search.Index;
import org.jboss.as.console.client.search.IndexProvider;
import org.jboss.as.console.client.shared.DialogPresenter;
import org.jboss.as.console.client.shared.DialogView;
import org.jboss.as.console.client.shared.DialogViewImpl;
import org.jboss.as.console.client.shared.deployment.DeploymentStore;
import org.jboss.as.console.client.shared.expr.DefaultExpressionResolver;
import org.jboss.as.console.client.shared.expr.ExpressionResolver;
import org.jboss.as.console.client.shared.general.InterfacePresenter;
import org.jboss.as.console.client.shared.general.InterfaceView;
import org.jboss.as.console.client.shared.general.PathManagementPresenter;
import org.jboss.as.console.client.shared.general.PathManagementView;
import org.jboss.as.console.client.shared.general.PropertiesPresenter;
import org.jboss.as.console.client.shared.general.PropertiesView;
import org.jboss.as.console.client.shared.general.SocketBindingPresenter;
import org.jboss.as.console.client.shared.general.SocketBindingView;
import org.jboss.as.console.client.shared.help.HelpSystem;
import org.jboss.as.console.client.shared.homepage.HomepagePresenter;
import org.jboss.as.console.client.shared.homepage.HomepageView;
import org.jboss.as.console.client.shared.model.SubsystemLoader;
import org.jboss.as.console.client.shared.model.SubsystemStoreImpl;
import org.jboss.as.console.client.shared.patching.PatchManagementPresenter;
import org.jboss.as.console.client.shared.patching.PatchManager;
import org.jboss.as.console.client.shared.patching.ui.PatchManagementView;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.runtime.ds.DataSourceMetricPresenter;
import org.jboss.as.console.client.shared.runtime.ds.DataSourceMetricView;
import org.jboss.as.console.client.shared.runtime.env.EnvironmentPresenter;
import org.jboss.as.console.client.shared.runtime.env.EnvironmentView;
import org.jboss.as.console.client.shared.runtime.jms.JMSMetricPresenter;
import org.jboss.as.console.client.shared.runtime.jms.JMSMetricView;
import org.jboss.as.console.client.shared.runtime.jpa.JPAMetricPresenter;
import org.jboss.as.console.client.shared.runtime.jpa.JPAMetricsView;
import org.jboss.as.console.client.shared.runtime.logging.files.LogFilesPresenter;
import org.jboss.as.console.client.shared.runtime.logging.files.LogFilesView;
import org.jboss.as.console.client.shared.runtime.logging.viewer.LogViewerPresenter;
import org.jboss.as.console.client.shared.runtime.logging.viewer.LogViewerView;
import org.jboss.as.console.client.shared.runtime.naming.JndiPresenter;
import org.jboss.as.console.client.shared.runtime.naming.JndiView;
import org.jboss.as.console.client.shared.runtime.tx.TXLogPresenter;
import org.jboss.as.console.client.shared.runtime.tx.TXLogView;
import org.jboss.as.console.client.shared.runtime.tx.TXMetricPresenter;
import org.jboss.as.console.client.shared.runtime.tx.TXMetricViewImpl;
import org.jboss.as.console.client.shared.runtime.web.WebMetricPresenter;
import org.jboss.as.console.client.shared.runtime.web.WebMetricView;
import org.jboss.as.console.client.shared.runtime.ws.WebServiceRuntimePresenter;
import org.jboss.as.console.client.shared.runtime.ws.WebServiceRuntimeView;
import org.jboss.as.console.client.shared.state.ReloadState;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.batch.BatchPresenter;
import org.jboss.as.console.client.shared.subsys.batch.ui.BatchView;
import org.jboss.as.console.client.shared.subsys.configadmin.ConfigAdminPresenter;
import org.jboss.as.console.client.shared.subsys.configadmin.ConfigAdminView;
import org.jboss.as.console.client.shared.subsys.ejb3.EEPresenter;
import org.jboss.as.console.client.shared.subsys.ejb3.EESubsystemView;
import org.jboss.as.console.client.shared.subsys.ejb3.EJB3Presenter;
import org.jboss.as.console.client.shared.subsys.ejb3.EJBView;
import org.jboss.as.console.client.shared.subsys.iiopopenjdk.IiopOpenJdkPresenter;
import org.jboss.as.console.client.shared.subsys.iiopopenjdk.IiopOpenJdkView;
import org.jboss.as.console.client.shared.subsys.infinispan.v3.CacheFinder;
import org.jboss.as.console.client.shared.subsys.infinispan.v3.CacheFinderPresenter;
import org.jboss.as.console.client.shared.subsys.infinispan.v3.CachesPresenter;
import org.jboss.as.console.client.shared.subsys.infinispan.v3.CachesView;
import org.jboss.as.console.client.shared.subsys.io.IOPresenter;
import org.jboss.as.console.client.shared.subsys.io.IOView;
import org.jboss.as.console.client.shared.subsys.jca.DataSourcePresenter;
import org.jboss.as.console.client.shared.subsys.jca.DatasourceView;
import org.jboss.as.console.client.shared.subsys.jca.JcaPresenter;
import org.jboss.as.console.client.shared.subsys.jca.JcaSubsystemView;
import org.jboss.as.console.client.shared.subsys.jca.ResourceAdapterPresenter;
import org.jboss.as.console.client.shared.subsys.jca.ResourceAdapterView;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceStore;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceStoreImpl;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSourceTemplates;
import org.jboss.as.console.client.shared.subsys.jca.model.DomainDriverStrategy;
import org.jboss.as.console.client.shared.subsys.jca.model.StandaloneDriverStrategy;
import org.jboss.as.console.client.shared.subsys.jgroups.JGroupsPresenter;
import org.jboss.as.console.client.shared.subsys.jgroups.JGroupsSubsystemView;
import org.jboss.as.console.client.shared.subsys.jmx.JMXPresenter;
import org.jboss.as.console.client.shared.subsys.jmx.JMXSubsystemView;
import org.jboss.as.console.client.shared.subsys.jpa.JpaPresenter;
import org.jboss.as.console.client.shared.subsys.jpa.JpaView;
import org.jboss.as.console.client.shared.subsys.logger.LoggerPresenter;
import org.jboss.as.console.client.shared.subsys.logger.LoggerView;
import org.jboss.as.console.client.shared.subsys.mail.MailFinder;
import org.jboss.as.console.client.shared.subsys.mail.MailFinderView;
import org.jboss.as.console.client.shared.subsys.mail.MailPresenter;
import org.jboss.as.console.client.shared.subsys.mail.MailSubsystemView;
import org.jboss.as.console.client.shared.subsys.messaging.HornetqFinder;
import org.jboss.as.console.client.shared.subsys.messaging.HornetqFinderView;
import org.jboss.as.console.client.shared.subsys.messaging.MsgDestinationsPresenter;
import org.jboss.as.console.client.shared.subsys.messaging.MsgDestinationsView;
import org.jboss.as.console.client.shared.subsys.messaging.cluster.MsgClusteringPresenter;
import org.jboss.as.console.client.shared.subsys.messaging.cluster.MsgClusteringView;
import org.jboss.as.console.client.shared.subsys.messaging.connections.MsgConnectionsPresenter;
import org.jboss.as.console.client.shared.subsys.messaging.connections.MsgConnectionsView;
import org.jboss.as.console.client.shared.subsys.modcluster.ModclusterPresenter;
import org.jboss.as.console.client.shared.subsys.modcluster.ModclusterView;
import org.jboss.as.console.client.shared.subsys.remoting.RemotingPresenter;
import org.jboss.as.console.client.shared.subsys.remoting.ui.RemotingView;
import org.jboss.as.console.client.shared.subsys.security.v3.SecDomainFinder;
import org.jboss.as.console.client.shared.subsys.security.v3.SecDomainFinderView;
import org.jboss.as.console.client.shared.subsys.security.v3.SecDomainPresenter;
import org.jboss.as.console.client.shared.subsys.security.v3.SecDomainView;
import org.jboss.as.console.client.shared.subsys.undertow.HttpMetricPresenter;
import org.jboss.as.console.client.shared.subsys.undertow.HttpMetricView;
import org.jboss.as.console.client.shared.subsys.undertow.HttpPresenter;
import org.jboss.as.console.client.shared.subsys.undertow.HttpView;
import org.jboss.as.console.client.shared.subsys.undertow.ServletPresenter;
import org.jboss.as.console.client.shared.subsys.undertow.ServletView;
import org.jboss.as.console.client.shared.subsys.undertow.UndertowPresenter;
import org.jboss.as.console.client.shared.subsys.undertow.UndertowView;
import org.jboss.as.console.client.shared.subsys.web.WebPresenter;
import org.jboss.as.console.client.shared.subsys.web.WebSubsystemView;
import org.jboss.as.console.client.shared.subsys.ws.DomainEndpointStrategy;
import org.jboss.as.console.client.shared.subsys.ws.EndpointRegistry;
import org.jboss.as.console.client.shared.subsys.ws.StandaloneEndpointStrategy;
import org.jboss.as.console.client.shared.subsys.ws.WebServicePresenter;
import org.jboss.as.console.client.shared.subsys.ws.WebServiceView;
import org.jboss.as.console.client.standalone.ColumnServerView;
import org.jboss.as.console.client.standalone.ServerMgmtApplicationPresenter;
import org.jboss.as.console.client.standalone.StandaloneServerPresenter;
import org.jboss.as.console.client.standalone.StandaloneServerView;
import org.jboss.as.console.client.standalone.deployment.StandaloneDeploymentPresenter;
import org.jboss.as.console.client.standalone.deployment.StandaloneDeploymentView;
import org.jboss.as.console.client.standalone.runtime.StandaloneRuntimePresenter;
import org.jboss.as.console.client.standalone.runtime.StandaloneRuntimeView;
import org.jboss.as.console.client.standalone.runtime.VMMetricsPresenter;
import org.jboss.as.console.client.standalone.runtime.VMMetricsView;
import org.jboss.as.console.client.tools.BrowserPresenter;
import org.jboss.as.console.client.tools.BrowserView;
import org.jboss.as.console.client.tools.ToolsPresenter;
import org.jboss.as.console.client.tools.ToolsView;
import org.jboss.as.console.client.tools.modelling.workbench.repository.RepositoryPresenter;
import org.jboss.as.console.client.tools.modelling.workbench.repository.RepositoryView;
import org.jboss.as.console.client.tools.modelling.workbench.repository.SampleRepository;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.deployment.DomainDeploymentFinder;
import org.jboss.as.console.client.v3.deployment.DomainDeploymentFinderView;
import org.jboss.as.console.client.v3.deployment.StandaloneDeploymentFinder;
import org.jboss.as.console.client.v3.deployment.StandaloneDeploymentFinderView;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.spi.GinExtensionBinding;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.HandlerMapping;
import org.jboss.dmr.client.dispatch.impl.DMRHandler;
import org.jboss.dmr.client.dispatch.impl.DispatchAsyncImpl;
import org.jboss.dmr.client.dispatch.impl.HandlerRegistry;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.dag.DAGDispatcher;
import org.useware.kernel.gui.behaviour.StatementContext;

/**
 * Provides the bindings for the core UI widgets.
 *
 * @author Heiko Braun
 * @date 1/31/11
 */
@GinExtensionBinding
public class CoreUIModule extends AbstractPresenterModule {

    @Override
    protected void configure() {
        // ------------------------------------------------------ GWTP presenters

        // sign in
        bindPresenter(SignInPagePresenter.class, SignInPagePresenter.MyView.class,
                SignInPageView.class, SignInPagePresenter.MyProxy.class);

        // main layout
        bindPresenter(MainLayoutPresenter.class,
                MainLayoutPresenter.MainLayoutView.class,
                MainLayoutViewImpl.class,
                MainLayoutPresenter.MainLayoutProxy.class);

        // homepage
        bindPresenter(HomepagePresenter.class,
                HomepagePresenter.MyView.class,
                HomepageView.class,
                HomepagePresenter.MyProxy.class);

        // tools
        bindPresenter(ToolsPresenter.class,
                ToolsPresenter.MyView.class,
                ToolsView.class,
                ToolsPresenter.MyProxy.class);

        bindPresenterWidget(BrowserPresenter.class,
                BrowserPresenter.MyView.class,
                BrowserView.class);

        /*bindPresenterWidget(DebugPresenter.class,
                DebugPresenter.MyView.class,
                DebugPresenterView.class
        ); */

        bindPresenter(SettingsPresenter.class,
                SettingsPresenter.MyView.class,
                SettingsPresenterViewImpl.class,
                SettingsPresenter.MyProxy.class);

        bindPresenterWidget(SettingsPresenterWidget.class,
                SettingsPresenterWidget.MyView.class,
                SettingsView.class);

        // ----------------------------------------------------------------------
        // server management application

        bindPresenter(ServerMgmtApplicationPresenter.class,
                ServerMgmtApplicationPresenter.ServerManagementView.class,
                ColumnServerView.class,
                ServerMgmtApplicationPresenter.ServerManagementProxy.class);

        bindPresenter(StandaloneDeploymentPresenter.class,
                StandaloneDeploymentPresenter.MyView.class,
                StandaloneDeploymentView.class,
                StandaloneDeploymentPresenter.MyProxy.class);

        // ------------------------------------------------
        // domain management application

        bindPresenter(InterfacePresenter.class,
                InterfacePresenter.MyView.class,
                InterfaceView.class,
                InterfacePresenter.MyProxy.class);

        bindPresenter(PropertiesPresenter.class,
                PropertiesPresenter.MyView.class,
                PropertiesView.class,
                PropertiesPresenter.MyProxy.class);

        bindPresenter(HostPropertiesPresenter.class,
                HostPropertiesPresenter.MyView.class,
                HostPropertiesView.class,
                HostPropertiesPresenter.MyProxy.class);

        bindPresenter(HostInterfacesPresenter.class,
                HostInterfacesPresenter.MyView.class,
                HostInterfacesView.class,
                HostInterfacesPresenter.MyProxy.class);

        bindPresenter(HostJVMPresenter.class,
                HostJVMPresenter.MyView.class,
                HostJVMView.class,
                HostJVMPresenter.MyProxy.class);

        // profile management application
        bindPresenter(ProfileMgmtPresenter.class,
                ProfileMgmtPresenter.MyView.class,
                ColumnProfileView.class,
                ProfileMgmtPresenter.MyProxy.class);

        bindPresenter(TopologyPresenter.class,
                TopologyPresenter.MyView.class,
                TopologyView.class,
                TopologyPresenter.MyProxy.class);

        // domain/server-group
        bindPresenter(ServerGroupPresenter.class,
                ServerGroupPresenter.MyView.class,
                ServerGroupView.class,
                ServerGroupPresenter.MyProxy.class);

        // domain/domain-deployments
        bindPresenter(DomainDeploymentPresenter.class,
                DomainDeploymentPresenter.MyView.class,
                DomainDeploymentView.class,
                DomainDeploymentPresenter.MyProxy.class);

        // deployment finders
        bindPresenter(DomainDeploymentFinder.class,
                DomainDeploymentFinder.MyView.class,
                DomainDeploymentFinderView.class,
                DomainDeploymentFinder.MyProxy.class);
        bindPresenter(StandaloneDeploymentFinder.class,
                StandaloneDeploymentFinder.MyView.class,
                StandaloneDeploymentFinderView.class,
                StandaloneDeploymentFinder.MyProxy.class);

        bindPresenter(HostMgmtPresenter.class,
                HostMgmtPresenter.MyView.class,
                ColumnHostView.class,
                HostMgmtPresenter.MyProxy.class);

        bindPresenter(ServerConfigPresenter.class,
                ServerConfigPresenter.MyView.class,
                ServerConfigView.class,
                ServerConfigPresenter.MyProxy.class);

        bindPresenter(DataSourcePresenter.class,
                DataSourcePresenter.MyView.class,
                DatasourceView.class,
                DataSourcePresenter.MyProxy.class);

        bindPresenter(MsgDestinationsPresenter.class,
                MsgDestinationsPresenter.MyView.class,
                MsgDestinationsView.class,
                MsgDestinationsPresenter.MyProxy.class);

        bindPresenter(MsgConnectionsPresenter.class,
                MsgConnectionsPresenter.MyView.class,
                MsgConnectionsView.class,
                MsgConnectionsPresenter.MyProxy.class);

        bindPresenter(MsgClusteringPresenter.class,
                MsgClusteringPresenter.MyView.class,
                MsgClusteringView.class,
                MsgClusteringPresenter.MyProxy.class);

        bindPresenter(LogViewerPresenter.class,
                LogViewerPresenter.MyView.class,
                LogViewerView.class,
                LogViewerPresenter.MyProxy.class);

        bindPresenter(LogFilesPresenter.class,
                LogFilesPresenter.MyView.class,
                LogFilesView.class,
                LogFilesPresenter.MyProxy.class);

        bindPresenter(ConfigAdminPresenter.class,
                ConfigAdminPresenter.MyView.class,
                ConfigAdminView.class,
                ConfigAdminPresenter.MyProxy.class);

        bindPresenter(SocketBindingPresenter.class,
                SocketBindingPresenter.MyView.class,
                SocketBindingView.class,
                SocketBindingPresenter.MyProxy.class);

        bindPresenter(WebPresenter.class,
                WebPresenter.MyView.class,
                WebSubsystemView.class,
                WebPresenter.MyProxy.class);

        bindPresenter(StandaloneServerPresenter.class,
                StandaloneServerPresenter.MyView.class,
                StandaloneServerView.class,
                StandaloneServerPresenter.MyProxy.class);

        bindPresenter(WebServicePresenter.class,
                WebServicePresenter.MyView.class,
                WebServiceView.class,
                WebServicePresenter.MyProxy.class);

        bindPresenter(WebServiceRuntimePresenter.class,
                WebServiceRuntimePresenter.MyView.class,
                WebServiceRuntimeView.class,
                WebServiceRuntimePresenter.MyProxy.class);

        bindPresenter(BatchPresenter.class,
                BatchPresenter.MyView.class,
                BatchView.class,
                BatchPresenter.MyProxy.class);

        bindPresenter(IOPresenter.class,
                IOPresenter.MyView.class,
                IOView.class,
                IOPresenter.MyProxy.class);

        bindPresenter(ResourceAdapterPresenter.class,
                ResourceAdapterPresenter.MyView.class,
                ResourceAdapterView.class,
                ResourceAdapterPresenter.MyProxy.class);

        bindPresenter(JndiPresenter.class,
                JndiPresenter.MyView.class,
                JndiView.class,
                JndiPresenter.MyProxy.class);

        bindPresenter(VMMetricsPresenter.class,
                VMMetricsPresenter.MyView.class,
                VMMetricsView.class,
                VMMetricsPresenter.MyProxy.class);

        bindPresenter(HostVMMetricPresenter.class,
                HostVMMetricPresenter.MyView.class,
                HostVMMetricView.class,
                HostVMMetricPresenter.MyProxy.class);


        bindPresenter(StandaloneRuntimePresenter.class,
                StandaloneRuntimePresenter.MyView.class,
                StandaloneRuntimeView.class,
                StandaloneRuntimePresenter.MyProxy.class);

        bindPresenter(DomainRuntimePresenter.class,
                DomainRuntimePresenter.MyView.class,
                DomainRuntimeView.class,
                DomainRuntimePresenter.MyProxy.class);

        bindPresenter(TXMetricPresenter.class,
                TXMetricPresenter.MyView.class,
                TXMetricViewImpl.class,
                TXMetricPresenter.MyProxy.class);

        bindPresenter(TXLogPresenter.class,
                TXLogPresenter.MyView.class,
                TXLogView.class,
                TXLogPresenter.MyProxy.class);

        bindPresenter(JpaPresenter.class,
                JpaPresenter.MyView.class,
                JpaView.class,
                JpaPresenter.MyProxy.class);

        bindPresenter(MailPresenter.class,
                MailPresenter.MyView.class,
                MailSubsystemView.class,
                MailPresenter.MyProxy.class);

        bindPresenter(MailFinder.class,
                MailFinder.MyView.class,
                MailFinderView.class,
                MailFinder.MyProxy.class);

        bindPresenter(ModclusterPresenter.class,
                ModclusterPresenter.MyView.class,
                ModclusterView.class,
                ModclusterPresenter.MyProxy.class);

        bindPresenter(JMXPresenter.class,
                JMXPresenter.MyView.class,
                JMXSubsystemView.class,
                JMXPresenter.MyProxy.class);

        bindPresenter(EEPresenter.class,
                EEPresenter.MyView.class,
                EESubsystemView.class,
                EEPresenter.MyProxy.class);

        bindPresenter(JcaPresenter.class,
                JcaPresenter.MyView.class,
                JcaSubsystemView.class,
                JcaPresenter.MyProxy.class);

        bindPresenter(WebMetricPresenter.class,
                WebMetricPresenter.MyView.class,
                WebMetricView.class,
                WebMetricPresenter.MyProxy.class);

        bindPresenter(JMSMetricPresenter.class,
                JMSMetricPresenter.MyView.class,
                JMSMetricView.class,
                JMSMetricPresenter.MyProxy.class);

        bindPresenter(DataSourceMetricPresenter.class,
                DataSourceMetricPresenter.MyView.class,
                DataSourceMetricView.class,
                DataSourceMetricPresenter.MyProxy.class);

        bindPresenter(JPAMetricPresenter.class,
                JPAMetricPresenter.MyView.class,
                JPAMetricsView.class,
                JPAMetricPresenter.MyProxy.class);

        bindPresenter(JGroupsPresenter.class,
                JGroupsPresenter.MyView.class,
                JGroupsSubsystemView.class,
                JGroupsPresenter.MyProxy.class);

        bindPresenter(PathManagementPresenter.class,
                PathManagementPresenter.MyView.class,
                PathManagementView.class,
                PathManagementPresenter.MyProxy.class);

        bindPresenter(EnvironmentPresenter.class,
                EnvironmentPresenter.MyView.class,
                EnvironmentView.class,
                EnvironmentPresenter.MyProxy.class);

        bindPresenter(PatchManagementPresenter.class,
                PatchManagementPresenter.MyView.class,
                PatchManagementView.class,
                PatchManagementPresenter.MyProxy.class);

        bindPresenter(IiopOpenJdkPresenter.class,
                IiopOpenJdkPresenter.MyView.class,
                IiopOpenJdkView.class,
                IiopOpenJdkPresenter.MyProxy.class);

        // Administration
        bindPresenter(AdministrationPresenter.class,
                AdministrationPresenter.MyView.class,
                AdministrationView.class,
                AdministrationPresenter.MyProxy.class);
        bindPresenter(RoleAssignmentPresenter.class,
                RoleAssignmentPresenter.MyView.class,
                RoleAssignementView.class,
                RoleAssignmentPresenter.MyProxy.class);
        bindPresenter(AuditLogPresenter.class,
                AuditLogPresenter.MyView.class,
                AuditLogView.class,
                AuditLogPresenter.MyProxy.class);

        // mbui workbench
        bindPresenter(RepositoryPresenter.class,
                RepositoryPresenter.MyView.class,
                RepositoryView.class,
                RepositoryPresenter.MyProxy.class);

        bindPresenterWidget(UnauthorisedPresenter.class,
                UnauthorisedPresenter.MyView.class,
                UnauthorisedView.class);

        bindPresenter(DialogPresenter.class,
                DialogView.class,
                DialogViewImpl.class,
                DialogPresenter.MyProxy.class);

        bindPresenter(NoServerPresenter.class,
                NoServerPresenter.MyView.class,
                NoServerView.class,
                NoServerPresenter.MyProxy.class);

        bindPresenter(CSPPresenter.class,
                CSPPresenter.MyView.class,
                CSPView.class,
                CSPPresenter.MyProxy.class);

        bindPresenter(HttpPresenter.class,
                HttpPresenter.MyView.class,
                HttpView.class,
                HttpPresenter.MyProxy.class);

        bindPresenter(ServletPresenter.class,
                ServletPresenter.MyView.class,
                ServletView.class,
                ServletPresenter.MyProxy.class);

        bindPresenter(UndertowPresenter.class,
                UndertowPresenter.MyView.class,
                UndertowView.class,
                UndertowPresenter.MyProxy.class);

        bindPresenter(HttpMetricPresenter.class,
                HttpMetricPresenter.MyView.class,
                HttpMetricView.class,
                HttpMetricPresenter.MyProxy.class);

        bindPresenter(RemotingPresenter.class,
                RemotingPresenter.MyView.class,
                RemotingView.class,
                RemotingPresenter.MyProxy.class);


        bindPresenter(CacheFinderPresenter.class,
                CacheFinderPresenter.MyView.class,
                CacheFinder.class,
                CacheFinderPresenter.MyProxy.class);

        bindPresenter(CachesPresenter.class,
                CachesPresenter.MyView.class,
                CachesView.class,
                CachesPresenter.MyProxy.class);



        bindPresenter(HornetqFinder.class,
                HornetqFinder.MyView.class,
                HornetqFinderView.class,
                HornetqFinder.MyProxy.class);

        bindPresenter(EJB3Presenter.class,
                EJB3Presenter.MyView.class,
                EJBView.class,
                EJB3Presenter.MyProxy.class);

        bindPresenter(LoggerPresenter.class,
                      LoggerPresenter.MyView.class,
                      LoggerView.class,
                      LoggerPresenter.MyProxy.class);

        bindPresenter(SecDomainFinder.class,
                SecDomainFinder.MyView.class,
                SecDomainFinderView.class,
                SecDomainFinder.MyProxy.class);

        bindPresenter(SecDomainPresenter.class,
                SecDomainPresenter.MyView.class,
                SecDomainView.class,
                SecDomainPresenter.MyProxy.class);


        // ------------------------------------------------------ circuit

        bind(Dispatcher.class).to(DAGDispatcher.class).in(Singleton.class);


        // ------------------------------------------------------ no circuit stores yet!

        bind(DeploymentStore.class).in(Singleton.class);
        bind(DataSourceStore.class).to(DataSourceStoreImpl.class).in(Singleton.class);
        bind(ProfileDAO.class).to(ProfileDAOImpl.class).in(Singleton.class);
        bind(SubsystemLoader.class).to(SubsystemStoreImpl.class).in(Singleton.class);
        bind(ServerGroupDAO.class).to(ServerGroupDAOImpl.class).in(Singleton.class);
        bind(HostInformationStore.class).to(HostInfoStoreImpl.class).in(Singleton.class);
        bind(PatchManager.class).in(Singleton.class);

        bind(DomainDriverStrategy.class).in(Singleton.class);
        bind(StandaloneDriverStrategy.class).in(Singleton.class);

        // ------------------------------------------------------ registries

        bind(SampleRepository.class).in(Singleton.class);
        bind(NameTokenRegistry.class).in(Singleton.class);
        bind(EndpointRegistry.class).in(Singleton.class);
        bind(DomainEndpointStrategy.class).in(Singleton.class);
        bind(RequiredResourcesRegistry.class).to(RequiredResourcesRegistryImpl.class).in(Singleton.class);
        bind(ResourceDescriptionRegistry.class).in(Singleton.class);
        bind(SearchIndexRegistry.class).to(SearchIndexRegistryImpl.class).in(Singleton.class);
        bind(SubsystemRegistry.class).to(SubsystemRegistryImpl.class).in(Singleton.class);
        bind(RuntimeExtensionRegistry.class).to(RuntimeLHSItemExtensionRegistryImpl.class).in(Singleton.class);

        // ------------------------------------------------------ application specific

        // static injections
        requestStaticInjection(RuntimeBaseAddress.class);
        requestStaticInjection(Baseadress.class);

        // bootstrapping
        bind(BootstrapContext.class).in(Singleton.class);
        bind(BootstrapServerSetup.class).in(Singleton.class);
        bind(LoadGoogleViz.class).in(Singleton.class);
        bind(ExecutionMode.class).in(Singleton.class);
        bind(TrackExecutionMode.class).in(Singleton.class);
        bind(LoadCompatMatrix.class).in(Singleton.class);
        bind(RegisterSubsystems.class).in(Singleton.class);
        bind(EagerLoadProfiles.class).in(Singleton.class);
        bind(HostStoreInit.class).in(Singleton.class);
        bind(ServerStoreInit.class).in(Singleton.class);
        bind(EagerLoadGroups.class).in(Singleton.class);
        bind(BootstrapSteps.class).in(Singleton.class);
        bind(Bootstrapper.class).in(Singleton.class);

        bind(StatementContext.class).to(CoreGUIContext.class).in(Singleton.class);
        bind(SecurityFramework.class).to(SecurityFrameworkImpl.class).in(Singleton.class);
        bind(PlaceRequestSecurityFramework.class).in(Singleton.class);

        bind(StandaloneEndpointStrategy.class).in(Singleton.class);
        bind(RequiredResourcesProcessor.class).in(Singleton.class);

        /* use this to test against 6.x until the RBAC facilities are available */
        //bind(SecurityFramework.class).to(MockSecurityFramework.class).in(Singleton.class);

        bind(Harvest.class).in(Singleton.class);
        bind(Index.class).toProvider(IndexProvider.class).in(Singleton.class);

        bind(DMRHandler.class).in(Singleton.class);
        bind(DispatchAsync.class).to(DispatchAsyncImpl.class).in(Singleton.class);
        bind(HandlerMapping.class).to(HandlerRegistry.class).in(Singleton.class);

        bind(ReloadState.class).in(Singleton.class);
        bind(CurrentProfileSelection.class).in(Singleton.class);

        bind(DataSourceTemplates.class).in(Singleton.class);

        bind(Baseadress.class).in(Singleton.class);
        bind(RuntimeBaseAddress.class).in(Singleton.class);
        bind(ExpressionResolver.class).to(DefaultExpressionResolver.class).in(Singleton.class);

        // main layout
        bind(ToplevelTabs.class).in(Singleton.class);
        bind(Header.class).in(Singleton.class);
        bind(Footer.class).in(Singleton.class);

        // supporting components
        bind(MessageBar.class).in(Singleton.class);
        bind(MessageCenter.class).to(MessageCenterImpl.class).in(Singleton.class);
        bind(MessageCenterView.class).in(Singleton.class);
        bind(HelpSystem.class).in(Singleton.class);

        // mobile:
        // bindConstant().annotatedWith(GaAccount.class).to("UA-36590267-1");
        bindConstant().annotatedWith(GaAccount.class).to("UA-35829315-1");
        bind(GoogleAnalytics.class).toProvider(AnalyticsProvider.class).in(Singleton.class);
        bind(NavigationTracker.class).asEagerSingleton();

        bind(ModelVersions.class).in(Singleton.class);
        bind(FeatureSet.class).in(Singleton.class);

        bind(EventBus.class).to(SimpleEventBus.class).in(Singleton.class);
        bind(PlaceManager.class).to(DefaultPlaceManager.class).in(Singleton.class);

        // see http://code.google.com/p/gwt-platform/issues/detail?id=381
        //bind(TokenFormatter.class).to(ParameterTokenFormatter.class).in(Singleton.class);
        bind(TokenFormatter.class).to(NewTokenFormatter.class).in(Singleton.class);

        bind(RootPresenter.class).asEagerSingleton();
        //bind(ProxyFailureHandler.class).to(DefaultProxyFailureHandler.class).in(Singleton.class);

        bind(Gatekeeper.class).to(RBACGatekeeper.class).in(Singleton.class);
        bind(HostManagementGatekeeper.class).in(Singleton.class);
        bind(DomainRuntimegateKeeper.class).in(Singleton.class);

        bind(CurrentUser.class).in(Singleton.class);
        bind(ApplicationProperties.class).to(BootstrapContext.class).in(Singleton.class);
        bind(ApplicationMetaData.class).in(Singleton.class);

        bind(PreviewContentFactory.class).to(PreviewContentFactoryImpl.class).in(Singleton.class);

        // Load and inject CSS resources
        bind(ResourceLoader.class).asEagerSingleton();
    }

    @Provides Scheduler provideScheduler() {
        return Scheduler.get();
    }
}

