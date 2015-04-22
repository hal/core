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
package org.jboss.as.console.client.v3.deployment;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.model.ServerInstance;
import org.jboss.as.console.client.domain.topology.HostInfo;
import org.jboss.as.console.client.domain.topology.TopologyFunctions;
import org.jboss.as.console.client.rbac.UnauthorisedPresenter;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.shared.state.PerspectivePresenter;
import org.jboss.as.console.client.v3.deployment.wizard.AddDeploymentWizard;
import org.jboss.as.console.client.v3.dmr.Composite;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.client.v3.presenter.Finder;
import org.jboss.as.console.client.v3.stores.domain.ServerGroupStore;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshServerGroups;
import org.jboss.as.console.client.widgets.nav.v3.ClearFinderSelectionEvent;
import org.jboss.as.console.client.widgets.nav.v3.FinderScrollEvent;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;
import org.jboss.as.console.spi.OperationMode;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Outcome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jboss.as.console.spi.OperationMode.Mode.DOMAIN;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
public class DeploymentFinder
        extends PerspectivePresenter<DeploymentFinder.MyView, DeploymentFinder.MyProxy>
        implements Finder, PreviewEvent.Handler, FinderScrollEvent.Handler, ClearFinderSelectionEvent.Handler {


    // @formatter:off --------------------------------------- proxy & view

    @ProxyCodeSplit
    @OperationMode(DOMAIN)
    @NameToken(NameTokens.DeploymentFinder)
    @SearchIndex(keywords = {"deployment", "war", "ear", "application"})
    @RequiredResources(resources = {
            "/deployment=*",
            //"/{selected.host}/server=*", TODO: https://issues.jboss.org/browse/WFLY-1997
            "/server-group={selected.group}/deployment=*"
    }, recursive = false)
    public interface MyProxy extends ProxyPlace<DeploymentFinder> {
    }

    public interface MyView extends View, HasPresenter<DeploymentFinder> {
        void updateServerGroups(Iterable<ServerGroupRecord> serverGroups);
        void updateAssignments(Iterable<Assignment> assignments);
        void updateDeployments(Iterable<Deployment> deployments);

        void setPreview(SafeHtml html);
        void clearActiveSelection(ClearFinderSelectionEvent event);
        void toggleScrolling(boolean enforceScrolling, int requiredWidth);
    }

    // @formatter:on ---------------------------------------- instance data


    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent = new GwtEvent.Type<>();

    private final BeanFactory beanFactory;
    private final DispatchAsync dispatcher;
    private final Dispatcher circuit;
    private final ServerGroupStore serverGroupStore;
    private final Map<String, ReferenceServer> referenceServers;
    private final AddDeploymentWizard wizard;


    // ------------------------------------------------------ presenter lifecycle

    @Inject
    public DeploymentFinder(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final PlaceManager placeManager, final UnauthorisedPresenter unauthorisedPresenter,
            final BeanFactory beanFactory, final DispatchAsync dispatcher, final Dispatcher circuit,
            final ServerGroupStore serverGroupStore, final BootstrapContext bootstrapContext, final Header header) {
        super(eventBus, view, proxy, placeManager, header, NameTokens.DeploymentFinder,
                unauthorisedPresenter, TYPE_MainContent);
        this.beanFactory = beanFactory;
        this.dispatcher = dispatcher;
        this.circuit = circuit;
        this.serverGroupStore = serverGroupStore;
        this.referenceServers = new HashMap<>();
        this.wizard = new AddDeploymentWizard(bootstrapContext, beanFactory, dispatcher, this);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);

        // GWT event handler
        registerHandler(getEventBus().addHandler(PreviewEvent.TYPE, this));
        registerHandler(getEventBus().addHandler(FinderScrollEvent.TYPE, this));
        registerHandler(getEventBus().addHandler(ClearFinderSelectionEvent.TYPE, this));

        // circuit handler
        registerHandler(serverGroupStore.addChangeHandler(RefreshServerGroups.class, action -> {
            getView().updateServerGroups(serverGroupStore.getServerGroups());
        }));
    }

    @Override
    protected void onFirstReveal(final PlaceRequest placeRequest, final PlaceManager placeManager,
            final boolean revealDefault) {
        circuit.dispatch(new RefreshServerGroups());
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }


    // ------------------------------------------------------ deployment methods

    public void launchAddAssignmentWizard(final String serverGroup) {
        loadUnassignedContent(serverGroup, new AsyncCallback<List<Content>>() {
            @Override
            public void onFailure(final Throwable caught) {
                // TODO Error handling
            }

            @Override
            public void onSuccess(final List<Content> result) {
                wizard.open(result, serverGroup);
            }
        });
    }

    private void loadUnassignedContent(final String serverGroup, final AsyncCallback<List<Content>> callback) {
        Operation content = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, ResourceAddress.ROOT)
                .param(CHILD_TYPE, "deployment")
                .build();
        ResourceAddress address = new ResourceAddress().add("server-group", serverGroup);
        Operation assignments = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, address)
                .param(CHILD_TYPE, "deployment")
                .build();

        dispatcher.execute(new DMRAction(new Composite(content, assignments)), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (result.isFailure()) {
                    callback.onFailure(new RuntimeException(result.getFailureDescription()));
                } else {
                    Set<String> assignemnts = new HashSet<>();
                    List<Property> properties = result.get(RESULT).get("step-2").get(RESULT).asPropertyList();
                    for (Property property : properties) {
                        assignemnts.add(property.getName());
                    }

                    List<Content> contentRepository = new ArrayList<>();
                    properties = result.get(RESULT).get("step-1").get(RESULT).asPropertyList();
                    for (Property property : properties) {
                        if (assignemnts.contains(property.getName())) {
                            continue; // skip already assigned content
                        }
                        contentRepository.add(new Content(property.getValue()));
                    }
                    callback.onSuccess(contentRepository);
                }
            }
        });
    }

    public void launchUpdateAssignmentWizard() {
        Console.warning("Update assignment not yet implemented");
    }

    public void verifyEnableDisableAssignment(final Assignment assignment) {
        String message;
        String operation;
        if (assignment.isEnabled()) {
            message = "Disable " + assignment.getName();
            operation = "undeploy";
        } else {
            message = "Enable " + assignment.getName();
            operation = "deploy";
        }
        Feedback.confirm(Console.CONSTANTS.common_label_areYouSure(), message,
                isConfirmed -> {
                    if (isConfirmed) {
                        modifyAssignment(assignment, operation);
                    }
                });
    }

    public void verifyRemoveAssignment(final Assignment assignment) {
        Feedback.confirm(Console.CONSTANTS.common_label_areYouSure(), "Remove " + assignment.getName(),
                isConfirmed -> {
                    if (isConfirmed) {
                        modifyAssignment(assignment, REMOVE);
                    }
                });
    }

    private void modifyAssignment(final Assignment assignment, final String operation) {
        final String serverGroup = assignment.getServerGroup();
        ResourceAddress address = new ResourceAddress()
                .add("server-group", serverGroup)
                .add("deployment", assignment.getName());
        final Operation op = new Operation.Builder(operation, address).build();
        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                // TODO Error handling
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (result.isFailure()) {
                    // TODO Error handling
                } else {
                    loadAssignments(serverGroup, false);
                }
            }
        });
    }

    public void loadAssignments(final String serverGroup, final boolean lookupReferenceServer) {
        ResourceAddress address = new ResourceAddress().add("server-group", serverGroup);
        Operation op = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, address)
                .param(CHILD_TYPE, "deployment")
                .build();

        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                // TODO Error handling
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (result.isFailure()) {
                    // TODO Error handling
                } else {
                    List<Assignment> assignments = new ArrayList<>();
                    ModelNode payload = result.get(RESULT);
                    List<Property> properties = payload.asPropertyList();
                    for (Property property : properties) {
                        assignments.add(new Assignment(serverGroup, property.getValue()));
                    }
                    if (lookupReferenceServer) {
                        findReferenceServer(serverGroup,
                                () -> getView().updateAssignments(assignments));
                    } else {
                        getView().updateAssignments(assignments);
                    }
                }
            }
        });
    }

    private void findReferenceServer(final String serverGroup, final ScheduledCommand andThen) {
        Outcome<FunctionContext> outcome = new Outcome<FunctionContext>() {
            @Override
            public void onFailure(final FunctionContext context) {
                // TODO Error handling
            }

            @Override
            public void onSuccess(final FunctionContext context) {
                ReferenceServer referenceServer = null;
                List<HostInfo> hosts = context.pop();
                for (Iterator<HostInfo> i = hosts.iterator(); i.hasNext() && referenceServer == null; ) {
                    HostInfo host = i.next();
                    List<ServerInstance> serverInstances = host.getServerInstances();
                    for (Iterator<ServerInstance> j = serverInstances.iterator();
                            j.hasNext() && referenceServer == null; ) {
                        ServerInstance server = j.next();
                        if (server.isRunning() && server.getGroup().equals(serverGroup)) {
                            referenceServer = new ReferenceServer(server.getHost(), server.getName());
                        }
                    }
                }
                referenceServers.remove(serverGroup);
                if (referenceServer != null) {
                    referenceServers.put(serverGroup, referenceServer);
                }
                andThen.execute();
            }
        };
        new Async<FunctionContext>(Footer.PROGRESS_ELEMENT).waterfall(new FunctionContext(), outcome,
                new TopologyFunctions.HostsAndGroups(dispatcher),
                new TopologyFunctions.ServerConfigs(dispatcher, beanFactory),
                new TopologyFunctions.RunningServerInstances(dispatcher));
    }

    public void loadDeployments(final Assignment assignment) {
        final ReferenceServer referenceServer = referenceServers.get(assignment.getServerGroup());
        if (referenceServer != null) {
            Operation op = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, referenceServer.getAddress())
                    .param(CHILD_TYPE, "deployment")
                    .param(INCLUDE_RUNTIME, true)
                    .param(RECURSIVE, true)
                    .build();

            dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
                @Override
                public void onFailure(final Throwable caught) {
                    // TODO Error handling
                }

                @Override
                public void onSuccess(final DMRResponse response) {
                    ModelNode result = response.get();
                    if (result.isFailure()) {
                        // TODO Error handling
                    } else {
                        List<Deployment> deployments = new ArrayList<Deployment>();
                        ModelNode payload = result.get(RESULT);
                        List<Property> properties = payload.asPropertyList();
                        for (Property property : properties) {
                            // filter by assignment
                            if (property.getName().equals(assignment.getName())) {
                                deployments.add(new Deployment(referenceServer, property.getValue()));
                            }
                        }
                        getView().updateDeployments(deployments);
                    }
                }
            });
        } else {
            // TODO Error handling
        }
    }


    // ------------------------------------------------------ finder related methods

    @Override
    public void onPreview(PreviewEvent event) {
        getView().setPreview(event.getHtml());
    }


    @Override
    public void onToggleScrolling(final FinderScrollEvent event) {
        getView().toggleScrolling(event.isEnforceScrolling(), event.getRequiredWidth());
    }

    @Override
    public void onClearActiveSelection(final ClearFinderSelectionEvent event) {
        getView().clearActiveSelection(event);
    }


    // ------------------------------------------------------ state

    public ReferenceServer getReferenceServer(String serverGroup) {
        return referenceServers.get(serverGroup);
    }

    public boolean hasReferenceServer(Assignment assignment) {
        return getReferenceServer(assignment.getServerGroup()) != null;
    }
}
