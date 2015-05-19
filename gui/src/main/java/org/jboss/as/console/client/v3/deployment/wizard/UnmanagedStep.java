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
import org.jboss.as.console.client.shared.deployment.model.DeploymentRecord;
import org.jboss.as.console.client.shared.help.StaticHelpPanel;
import org.jboss.as.console.client.v3.widgets.wizard.WizardStep;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.TextAreaItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;

/**
 * @author Harald Pehl
 */
public class UnmanagedStep extends
        WizardStep<Context, State> {

    private Form<DeploymentRecord> form;

    public UnmanagedStep(final DeploymentWizard wizard) {super(wizard, "Unmanaged");}

    @Override
    public Widget asWidget() {
        FlowPanel panel = new FlowPanel();
        panel.add(new StaticHelpPanel(StaticHelp.unmanaged()).asWidget());

        form = new Form<>(DeploymentRecord.class);
        TextBoxItem name = new TextBoxItem("name", "Name");
        TextBoxItem runtimeName = new TextBoxItem("runtimeName", "Runtime Name") {
            @Override
            public void setFiltered(boolean filtered) {
                // ignore
            }
        };
        TextAreaItem path = new TextAreaItem("path", "Path");
        TextBoxItem relativeTo = new TextBoxItem("relativeTo", "Relative To", false);
        CheckBoxItem archive = new CheckBoxItem("archive", "Is Archive?");
        archive.setValue(true);
        CheckBoxItem enabled = new CheckBoxItem("enabled", "Enable");
        form.setFields(path, relativeTo, archive, name, runtimeName, enabled);
        panel.add(form.asWidget());

        return panel;
    }

    @Override
    public void reset() {
        form.clearValues();
    }

    @Override
    protected boolean onNext(final Context context) {
        FormValidation validation = form.validate();
        if (validation.hasErrors()) {
            return false;
        }
        context.unmanagedDeployment = form.getUpdatedEntity();
        return true; // actual upload is done in AddDomainDeploymentWizard.finish()
    }
}
