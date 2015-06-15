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

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.util.IdHelper;
import org.jboss.as.console.client.v3.widgets.wizard.WizardStep;

/**
 * @author Harald Pehl
 */
public class ChooseStep extends
        WizardStep<Context, State> {

    private final boolean standalone;
    private RadioButton deployNew;
    private RadioButton deployExisting;
    private RadioButton deployUnmanaged;

    public ChooseStep(final DeploymentWizard wizard, final boolean standalone) {
        super(wizard, "Please Choose");
        this.standalone = standalone;
    }

    @Override
    public Widget asWidget() {
        FlowPanel body = new FlowPanel();

        deployNew = new RadioButton("deployment_kind", "Upload a new deployment");
        deployNew.addStyleName("radio-block");
        IdHelper.setId(deployNew, id(), "deployNew");

        deployExisting = new RadioButton("deployment_kind", "Choose a deployment from the content repository");
        deployExisting.addStyleName("radio-block");
        IdHelper.setId(deployExisting, id(), "deployExisting");

        deployUnmanaged = new RadioButton("deployment_kind", "Create an unmanaged deployment");
        deployUnmanaged.addStyleName("radio-block");
        IdHelper.setId(deployUnmanaged, id(), "deployUnmanaged");

        body.add(deployNew);
        if (!standalone) {
            body.add(deployExisting);
        }
        body.add(deployUnmanaged);
        return body;
    }

    @Override
    public void reset() {
        deployNew.setValue(true);
        deployExisting.setValue(false);
        deployUnmanaged.setValue(false);
    }

    @Override
    protected boolean onNext(final Context context) {
        context.deployNew = deployNew.getValue();
        context.deployExisting = deployExisting.getValue();
        context.deployUnmanaged = deployUnmanaged.getValue();
        return true;
    }
}
