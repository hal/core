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

import static org.jboss.as.console.client.v3.deployment.wizard.State.UPLOAD;

import java.util.EnumSet;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.v3.deployment.Content;
import org.jboss.as.console.client.v3.deployment.DeploymentFunctions;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Async;

import com.google.gwt.user.client.ui.PopupPanel;

/**
 * @author Harald Pehl
 */
public abstract class ReplaceDeploymentWizard extends DeploymentWizard {

    private Content content;

    public ReplaceDeploymentWizard(BootstrapContext bootstrapContext, BeanFactory beanFactory,
            DispatchAsync dispatcher, FinishCallback onFinish) {
        super("replace_deployment", bootstrapContext, beanFactory, dispatcher, onFinish);

        addStep(UPLOAD, new UploadStep(this));
    }

    public void open(Content content) {
        this.content = content;
        super.open(Console.CONSTANTS.replaceDeployment());
    }

    @Override
    protected State initialState() {
        return UPLOAD;
    }

    @Override
    protected EnumSet<State> lastStates() {
        return EnumSet.of(UPLOAD);
    }

    @Override
    protected State back(final State state) {
        return null;
    }

    @Override
    protected State next(final State state) {
        return null;
    }

    @Override
    protected void finish() {
        final PopupPanel loading = Feedback.loading(
                Console.CONSTANTS.common_label_plaseWait(),
                Console.CONSTANTS.common_label_requestProcessed(), () -> {}
        );

        // as it is as replace operation, the new upload name/runtime must preserve the actual deployment information.
        context.upload.setName(content.getName());
        context.upload.setRuntimeName(content.getRuntimeName());

        new Async<FunctionContext>(Footer.PROGRESS_ELEMENT).single(new FunctionContext(),
                new DeploymentWizardOutcome(loading, context),
                new DeploymentFunctions.UploadContent(dispatcher, context.fileUpload, context.upload, true));
    }
}
