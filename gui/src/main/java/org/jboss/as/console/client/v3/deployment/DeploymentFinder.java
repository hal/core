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

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.PopupPanel;
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
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.groups.deployment.ServerGroupSelection;
import org.jboss.as.console.client.domain.groups.deployment.ServerGroupSelector;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.UnauthorisedPresenter;
import org.jboss.as.console.client.shared.deployment.DeployCommandExecutor;
import org.jboss.as.console.client.shared.deployment.DeploymentCommand;
import org.jboss.as.console.client.shared.deployment.DeploymentStore;
import org.jboss.as.console.client.shared.deployment.NewDeploymentWizard;
import org.jboss.as.console.client.shared.deployment.model.ContentRepository;
import org.jboss.as.console.client.shared.deployment.model.DeploymentRecord;
import org.jboss.as.console.client.shared.state.PerspectivePresenter;
import org.jboss.as.console.client.v3.presenter.Finder;
import org.jboss.as.console.client.widgets.nav.v3.FinderScrollEvent;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;
import org.jboss.as.console.spi.OperationMode;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.jboss.as.console.spi.OperationMode.Mode.DOMAIN;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * TODO Remove dependency to PerspectivePresenter
 *
 * @author Harald Pehl
 */
public class DeploymentFinder
        extends PerspectivePresenter<DeploymentFinder.MyView, DeploymentFinder.MyProxy>
        implements DeployCommandExecutor, Finder, PreviewEvent.Handler, FinderScrollEvent.Handler {

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
        void setPreview(SafeHtml html);
        void toggleScrolling(boolean enforceScrolling, int requiredWidth);
        void updateDeployments(List<DeploymentRecord> deployments);
        void updateServerGroups(List<ServerGroupAssignment> assignments);
    }

    // @formatter:on ---------------------------------------- instance data


    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent = new GwtEvent.Type<RevealContentHandler<?>>();
    private final DispatchAsync dispatcher;
    private final PlaceManager placeManager;
    private final DeploymentStore deploymentStore;
    private ContentRepository contentRepository;
    private DefaultWindow window;


    // ------------------------------------------------------ presenter lifecycle

    @Inject
    public DeploymentFinder(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final DispatchAsync dispatcher, final PlaceManager placeManager,
            final Header header, final UnauthorisedPresenter unauthorisedPresenter,
            final DeploymentStore deploymentStore) {
        super(eventBus, view, proxy, placeManager, header, NameTokens.DeploymentFinder,
                unauthorisedPresenter, TYPE_MainContent);
        this.dispatcher = dispatcher;
        this.placeManager = placeManager;
        this.deploymentStore = deploymentStore;
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
    protected void onFirstReveal(final PlaceRequest placeRequest, final PlaceManager placeManager,
            final boolean revealDefault) {
        // TODO Migrate to store / change events!
        loadContentRepository();
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }

    @Override
    protected void onReset() {
        super.onReset();
    }

    public PlaceManager getPlaceManager() {
        return placeManager;
    }


    // ------------------------------------------------------ deployment related methods

    private void loadContentRepository() {
        deploymentStore.loadContentRepository(new SimpleCallback<ContentRepository>() {
            @Override
            public void onSuccess(final ContentRepository result) {
                contentRepository = result;
                getView().updateDeployments(contentRepository.getDeployments());
            }
        });
    }

    public void loadAssignmentsFor(final DeploymentRecord selectedDeployment) {
        // TODO Reduce duplicate code
        deploymentStore.loadContentRepository(new SimpleCallback<ContentRepository>() {
            @Override
            public void onSuccess(final ContentRepository result) {
                contentRepository = result;
                List<String> serverGroups = result.getServerGroups(selectedDeployment);
                List<ServerGroupAssignment> assignments = new ArrayList<>();
                for (String serverGroup : serverGroups) {
                    // TODO We need the enabled / disabled state per *server-group*
                    assignments.add(new ServerGroupAssignment(selectedDeployment, serverGroup));
                }
                getView().updateServerGroups(assignments);
            }
        });
    }

    @Override
    public void onAssignToServerGroup(final DeploymentRecord deployment, final boolean enable,
            final Set<ServerGroupSelection> selectedGroups) {

        final PopupPanel loading = Feedback.loading(
                Console.CONSTANTS.common_label_plaseWait(),
                Console.CONSTANTS.common_label_requestProcessed(),
                () -> {});

        Set<String> names = new HashSet<>();
        for (ServerGroupSelection group : selectedGroups) { names.add(group.getName()); }

        deploymentStore.addToServerGroups(names, enable, deployment, new SimpleCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                loading.hide();
                Console.error(Console.MESSAGES.addingFailed("Deployment " + deployment.getRuntimeName()),
                        caught.getMessage());
            }

            @Override
            public void onSuccess(DMRResponse response) {
                loading.hide();
                ModelNode result = response.get();
                if (result.isFailure()) {
                    Console.error(Console.MESSAGES.addingFailed("Deployment " + deployment.getRuntimeName()),
                            result.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES
                            .added("Deployment " + deployment.getRuntimeName() + " to group " + selectedGroups));
                }
                refreshDeployments();
            }
        });
    }

    @Override
    public void enableDisableDeployment(final DeploymentRecord deployment) {
        final String success;
        final String failed;
        if (deployment.isEnabled()) {
            success = Console.MESSAGES.successDisabled(deployment.getRuntimeName());
            failed = Console.MESSAGES.failedToDisable(deployment.getRuntimeName());
        } else {
            success = Console.MESSAGES.successEnabled(deployment.getRuntimeName());
            failed = Console.MESSAGES.failedToEnable(deployment.getRuntimeName());
        }
        final PopupPanel loading = Feedback.loading(
                Console.CONSTANTS.common_label_plaseWait(),
                Console.CONSTANTS.common_label_requestProcessed(),
                () -> {});

        deploymentStore.enableDisableDeployment(deployment, new SimpleCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                loading.hide();
                Console.error(failed, caught.getMessage());
            }

            @Override
            public void onSuccess(DMRResponse response) {
                loading.hide();
                ModelNode result = response.get();
                if (result.isFailure()) {
                    Console.error(failed, result.getFailureDescription());
                } else {
                    Console.info(success);
                }
                refreshDeployments();
            }
        });
    }

    @Override
    public void updateDeployment(final DeploymentRecord deployment) {

    }

    @Override
    public void removeDeploymentFromGroup(final DeploymentRecord deployment) {
        deploymentStore.removeDeploymentFromGroup(deployment, new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse response) {
                refreshDeployments();
                DeploymentCommand.REMOVE_FROM_GROUP.displaySuccessMessage(DeploymentFinder.this, deployment);
            }

            @Override
            public void onFailure(Throwable t) {
                super.onFailure(t);
                refreshDeployments();
                DeploymentCommand.REMOVE_FROM_GROUP
                        .displayFailureMessage(DeploymentFinder.this, deployment, t);
            }
        });
    }

    @Override
    public void onRemoveContent(final DeploymentRecord deployment) {
        List<String> assignedGroups = contentRepository.getServerGroups(deployment);

        final PopupPanel loading = Feedback.loading(
                Console.CONSTANTS.common_label_plaseWait(),
                Console.CONSTANTS.common_label_requestProcessed(),
                () -> {});

        ModelNode operation = new ModelNode();
        operation.get(OP).set(COMPOSITE);
        operation.get(ADDRESS).setEmptyList();

        List<ModelNode> steps = new LinkedList<>();
        for (String group : assignedGroups) {
            ModelNode groupOp = new ModelNode();
            groupOp.get(OP).set(REMOVE);
            groupOp.get(ADDRESS).add("server-group", group);
            groupOp.get(ADDRESS).add("deployment", deployment.getName());
            steps.add(groupOp);
        }

        ModelNode removeContentOp = new ModelNode();
        removeContentOp.get(OP).set(REMOVE);
        removeContentOp.get(ADDRESS).add("deployment", deployment.getName());
        steps.add(removeContentOp);
        operation.get(STEPS).set(steps);
        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                loading.hide();

                ModelNode result = dmrResponse.get();

                if (result.isFailure()) {
                    Console.error(Console.MESSAGES.deletionFailed("Deployment " + deployment.getRuntimeName()),
                            result.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.deleted("Deployment " + deployment.getRuntimeName()));
                }
                refreshDeployments();
            }
        });
    }

    @Override
    public List<ServerGroupRecord> getPossibleGroupAssignments(final DeploymentRecord record) {
        return contentRepository.getPossibleServerGroupAssignments(record);
    }

    @Override
    public void launchGroupSelectionWizard(final DeploymentRecord record) {
        new ServerGroupSelector(this, record);
    }

    @Override
    public void onCreateUnmanaged(final DeploymentRecord entity) {

    }

    @Override
    public void refreshDeployments() {
        loadContentRepository();
    }

    public void launchNewDeploymentDialoge(DeploymentRecord record, boolean isUpdate) {
        launchDeploymentDialoge(Console.MESSAGES.createTitle("Deployment"), record, isUpdate);
    }

    public void launchDeploymentDialoge(String title, DeploymentRecord record, boolean isUpdate) {
        window = new DefaultWindow(title);
        window.setWidth(480);
        window.setHeight(480);
        window.trapWidget(
                new NewDeploymentWizard(this, window, isUpdate, record).asWidget());
        window.setGlassEnabled(true);
        window.center();
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


    static class ServerGroupAssignment {

        final DeploymentRecord deployment;
        final String serverGroup;

        ServerGroupAssignment(final DeploymentRecord deployment, final String serverGroup) {
            this.deployment = deployment;
            this.serverGroup = serverGroup;
        }
    }
}
