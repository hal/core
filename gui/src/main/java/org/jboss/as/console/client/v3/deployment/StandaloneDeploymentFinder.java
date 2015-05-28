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
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.rbac.UnauthorisedPresenter;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.state.PerspectivePresenter;
import org.jboss.as.console.client.v3.deployment.wizard.AddStandaloneDeploymentWizard;
import org.jboss.as.console.client.v3.deployment.wizard.ReplaceStandaloneDeploymentWizard;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.client.v3.presenter.Finder;
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

import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.console.spi.OperationMode.Mode.STANDALONE;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
public class StandaloneDeploymentFinder
        extends PerspectivePresenter<StandaloneDeploymentFinder.MyView, StandaloneDeploymentFinder.MyProxy>
        implements Finder, PreviewEvent.Handler, FinderScrollEvent.Handler, ClearFinderSelectionEvent.Handler {

    // @formatter:off --------------------------------------- proxy & view

    @ProxyCodeSplit
    @OperationMode(STANDALONE)
    @NameToken(NameTokens.StandaloneDeploymentFinder)
    @SearchIndex(keywords = {"deployment", "war", "ear", "application"})
    @RequiredResources(resources = "/deployment=*", recursive = false)
    public interface MyProxy extends ProxyPlace<StandaloneDeploymentFinder> {}

    public interface MyView extends View, HasPresenter<StandaloneDeploymentFinder> {
        void updateDeployments(Iterable<Deployment> deployments);

        void setPreview(SafeHtml html);
        void clearActiveSelection(ClearFinderSelectionEvent event);
        void toggleScrolling(boolean enforceScrolling, int requiredWidth);
    }

    // @formatter:on ---------------------------------------- instance data


    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent = new GwtEvent.Type<>();

    private final DispatchAsync dispatcher;
    private final AddStandaloneDeploymentWizard addWizard;
    private final ReplaceStandaloneDeploymentWizard replaceWizard;


    // ------------------------------------------------------ presenter lifecycle

    @Inject
    public StandaloneDeploymentFinder(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final PlaceManager placeManager, final UnauthorisedPresenter unauthorisedPresenter,
            final BeanFactory beanFactory, final DispatchAsync dispatcher,
            final BootstrapContext bootstrapContext, final Header header) {
        super(eventBus, view, proxy, placeManager, header, NameTokens.StandaloneDeploymentFinder, TYPE_MainContent);
        this.dispatcher = dispatcher;

        this.addWizard = new AddStandaloneDeploymentWizard(bootstrapContext, beanFactory, dispatcher,
                context -> loadDeployments());
        this.replaceWizard = new ReplaceStandaloneDeploymentWizard(bootstrapContext, beanFactory, dispatcher,
                context -> loadDeployments());
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
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
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
                Console.error("Unable to load deployments", caught.getMessage());
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (result.isFailure()) {
                    Console.error("Unable to load deployments", result.getFailureDescription());
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
        addWizard.open("Add Deployment");
    }

    public void launchReplaceDeploymentWizard(final Deployment deployment) {
        replaceWizard.open(deployment);
    }

    public void verifyEnableDisableDeployment(final Deployment deployment) {
        String message;
        String operation;
        if (deployment.isEnabled()) {
            message = "Disable " + deployment.getName();
            operation = "undeploy";
        } else {
            message = "Enable " + deployment.getName();
            operation = "deploy";
        }
        Feedback.confirm(Console.CONSTANTS.common_label_areYouSure(), message,
                isConfirmed -> {
                    if (isConfirmed) {
                        modifyDeployment(deployment, operation);
                    }
                });
    }

    public void verifyRemoveDeployment(final Deployment deployment) {
        Feedback.confirm(Console.CONSTANTS.common_label_areYouSure(), "Remove " + deployment.getName(),
                isConfirmed -> {
                    if (isConfirmed) {
                        modifyDeployment(deployment, REMOVE);
                    }
                });
    }

    private void modifyDeployment(final Deployment deployment, final String operation) {
        ResourceAddress address = new ResourceAddress().add("deployment", deployment.getName());
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
                    loadDeployments();
                }
            }
        });
    }


    // ------------------------------------------------------ finder related methods

    @Override
    public void onPreview(PreviewEvent event) {
        if(isVisible())
            getView().setPreview(event.getHtml());
    }

    @Override
    public void onToggleScrolling(final FinderScrollEvent event) {
        if(isVisible())
            getView().toggleScrolling(event.isEnforceScrolling(), event.getRequiredWidth());
    }

    @Override
    public void onClearActiveSelection(final ClearFinderSelectionEvent event) {
        if(isVisible())
            getView().clearActiveSelection(event);
    }
}
