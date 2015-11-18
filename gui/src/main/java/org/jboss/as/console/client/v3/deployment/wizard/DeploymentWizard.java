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
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.v3.widgets.wizard.Wizard;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Outcome;

/**
 * @author Harald Pehl
 */
public abstract class DeploymentWizard extends Wizard<Context, State> {

    @FunctionalInterface
    public interface FinishCallback {

        void onFinish(Context context);
    }

    public class DeploymentWizardOutcome implements Outcome<FunctionContext> {

        private final PopupPanel loading;
        private final Context wizardContext;

        public DeploymentWizardOutcome(final PopupPanel loading, final Context wizardContext) {
            this.loading = loading;
            this.wizardContext = wizardContext;
        }

        @Override
        public void onFailure(final FunctionContext functionContext) {
            loading.hide();
            showError(functionContext.getErrorMessage());
        }

        @Override
        public void onSuccess(final FunctionContext functionContext) {
            loading.hide();
            close();
            onFinish.onFinish(wizardContext);
        }
    }


    protected final BootstrapContext bootstrapContext;
    protected final BeanFactory beanFactory;
    protected final DispatchAsync dispatcher;
    protected final FinishCallback onFinish;

    public DeploymentWizard(String id,
            BootstrapContext bootstrapContext, BeanFactory beanFactory, DispatchAsync dispatcher,
            FinishCallback onFinish) {
        super(id, new Context(bootstrapContext.isStandalone()));

        this.bootstrapContext = bootstrapContext;
        this.beanFactory = beanFactory;
        this.dispatcher = dispatcher;
        this.onFinish = onFinish;
    }

    @Override
    protected void resetContext() {
        context.deployNew = false;
        context.deployExisting = false;
        context.deployUnmanaged = false;
        context.upload = beanFactory.upload().as();
        context.existingContent = null;
        context.enableExistingContent = false;
        context.unmanagedDeployment = beanFactory.unmanaged().as();
    }
}
