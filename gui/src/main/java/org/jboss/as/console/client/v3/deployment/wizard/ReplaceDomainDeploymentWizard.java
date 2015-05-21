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

import com.google.gwt.user.client.ui.PopupPanel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.v3.deployment.Assignment;
import org.jboss.as.console.client.v3.deployment.DeploymentFunctions;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Outcome;

import java.util.EnumSet;

import static org.jboss.as.console.client.v3.deployment.wizard.State.UPLOAD;
import static org.jboss.as.console.client.v3.deployment.wizard.State.VERIFY_UPLOAD;

/**
 * @author Harald Pehl
 */
public class ReplaceDomainDeploymentWizard extends DeploymentWizard implements ReplaceDeploymentWizard {

    private Assignment assignment;

    public ReplaceDomainDeploymentWizard(BootstrapContext bootstrapContext, BeanFactory beanFactory,
            DispatchAsync dispatcher, FinishCallback onFinish) {
        super("replace_deployment", bootstrapContext, beanFactory, dispatcher, onFinish);

        addStep(UPLOAD, new UploadStep(this, bootstrapContext));
        addStep(VERIFY_UPLOAD, new VerifyUploadStep(this));
    }

    public void open(final Assignment assignment) {
        this.assignment = assignment;
        super.open("Replace Deployment");
        context.serverGroup = assignment.getServerGroup();
    }

    @Override
    protected State initialState() {
        return UPLOAD;
    }

    @Override
    protected EnumSet<State> lastStates() {
        return EnumSet.of(VERIFY_UPLOAD);
    }

    @Override
    protected State back(final State state) {
        return state == VERIFY_UPLOAD ? UPLOAD : null;
    }

    @Override
    protected State next(final State state) {
        return state == UPLOAD ? VERIFY_UPLOAD : null;
    }

    @Override
    protected void finish() {
        final PopupPanel loading = Feedback.loading(
                Console.CONSTANTS.common_label_plaseWait(),
                Console.CONSTANTS.common_label_requestProcessed(), () -> {}
        );

        final Context wizardContext = context;
        final Outcome<FunctionContext> outcome = new Outcome<FunctionContext>() {
            @Override
            public void onFailure(final FunctionContext context) {
                loading.hide();
                showError(context.getErrorMessage());
            }

            @Override
            public void onSuccess(final FunctionContext context) {
                loading.hide();
                close();
                onFinish.onFinish(wizardContext);
            }
        };

        context.upload.setEnableAfterDeployment(assignment.isEnabled());
        new Async<FunctionContext>(Footer.PROGRESS_ELEMENT).waterfall(new FunctionContext(), outcome,
                new DeploymentFunctions.UploadContent(context.uploadForm, context.fileUpload, context.upload),
                new DeploymentFunctions.AddOrReplaceContent(bootstrapContext, true));
    }
}
