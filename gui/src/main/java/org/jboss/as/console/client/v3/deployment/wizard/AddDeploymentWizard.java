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
package org.jboss.as.console.client.v3.deployment.wizard;

import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.PopupPanel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.core.UIMessages;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.deployment.DeploymentReference;
import org.jboss.as.console.client.shared.deployment.model.DeploymentRecord;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.v3.deployment.Content;
import org.jboss.as.console.client.v3.deployment.DeploymentFinder;
import org.jboss.as.console.client.v3.deployment.DeploymentFunctions;
import org.jboss.as.console.client.v3.widgets.wizard.Wizard;
import org.jboss.as.console.client.widgets.forms.UploadForm;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Outcome;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.jboss.as.console.client.v3.deployment.wizard.AddDeploymentWizard.State.*;

/**
 * @author Harald Pehl
 */
public class AddDeploymentWizard extends Wizard<AddDeploymentWizard.Context, AddDeploymentWizard.State> {

    static class Context {

        final boolean standalone;
        final List<Content> contentRepository;
        String serverGroup;
        boolean deployNew;
        boolean deployExisting;
        boolean deployUnmanaged;
        FileUpload fileUpload;
        UploadForm uploadForm;
        DeploymentReference upload;
        Content existingContent;
        boolean enableExistingContent;
        DeploymentRecord unmanagedDeployment;

        public Context(final boolean standalone) {
            this.standalone = standalone;
            this.contentRepository = new ArrayList<>();
        }
    }


    enum State {
        CHOOSE, UPLOAD, VERIFY_UPLOAD, CONTENT_REPOSITORY, UNMANAGED
    }


    static final UIMessages MESSAGES = Console.MESSAGES;

    private final BootstrapContext bootstrapContext;
    private final BeanFactory beanFactory;
    private final DispatchAsync dispatcher;
    private final DeploymentFinder deploymentFinder;

    public AddDeploymentWizard(BootstrapContext bootstrapContext, BeanFactory beanFactory, DispatchAsync dispatcher,
            DeploymentFinder deploymentFinder) {
        super("add_deployment", new Context(bootstrapContext.isStandalone()), MESSAGES.createTitle("Deployment"));
        this.bootstrapContext = bootstrapContext;
        this.beanFactory = beanFactory;
        this.dispatcher = dispatcher;
        this.deploymentFinder = deploymentFinder;

        addStep(CHOOSE, new ChooseStep(this));
        addStep(UPLOAD, new UploadStep(this, bootstrapContext));
        addStep(VERIFY_UPLOAD, new VerifyUploadStep(this));
        addStep(CONTENT_REPOSITORY, new ContentRepositoryStep(this));
        addStep(UNMANAGED, new UnmanagedStep(this));
    }

    public void open(List<Content> contentRepository, String serverGroup) {
        super.open();
        context.contentRepository.clear();
        context.contentRepository.addAll(contentRepository);
        context.serverGroup = serverGroup;
    }

    @Override
    protected void resetContext() {
        context.deployNew = false;
        context.deployExisting = false;
        context.deployUnmanaged = false;
        context.upload = beanFactory.deploymentReference().as();
        context.existingContent = null;
        context.enableExistingContent = false;
        context.unmanagedDeployment = beanFactory.deployment().as();
    }

    @Override
    protected EnumSet<State> lastSteps() {
        //noinspection NonJREEmulationClassesInClientCode
        return EnumSet.of(VERIFY_UPLOAD, CONTENT_REPOSITORY, UNMANAGED);
    }

    @Override
    protected State back(final State state) {
        State previous = null;
        switch (state) {
            case CHOOSE:
                previous = null;
                break;
            case UPLOAD:
                previous = CHOOSE;
                break;
            case VERIFY_UPLOAD:
                previous = UPLOAD;
                break;
            case CONTENT_REPOSITORY:
                previous = CHOOSE;
                break;
            case UNMANAGED:
                previous = CHOOSE;
                break;
        }
        return previous;
    }

    @Override
    protected State next(final State state) {
        State next = null;
        switch (state) {
            case CHOOSE:
                if (context.deployNew) {
                    next = UPLOAD;
                } else if (context.deployExisting) {
                    next = CONTENT_REPOSITORY;
                } else if (context.deployUnmanaged) {
                    next = UNMANAGED;
                }
                break;
            case UPLOAD:
                next = VERIFY_UPLOAD;
                break;
            case VERIFY_UPLOAD:
                next = null;
                break;
            case CONTENT_REPOSITORY:
                next = null;
                break;
            case UNMANAGED:
                next = null;
                break;
        }
        return next;
    }

    @Override
    protected void finish() {
        if (context.deployNew) {
            uploadAddContentAndAssign();

        } else if (context.deployExisting) {
            addAssignment();

        } else if (context.deployUnmanaged) {
            Console.warning("Uploading unmanaged deployment " + context.unmanagedDeployment.getName());
            close();
        }
    }

    private void uploadAddContentAndAssign() {
        final PopupPanel loading = Feedback.loading(
                Console.CONSTANTS.common_label_plaseWait(),
                Console.CONSTANTS.common_label_requestProcessed(), () -> {}
        );

        final Context wizardContext = context;
        Outcome<FunctionContext> outcome = new Outcome<FunctionContext>() {
            @Override
            public void onFailure(final FunctionContext context) {
                loading.hide();
                showError(context.getErrorMessage());
            }

            @Override
            public void onSuccess(final FunctionContext context) {
                loading.hide();
                close();
                deploymentFinder.loadAssignments(wizardContext.serverGroup, false);
            }
        };

        final FunctionContext functionContext = new FunctionContext();
        functionContext.push(wizardContext.upload);

        new Async<FunctionContext>(Footer.PROGRESS_ELEMENT).waterfall(functionContext, outcome,
                new DeploymentFunctions.Upload(wizardContext.uploadForm, wizardContext.fileUpload),
                new DeploymentFunctions.AddContent(bootstrapContext, false),
                new DeploymentFunctions.AddAssignment(dispatcher, wizardContext.serverGroup,
                        wizardContext.upload.isEnableAfterDeployment()));
    }

    private void addAssignment() {
        final Context wizardContext = context;
        Outcome<FunctionContext> outcome = new Outcome<FunctionContext>() {
            @Override
            public void onFailure(final FunctionContext context) {
                showError(context.getErrorMessage());
            }

            @Override
            public void onSuccess(final FunctionContext context) {
                close();
                deploymentFinder.loadAssignments(wizardContext.serverGroup, false);
            }
        };

        final FunctionContext functionContext = new FunctionContext();
        functionContext.push(wizardContext.existingContent);

        new Async<FunctionContext>(Footer.PROGRESS_ELEMENT).waterfall(functionContext, outcome,
                new DeploymentFunctions.AddAssignment(dispatcher, wizardContext.serverGroup,
                        wizardContext.enableExistingContent));
    }
}
