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
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.deployment.DeploymentReference;
import org.jboss.as.console.client.shared.help.StaticHelpPanel;
import org.jboss.as.console.client.v3.widgets.wizard.WizardStep;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;

/**
 * @author Harald Pehl
 */
public class VerifyUploadStep extends
        WizardStep<Context, State> {

    private Form<DeploymentReference> form;

    public VerifyUploadStep(final DeploymentWizard wizard) {
        super(wizard, "Verify Upload");
    }

    @Override
    public Widget asWidget() {
        FlowPanel panel = new FlowPanel();
        panel.add(new StaticHelpPanel(StaticHelp.replace()).asWidget());

        form = new Form<>(DeploymentReference.class);
        TextBoxItem nameField = new TextBoxItem("name", Console.CONSTANTS.common_label_name());
        TextBoxItem runtimeNameField = new TextBoxItem("runtimeName", Console.CONSTANTS.common_label_runtimeName());
        CheckBoxItem enable = new CheckBoxItem("enableAfterDeployment", "Enable");
        if (wizard instanceof AddDeploymentWizard) {
            form.setFields(nameField, runtimeNameField, enable);
        } else if (wizard instanceof ReplaceDeploymentWizard) {
            form.setFields(nameField, runtimeNameField);
        }
        panel.add(form.asWidget());

        return panel;
    }

    @Override
    public void reset() {
        form.clearValues();
    }

    @Override
    protected void onShow(final Context context) {
        form.edit(context.upload);
    }

    @Override
    protected boolean onNext(final Context context) {
        FormValidation validation = form.validate();
        if (validation.hasErrors()) {
            return false;
        }
        context.upload = form.getUpdatedEntity();
        return true;
    }
}
