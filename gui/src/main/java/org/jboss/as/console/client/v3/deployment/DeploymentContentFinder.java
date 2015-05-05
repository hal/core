package org.jboss.as.console.client.v3.deployment;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.ServerInstance;
import org.jboss.as.console.client.domain.topology.HostInfo;
import org.jboss.as.console.client.domain.topology.TopologyFunctions;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.deployment.model.DeploymentRecord;
import org.jboss.as.console.client.shared.deployment.model.DeploymentSubsystem;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.v3.presenter.Finder;
import org.jboss.as.console.client.widgets.nav.v3.FinderScrollEvent;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Outcome;

import java.util.Iterator;
import java.util.List;

/**
 * @author Heiko Braun
 */
public class DeploymentContentFinder extends Presenter<DeploymentContentFinder.MyView, DeploymentContentFinder.MyProxy>
        implements Finder, PreviewEvent.Handler, FinderScrollEvent.Handler  {

    // @formatter:off --------------------------------------- proxy & view

    @ProxyCodeSplit
    @NameToken(NameTokens.DeploymentContentFinder)
    // TODO Introduce custom pattern for selected host / server for given server-group selection
//    @RequiredResources(resources = {"???/{selected.deployment}/???"})
    @SearchIndex(keywords = {"deployment", "war", "ear", "application"})
    public interface MyProxy extends Proxy<DeploymentContentFinder>, Place {}

    public interface MyView extends View, HasPresenter<DeploymentContentFinder> {
        void setPreview(SafeHtml html);
        void toggleSubdeployments( boolean hasSubdeployments);
        void updateSubdeployments( List<DeploymentRecord> subdeployments);
        void updateSubsystems( List<DeploymentSubsystem> subsystems);
        void toggleScrolling( boolean enforceScrolling, int requiredWidth);
    }

    // @formatter:on ---------------------------------------- instance data


    private final DispatchAsync dispatcher;
    private final BeanFactory beanFactory;
    private final BootstrapContext bootstrapContext;
    private final DeploymentStore deploymentStore;
    private final org.jboss.as.console.client.shared.deployment.DeploymentStore deployments;


    // ------------------------------------------------------ presenter lifecycle

    @Inject
    public DeploymentContentFinder(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final DispatchAsync dispatcher, final BeanFactory beanFactory, final BootstrapContext bootstrapContext,
            final DeploymentStore deploymentStore,
            final org.jboss.as.console.client.shared.deployment.DeploymentStore deployments) {
        super(eventBus, view, proxy);
        this.dispatcher = dispatcher;
        this.beanFactory = beanFactory;
        this.bootstrapContext = bootstrapContext;
        this.deploymentStore = deploymentStore;
        this.deployments = deployments;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        registerHandler(getEventBus().addHandler(PreviewEvent.TYPE, this));
        registerHandler(getEventBus().addHandler(FinderScrollEvent.TYPE, this));
    }

    @Override
    protected void revealInParent() {
        if (bootstrapContext.isStandalone()) {
            // TODO Implement standalone mode
        } else {
            RevealContentEvent.fire(this, DeploymentFinder.TYPE_MainContent, this);
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        if (deploymentStore.getSelectedDeployment() != null) {
            findReferenceServer(deploymentStore.getSelectedDeployment());
        }
    }


    // ------------------------------------------------------ deployment related

    private void findReferenceServer(final DeploymentRecord selectedDeployment) {
        if (selectedDeployment.isEnabled()) {
            Outcome<FunctionContext> outcome = new Outcome<FunctionContext>() {
                @Override
                public void onFailure(final FunctionContext context) {
                    Console.error("Unable to load deployment content", context.getErrorMessage()); // TODO i18n
                }

                @Override
                public void onSuccess(final FunctionContext context) {
                    ServerInstance referenceServer = null;
                    List<HostInfo> hosts = context.pop();
                    for (Iterator<HostInfo> i = hosts.iterator(); i.hasNext() && referenceServer == null; ) {
                        HostInfo host = i.next();
                        List<ServerInstance> serverInstances = host.getServerInstances();
                        for (Iterator<ServerInstance> j = serverInstances.iterator();
                                j.hasNext() && referenceServer == null; ) {
                            ServerInstance server = j.next();
                            if (server.isRunning() && server.getGroup().equals(selectedDeployment.getServerGroup())) {
                                referenceServer = server;
                            }
                        }
                    }
                    if (referenceServer != null) {
                        loadDeployments(selectedDeployment, referenceServer);
                        System.out.println("Found reference server " + referenceServer.getName() + " on " + referenceServer.getGroup() + " / " + referenceServer.getHost());
                    } else {
                        System.out.println("No reference server found!");
                        // TODO No reference server found
                    }
                }
            };
            new Async<FunctionContext>().waterfall(new FunctionContext(), outcome,
                    new TopologyFunctions.HostsAndGroups(dispatcher),
                    new TopologyFunctions.ServerConfigs(dispatcher, beanFactory),
                    new TopologyFunctions.RunningServerInstances(dispatcher));
        }
    }

    private void loadDeployments(final DeploymentRecord selectedDeployment, ServerInstance referenceServer) {
        // TODO Should be replaced with a :read-resource(recursive=true)
        deployments.loadDeployments(referenceServer, new AsyncCallback<List<DeploymentRecord>>() {
            @Override
            public void onFailure(final Throwable caught) {
                Console.error("Unable to load deployment content", caught.getMessage()); // TODO i18n
            }

            @Override
            public void onSuccess(final List<DeploymentRecord> result) {
                DeploymentRecord referenceDeployment = null;
                for (DeploymentRecord deploymentRecord : result) {
                    if (deploymentRecord.getName().equals(selectedDeployment.getName())) {
                        referenceDeployment = deploymentRecord;
                        break;
                    }
                }
                if (referenceDeployment != null) {
                    getView().toggleSubdeployments(referenceDeployment.isHasSubdeployments());
                    if (referenceDeployment.isHasSubdeployments()) {
                        deployments.loadSubdeployments(referenceDeployment,
                                new AsyncCallback<List<DeploymentRecord>>() {
                                    @Override
                                    public void onFailure(final Throwable caught) {
                                        Console.error("Unable to load deployment content",
                                                caught.getMessage()); // TODO i18n
                                    }

                                    @Override
                                    public void onSuccess(final List<DeploymentRecord> result) {
                                        getView().updateSubdeployments(result);
                                    }
                                });
                    } else {
                        deployments.loadSubsystems(referenceDeployment,
                                new AsyncCallback<List<DeploymentSubsystem>>() {
                                    @Override
                                    public void onFailure(final Throwable caught) {
                                        Console.error("Unable to load deployment content",
                                                caught.getMessage()); // TODO i18n
                                    }

                                    @Override
                                    public void onSuccess(final List<DeploymentSubsystem> result) {
                                        getView().updateSubsystems(result);
                                    }
                                });
                    }
                } else {
                    // TODO No reference deployment found
                }
            }
        });
    }

    public void loadSubsystems(final DeploymentRecord subdeployment) {
        deployments.loadSubsystems(subdeployment, new AsyncCallback<List<DeploymentSubsystem>>() {
            @Override
            public void onFailure(final Throwable caught) {
                Console.error("Unable to load deployment content", caught.getMessage()); // TODO i18n
            }

            @Override
            public void onSuccess(final List<DeploymentSubsystem> result) {
                getView().updateSubsystems(result);
            }
        });
    }


    // ------------------------------------------------------ finder related

    @Override
    public void onPreview(PreviewEvent event) {
        if (isVisible()) {
            getView().setPreview(event.getHtml());
        }
    }

    @Override
    public void onToggleScrolling(final FinderScrollEvent event) {
        getView().toggleScrolling(event.isEnforceScrolling(), event.getRequiredWidth());
    }
}
