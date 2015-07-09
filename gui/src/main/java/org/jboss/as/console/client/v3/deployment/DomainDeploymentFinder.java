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

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
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
import org.jboss.as.console.client.domain.topology.TopologyFunctions;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.shared.state.PerspectivePresenter;
import org.jboss.as.console.client.v3.deployment.wizard.AddContentWizard;
import org.jboss.as.console.client.v3.deployment.wizard.AddDomainDeploymentWizard;
import org.jboss.as.console.client.v3.deployment.wizard.AssignContentDialog;
import org.jboss.as.console.client.v3.deployment.wizard.ReplaceDomainDeploymentWizard;
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
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.jboss.as.console.spi.OperationMode.Mode.DOMAIN;
import static org.jboss.dmr.client.ModelDescriptionConstants.ADD;
import static org.jboss.dmr.client.ModelDescriptionConstants.REMOVE;

/**
 * @author Harald Pehl
 */
public class DomainDeploymentFinder
        extends PerspectivePresenter<DomainDeploymentFinder.MyView, DomainDeploymentFinder.MyProxy>
        implements Finder, PreviewEvent.Handler, FinderScrollEvent.Handler, ClearFinderSelectionEvent.Handler {

    // @formatter:off --------------------------------------- proxy & view

    @ProxyCodeSplit
    @OperationMode(DOMAIN)
    @NameToken(NameTokens.DomainDeploymentFinder)
    @SearchIndex(keywords = {"deployment", "war", "ear", "application"})
    @RequiredResources(resources = {
            "/deployment=*",
            //"/{selected.host}/server=*", TODO: https://issues.jboss.org/browse/WFLY-1997
            "/server-group={selected.group}/deployment=*"},
            recursive = false)
    public interface MyProxy extends ProxyPlace<DomainDeploymentFinder> {}

    public interface MyView extends View, HasPresenter<DomainDeploymentFinder> {
        void updateContentRepository( Iterable<Content> content);
        void updateUnassigned( Iterable<Content> unassigned);
        void updateServerGroups(Iterable<ServerGroupRecord> serverGroups);
        void updateAssignments(Iterable<Assignment> assignments);

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
    private final AddContentWizard addContentWizard;
    private final AssignContentDialog assignContentDialog;
    private final AddDomainDeploymentWizard addDeploymentWizard;
    private final ReplaceDomainDeploymentWizard replaceWizard;


    // ------------------------------------------------------ presenter lifecycle

    @Inject
    public DomainDeploymentFinder(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final PlaceManager placeManager, final BeanFactory beanFactory, final DispatchAsync dispatcher,
            final Dispatcher circuit, final ServerGroupStore serverGroupStore, final BootstrapContext bootstrapContext,
            final Header header) {
        super(eventBus, view, proxy, placeManager, header, NameTokens.DomainDeploymentFinder,
                TYPE_MainContent);
        this.beanFactory = beanFactory;
        this.dispatcher = dispatcher;
        this.circuit = circuit;
        this.serverGroupStore = serverGroupStore;

        this.assignContentDialog = new AssignContentDialog(this);
        this.addContentWizard = new AddContentWizard(bootstrapContext, beanFactory, dispatcher,
                context -> {
                    String name = context.deployNew ?
                            context.upload.getName() :
                            context.unmanagedDeployment.getName();
                    Console.info(name + " successfully uploaded.");
                    loadContentRepository();
                });
        this.addDeploymentWizard = new AddDomainDeploymentWizard(bootstrapContext, beanFactory, dispatcher,
                context -> {
                    String name = context.deployNew ?
                            context.upload.getName() :
                            context.deployExisting ?
                                    context.existingContent.getName() :
                                    context.unmanagedDeployment.getName();
                    Console.info(name + " successfully deployed.");
                    loadAssignments(context.serverGroup);
                });
        this.replaceWizard = new ReplaceDomainDeploymentWizard(bootstrapContext, beanFactory, dispatcher,
                context -> {
                    Console.info(context.upload.getName() + " successfully replaced.");
                    loadAssignments(context.serverGroup);
                });
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
    }

    @Override
    protected void onFirstReveal(final PlaceRequest placeRequest, final PlaceManager placeManager,
            final boolean revealDefault) {
        circuit.dispatch(new RefreshServerGroups());
    }

    @Override
    protected void onReset() {
        super.onReset();
        Console.MODULES.getHeader().highlight(getProxy().getNameToken());
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }


    // ------------------------------------------------------ finder related methods

    @Override
    public void onPreview(PreviewEvent event) {
        if (isVisible()) { getView().setPreview(event.getHtml()); }
    }

    @Override
    public void onToggleScrolling(final FinderScrollEvent event) {
        if (isVisible()) { getView().toggleScrolling(event.isEnforceScrolling(), event.getRequiredWidth()); }
    }

    @Override
    public void onClearActiveSelection(final ClearFinderSelectionEvent event) {
        if (isVisible()) { getView().clearActiveSelection(event); }
    }


    // ------------------------------------------------------ deployment methods

    public void loadContentRepository() {
        new Async<FunctionContext>().single(new FunctionContext(),
                new DeploymentFunctions.LoadContentAssignments(dispatcher),
                new Outcome<FunctionContext>() {
                    @Override
                    public void onFailure(final FunctionContext context) {
                        Console.error("Unable to find deployments.", context.getErrorMessage());
                    }

                    @Override
                    public void onSuccess(final FunctionContext context) {
                        List<Content> contentRepository = context.pop();
                        getView().updateContentRepository(contentRepository);
                    }
                });
    }

    public void loadUnassignedContent() {
        new Async<FunctionContext>().single(new FunctionContext(),
                new DeploymentFunctions.LoadContentAssignments(dispatcher),
                new Outcome<FunctionContext>() {
                    @Override
                    public void onFailure(final FunctionContext context) {
                        Console.error("Unable to find deployments.", context.getErrorMessage());
                    }

                    @Override
                    public void onSuccess(final FunctionContext context) {
                        List<Content> contentRepository = context.pop();
                        List<Content> unassigned = new ArrayList<>();
                        for (Content content : contentRepository) {
                            if (content.getAssignments().isEmpty()) {
                                unassigned.add(content);
                            }
                        }
                        getView().updateUnassigned(unassigned);
                    }
                });
    }

    public void loadServerGroups() {
        getView().updateServerGroups(serverGroupStore.getServerGroups());
    }

    public void launchAddContentWizard() {
        addContentWizard.open("Add Content");
    }

    public void launchAssignContentDialog(Content content) {
        Set<String> assignedServerGroupNames = Sets.newHashSet(
                Lists.transform(content.getAssignments(), Assignment::getServerGroup));
        Set<String> serverGroupNames = Sets.newHashSet(
                Lists.transform(serverGroupStore.getServerGroups(), ServerGroupRecord::getName));
        serverGroupNames.removeAll(assignedServerGroupNames);
        if (serverGroupNames.isEmpty()) {
            Console.warning(content.getName() + " is already assigned to all server groups.");
        } else {
            assignContentDialog.open(content, Ordering.natural().immutableSortedCopy(serverGroupNames));
        }
    }

    public void assignContent(Content content, Set<String> serverGroups, boolean enable) {
        List<Operation> operations = new ArrayList<>();
        for (String serverGroup : serverGroups) {
            ResourceAddress address = new ResourceAddress()
                    .add("server-group", serverGroup)
                    .add("deployment", content.getName());
            Operation operation = new Operation.Builder(ADD, address)
                    .param("runtime-name", content.getRuntimeName())
                    .param("enabled", enable)
                    .build();
            operations.add(operation);
        }
        dispatcher.execute(new DMRAction(new Composite(operations)), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                Console.error("Unable to assign " + content.getName() + ".", caught.getMessage());
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (result.isFailure()) {
                    Console.error("Unable to assign " + content.getName() + ".", result.getFailureDescription());
                } else {
                    Console.info(content.getName() + " successfully assigned to selected server groups.");
                    loadContentRepository();
                }
            }
        });
    }

    public void removeContent(final Content content) {
        Operation operation = new Operation.Builder(REMOVE, new ResourceAddress().add("deployment", content.getName()))
                .build();

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                Console.error("Unable to remove deployment.", caught.getMessage());
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (result.isFailure()) {
                    Console.error("Unable to remove deployment.", result.getFailureDescription());
                } else {
                    Console.info(content.getName() + " successfully removed.");
                    loadUnassignedContent(); // Remove is an action of the "Unassigned Content" column
                }
            }
        });
    }

    public void launchAddAssignmentWizard(final String serverGroup) {
        new Async<FunctionContext>().single(new FunctionContext(),
                new DeploymentFunctions.LoadContentAssignments(dispatcher, serverGroup),
                new Outcome<FunctionContext>() {
                    @Override
                    public void onFailure(final FunctionContext context) {
                        Console.error("Unable to add deployment.", context.getErrorMessage());
                    }

                    @Override
                    public void onSuccess(final FunctionContext context) {
                        List<Content> contentRepository = context.pop();
                        List<Content> unassigned = new ArrayList<>();
                        for (Content content : contentRepository) {
                            if (content.getAssignments().isEmpty()) {
                                unassigned.add(content);
                            }
                        }
                        addDeploymentWizard.open(unassigned, serverGroup);
                    }
                });
    }

    public void launchReplaceAssignmentWizard(final Assignment assignment) {
        replaceWizard.open(assignment);
    }

    public void verifyEnableDisableAssignment(final Assignment assignment) {
        String question;
        String operation;
        String successMessage;
        if (assignment.isEnabled()) {
            operation = "undeploy";
            question = "Disable " + assignment.getName();
            successMessage = assignment.getName() + " successfully disabled.";
        } else {
            operation = "deploy";
            question = "Enable " + assignment.getName();
            successMessage = assignment.getName() + " successfully enabled.";
        }
        Feedback.confirm(Console.CONSTANTS.common_label_areYouSure(), question,
                isConfirmed -> {
                    if (isConfirmed) {
                        modifyAssignment(assignment, operation, successMessage);
                    }
                });
    }

    public void verifyUnassignAssignment(final Assignment assignment) {
        Feedback.confirm(Console.CONSTANTS.common_label_areYouSure(), "Unassign " + assignment.getName(),
                isConfirmed -> {
                    if (isConfirmed) {
                        modifyAssignment(assignment, REMOVE, assignment.getName() + " successfully unassigned.");
                    }
                });
    }

    private void modifyAssignment(final Assignment assignment, final String operation, final String successMessage) {
        String serverGroup = assignment.getServerGroup();
        ResourceAddress address = new ResourceAddress()
                .add("server-group", serverGroup)
                .add("deployment", assignment.getName());
        Operation op = new Operation.Builder(operation, address).build();

        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                Console.error("Unable to modify deployment.", caught.getMessage());
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (result.isFailure()) {
                    Console.error("Unable to modify deployment.", result.getFailureDescription());
                } else {
                    Console.info(successMessage);
                    loadAssignments(serverGroup);
                }
            }
        });
    }

    public void loadAssignments(final String serverGroup) {
        // TODO Optimize - find a way to cache the reference server
        List<Function<FunctionContext>> functions = new ArrayList<>();
        functions.add(new DeploymentFunctions.LoadAssignments(dispatcher, serverGroup));
        functions.add(new TopologyFunctions.ReadHostsAndGroups(dispatcher));
        functions.add(new TopologyFunctions.ReadServerConfigs(dispatcher, beanFactory));
        functions.add(new TopologyFunctions.FindRunningServerInstances(dispatcher));
        functions.add(new DeploymentFunctions.FindReferenceServer(serverGroup));
        functions.add(new DeploymentFunctions.LoadDeploymentsFromReferenceServer(dispatcher));

        Outcome<FunctionContext> outcome = new Outcome<FunctionContext>() {
            @Override
            public void onFailure(final FunctionContext context) {
                String errorMessage = context.getErrorMessage();
                if (DeploymentFunctions.NO_REFERENCE_SERVER_WARNING.equals(errorMessage)) {
                    // not a real error
                    List<Assignment> assignments = context.get(DeploymentFunctions.ASSIGNMENTS);
                    getView().updateAssignments(assignments);
                } else {
                    Console.error("Unable to load deployments", context.getErrorMessage());
                }
            }

            @Override
            public void onSuccess(final FunctionContext context) {
                List<Assignment> assignments = context.get(DeploymentFunctions.ASSIGNMENTS);
                getView().updateAssignments(assignments);
            }
        };

        //noinspection unchecked
        new Async<FunctionContext>(Footer.PROGRESS_ELEMENT).waterfall(new FunctionContext(), outcome,
                functions.toArray(new Function[functions.size()]));
    }
}
