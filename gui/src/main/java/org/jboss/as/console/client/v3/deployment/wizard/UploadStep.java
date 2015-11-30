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

import com.google.common.base.Strings;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.shared.util.IdHelper;
import org.jboss.as.console.client.v3.widgets.wizard.WizardStep;

/**
 * @author Harald Pehl
 */
public class UploadStep extends
        WizardStep<Context, State> {

    private FormPanel form;
    private FileUpload fileUpload;

    public UploadStep(final DeploymentWizard wizard) {
        super(wizard, "Upload Deployment");
    }

    @Override
    public Widget asWidget() {
        final FlowPanel panel = new FlowPanel();

        HTML description = new HTML(Console.CONSTANTS.common_label_chooseFile());
        description.getElement().setAttribute("style", "padding-bottom:15px;");
        panel.add(description);

        form = new FormPanel();

        // create a panel to hold all of the form widgets.
        VerticalPanel formPanel = new VerticalPanel();
        form.setWidget(formPanel);

        // create a FileUpload widgets.
        fileUpload = new FileUpload();
        fileUpload.setName("uploadFormElement");
        IdHelper.setId(fileUpload, id(), "file");
        formPanel.add(fileUpload);

        panel.add(form);
        return panel;
    }

    @Override
    protected void onShow(final Context context) {
        form.reset();
        context.fileUpload = fileUpload;
    }

    @Override
    protected boolean onNext(final Context context) {
        String filename = fileUpload.getFilename();
        if (Strings.isNullOrEmpty(filename)) {
            wizard.showError(Console.CONSTANTS.pleaseChooseFile());
            return false;
        } else {
            wizard.clearError();
            String name = filename;
            int fakePathIndex = filename.lastIndexOf("\\");
            if (fakePathIndex != -1) {
                name = filename.substring(fakePathIndex + 1, filename.length());
            }
            context.upload.setName(name);
            context.upload.setRuntimeName(name);
            return true;
        }
    }
}
