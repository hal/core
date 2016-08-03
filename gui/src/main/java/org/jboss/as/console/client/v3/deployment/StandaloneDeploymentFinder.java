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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.v3.deployment.wizard.AddStandaloneDeploymentWizard;
import org.jboss.as.console.client.v3.deployment.wizard.ReplaceStandaloneDeploymentWizard;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
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
import org.jboss.dmr.client.dispatch.impl.UploadHandler;

import static org.jboss.as.console.spi.OperationMode.Mode.STANDALONE;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
public class StandaloneDeploymentFinder
        extends DeploymentFinder<StandaloneDeploymentFinder.MyView, StandaloneDeploymentFinder.MyProxy> {

    // @formatter:off --------------------------------------- proxy & view

    @ProxyCodeSplit
    @OperationMode(STANDALONE)
    @NameToken(NameTokens.StandaloneDeploymentFinder)
    @SearchIndex(keywords = {"deployment", "war", "ear", "application"})
    @RequiredResources(resources = "/deployment=*", recursive = false)
    public interface MyProxy extends ProxyPlace<StandaloneDeploymentFinder> {}

    public interface MyView extends DeploymentFinder.DeploymentView<StandaloneDeploymentFinder> {
        void updateDeployments(Iterable<Deployment> deployments);
    }


    // @formatter:on ---------------------------------------- instance data

    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent = new GwtEvent.Type<>();

    private final PlaceManager placeManager;
    private final DispatchAsync dispatcher;
    private final AddStandaloneDeploymentWizard addWizard;
    private final ReplaceStandaloneDeploymentWizard replaceWizard;


    // ------------------------------------------------------ presenter lifecycle

    @Inject
    public StandaloneDeploymentFinder(final EventBus eventBus, final MyView view, final MyProxy proxy,
                                      final PlaceManager placeManager, final BeanFactory beanFactory, final DispatchAsync dispatcher,
                                      final BootstrapContext bootstrapContext, final Header header) {
        super(eventBus, view, proxy, placeManager, header, NameTokens.StandaloneDeploymentFinder, TYPE_MainContent);
        this.placeManager = placeManager;
        this.dispatcher = dispatcher;

        this.addWizard = new AddStandaloneDeploymentWizard(bootstrapContext, beanFactory, dispatcher,
                context -> {
                    String name = context.deployNew ?
                            context.upload.getName() :
                            context.unmanagedDeployment.getName();
                    Console.info(Console.MESSAGES.deploymentSuccessfullyDeployed(name));
                    loadDeployments();
                });
        this.replaceWizard = new ReplaceStandaloneDeploymentWizard(bootstrapContext, beanFactory, dispatcher,
                context -> {
                    Console.info(Console.MESSAGES.deploymentSuccessfullyReplaced(context.upload.getName()));
                    loadDeployments();
                });
    }

    @Override
    protected void onFirstReveal(final PlaceRequest placeRequest, final PlaceManager placeManager,
                                 final boolean revealDefault) {
        // noop
    }

    @Override
    protected void onReset() {
        super.onReset();
        loadDeployments();
        Console.MODULES.getHeader().highlight(getProxy().getNameToken());
    }


    // ------------------------------------------------------ deployment methods

    public void loadDeployments() {
        Operation op = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, ResourceAddress.ROOT)
                .param(CHILD_TYPE, "deployment")
                .param(INCLUDE_RUNTIME, true)
                .param(RECURSIVE, true)
                .build();
        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                Console.error(Console.CONSTANTS.unableToLoadDeployments(), caught.getMessage());
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (result.isFailure()) {
                    Console.error(Console.CONSTANTS.unableToLoadDeployments(), result.getFailureDescription());
                } else {
                    List<Deployment> deployments = new ArrayList<>();
                    ModelNode payload = result.get(RESULT);
                    List<Property> properties = payload.asPropertyList();
                    for (Property property : properties) {
                        deployments.add(new Deployment(ReferenceServer.STANDALONE, property.getValue()));
                    }
                    getView().updateDeployments(deployments);
                }
            }
        });
    }

    public void launchAddDeploymentWizard() {
        if (!UploadHandler.verifySupport()) {
            Console.warning(Console.CONSTANTS.uploadsNotSupported(), Console.CONSTANTS.noUploadDueToSecurityReasons());
        } else {
            addWizard.open(Console.MESSAGES.newTitle("Deployment"));
        }
    }

    public void launchReplaceDeploymentWizard(Content content) {
        replaceWizard.open(content);
    }

    public void verifyEnableDisableDeployment(final Deployment deployment) {
        String question;
        String operation;
        String successMessage;
        //noinspection Duplicates
        if (deployment.isEnabled()) {
            operation = "undeploy";
            question = Console.MESSAGES.disableConfirm(deployment.getName());
            successMessage = Console.MESSAGES.successDisabled(deployment.getName());
        } else {
            operation = "deploy";
            question = Console.MESSAGES.enableConfirm(deployment.getName());
            successMessage = Console.MESSAGES.successEnabled(deployment.getName());
        }
        Feedback.confirm(Console.CONSTANTS.common_label_areYouSure(), question,
                isConfirmed -> {
                    if (isConfirmed) {
                        modifyDeployment(deployment, operation, successMessage);
                    }
                });
    }

    public void verifyRemoveDeployment(final Deployment deployment) {
        Feedback.confirm(Console.CONSTANTS.common_label_areYouSure(), Console.MESSAGES.deleteTitle(deployment.getName()),
                isConfirmed -> {
                    if (isConfirmed) {
                        modifyDeployment(deployment, REMOVE, Console.MESSAGES.successfullyRemoved(deployment.getName()));
                    }
                });
    }

    public void showDetails() {
        placeManager.revealRelativePlace(new PlaceRequest.Builder().nameToken(NameTokens.DeploymentDetails).build());
    }

    private void modifyDeployment(final Deployment deployment, final String operation, final String successMessage) {
        ResourceAddress address = new ResourceAddress().add("deployment", deployment.getName());
        Operation op = new Operation.Builder(operation, address).build();
        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                Console.error(Console.CONSTANTS.unableToModifyDeployment(), caught.getMessage());
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (result.isFailure()) {
                    Console.error(Console.CONSTANTS.unableToModifyDeployment(), result.getFailureDescription());
                } else {
                    Console.info(successMessage);
                    loadDeployments();
                }
            }
        });
    }

    public void explodeContent(final Deployment content) {
        Operation operation = new Operation.Builder(EXPLODE, new ResourceAddress().add("deployment", content.getName()))
                .build();

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                Console.error(Console.CONSTANTS.unableToExplodeDeployment(), caught.getMessage());
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (result.isFailure()) {
                    Console.error(Console.CONSTANTS.unableToExplodeDeployment(), result.getFailureDescription());
                } else {
                    Console.info(content.getName() + " successfully exploded.");
                    loadDeployments();
                }
            }
        });
    }

    public void browseContent() {
        placeManager.revealRelativePlace(new PlaceRequest.Builder().nameToken(NameTokens.DeploymentBrowseContent).build());
    }
    
    static java.util.logging.Logger _log = java.util.logging.Logger.getLogger("org.jboss");

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
}
