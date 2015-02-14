/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.console.client.shared.deployment;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.*;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.shared.deployment.model.DeploymentRecord;
import org.jboss.as.console.client.shared.help.StaticHelpPanel;
import org.jboss.as.console.client.widgets.forms.UploadForm;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.TextAreaItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

/**
 * @author Harald Pehl
 * @author Heiko Braun
 * @author Stan Silvert <ssilvert@redhat.com> (C) 2011 Red Hat Inc.
 */
public class DeploymentStep1 implements IsWidget {

    private final NewDeploymentWizard wizard;
    private final DefaultWindow window;

    private UploadForm managedForm;
    private Form<DeploymentRecord> unmanagedForm;
    private FileUpload fileUpload;

    public DeploymentStep1(NewDeploymentWizard wizard, DefaultWindow window) {
        this.wizard = wizard;
        this.window = window;
    }

    @Override
    public Widget asWidget() {
        final TabPanel tabs = new TabPanel();
        tabs.setStyleName("default-tabpanel");

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");

        String stepText = "<h3>" + Console.CONSTANTS.common_label_step() + "1/2: " +
                Console.CONSTANTS.common_label_deploymentSelection() + "</h3>";
        layout.add(new HTML(stepText));
        HTML description = new HTML();
        description.setHTML(Console.CONSTANTS.common_label_chooseFile());
        description.getElement().setAttribute("style", "padding-bottom:15px;");
        layout.add(description);

        // point the managed form to the upload endpoint
        managedForm = new UploadForm();
        managedForm.setAction(Console.getBootstrapContext().getProperty(BootstrapContext.DEPLOYMENT_API));
        managedForm.setEncoding(FormPanel.ENCODING_MULTIPART);
        managedForm.setMethod(FormPanel.METHOD_POST);

        // create a panel to hold all of the form widgets.
        VerticalPanel formPanel = new VerticalPanel();
        formPanel.getElement().setAttribute("style", "width:100%");
        managedForm.setWidget(formPanel);

        // create a FileUpload widgets.
        fileUpload = new FileUpload();
        fileUpload.setName("uploadFormElement");
        formPanel.add(fileUpload);

        final HTML errorMessages = new HTML("Please chose a file!");
        errorMessages.setStyleName("error-panel");
        errorMessages.setVisible(false);
        formPanel.add(errorMessages);

        layout.add(managedForm);
        tabs.add(layout, "Managed");

        // Unmanaged form only for new deployments
        if (!wizard.isUpdate()) {
            VerticalPanel unmanagedPanel = new VerticalPanel();
            unmanagedPanel.setStyleName("window-content");

            String unmanagedText = "<h3>" + Console.CONSTANTS.common_label_step() + "1/1: Specify Deployment</h3>";
            unmanagedPanel.add(new HTML(unmanagedText));
            unmanagedPanel.add(new StaticHelpPanel(unmanagedHelp()).asWidget());

            unmanagedForm = new Form<>(DeploymentRecord.class);
            TextAreaItem path = new TextAreaItem("path", "Path");
            TextBoxItem relativeTo = new TextBoxItem("relativeTo", "Relative To", false);

            TextBoxItem name = new TextBoxItem("name", "Name");
            TextBoxItem runtimeName = new TextBoxItem("runtimeName", "Runtime Name") {
                @Override
                public void setFiltered(boolean filtered) {
                    // ignore
                }
            };
            CheckBoxItem archive = new CheckBoxItem("archive", "Is Archive?");
            archive.setValue(true);
            CheckBoxItem enabled = new CheckBoxItem("enabled", "Enable");
            if (Console.getBootstrapContext().isStandalone()) {
                unmanagedForm.setFields(path, relativeTo, archive, name, runtimeName, enabled);
            } else {
                unmanagedForm.setFields(path, relativeTo, archive, name, runtimeName);
            }
            unmanagedPanel.add(unmanagedForm.asWidget());
            tabs.add(unmanagedPanel, "Unmanaged");
        }
        tabs.selectTab(0);

        ClickHandler cancelHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                window.hide();
            }
        };
        ClickHandler submitHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                errorMessages.setVisible(false);

                // managed deployment
                if (tabs.getTabBar().getSelectedTab() == 0) {
                    String filename = fileUpload.getFilename();
                    if (filename != null && !filename.equals("")) {
                        wizard.createManagedDeployment(filename);
                    } else {
                        errorMessages.setVisible(true);
                    }

                    // unmanaged deployment
                } else {
                    if (!unmanagedForm.validate().hasErrors()) {
                        wizard.createUnmanaged(unmanagedForm.getUpdatedEntity());
                    } else {
                        errorMessages.setVisible(true);
                    }
                }
            }
        };
        DialogueOptions options = new DialogueOptions(
                Console.CONSTANTS.common_label_next(), submitHandler,
                Console.CONSTANTS.common_label_cancel(), cancelHandler);

        return new WindowContentBuilder(tabs, options).build();
    }

    UploadForm getManagedForm() {
        return managedForm;
    }

    public FileUpload getFileUpload() {
        return fileUpload;
    }

    private SafeHtml unmanagedHelp() {
        // TODO I18n or take from DMR
        SafeHtmlBuilder builder = new SafeHtmlBuilder();
        builder.appendHtmlConstant("<table class='help-attribute-descriptions'>");
        addHelpTextRow(builder, "Path:", "Path (relative or absolute) to unmanaged content that is part of the deployment.");
        addHelpTextRow(builder, "Relative To:", "Name of a system path to which the value of the 'path' is relative. If not set, the 'path' is considered to be absolute.");
        addHelpTextRow(builder, "Is Archive?:", "Flag indicating whether unmanaged content is a zip archive (true) or exploded (false).");
        addHelpTextRow(builder, "Name:", "Unique identifier of the deployment. Must be unique across all deployments.");
        addHelpTextRow(builder, "Runtime Name:", "Name by which the deployment should be known within a server's runtime. This would be equivalent to the file name of a deployment file, and would form the basis for such things as default Java Enterprise Edition application and module names. This would typically be the same as 'name', but in some cases users may wish to have two deployments with the same 'runtime-name' (e.g. two versions of \\\"foo.war\\\") both available in the deployment content repository, in which case the deployments would need to have distinct 'name' values but would have the same 'runtime-name'.");
        if (Console.getBootstrapContext().isStandalone()) {
            addHelpTextRow(builder, "Enable:", "Boolean indicating whether the deployment should be enabled after deployment.");
        }
        return builder.toSafeHtml();
    }

    private void addHelpTextRow(SafeHtmlBuilder builder, String name, String desc) {
        builder.appendHtmlConstant("<tr class='help-field-row'>");
        builder.appendHtmlConstant("<td class='help-field-name'>");
        builder.appendEscaped(name);
        builder.appendHtmlConstant("</td>");
        builder.appendHtmlConstant("<td class='help-field-desc'>");
        builder.appendEscaped(desc);
        builder.appendHtmlConstant("</td>");
        builder.appendHtmlConstant("</tr>");
    }
}
