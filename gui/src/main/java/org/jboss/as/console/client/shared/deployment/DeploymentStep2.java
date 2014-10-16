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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.help.StaticHelpPanel;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

/**
 * @author Harald Pehl
 * @author Heiko Braun
 * @author Stan Silvert <ssilvert@redhat.com> (C) 2011 Red Hat Inc.
 */
public class DeploymentStep2 {

    private NewDeploymentWizard wizard;
    private DefaultWindow window;
    private Form<DeploymentReference> form;

    public DeploymentStep2(NewDeploymentWizard wizard, DefaultWindow window) {
        this.wizard = wizard;
        this.window = window;
    }

    public Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");

        layout.add(new HTML("<h3>" + Console.CONSTANTS.common_label_step() + " 2/2: "
                + Console.CONSTANTS.common_label_verifyDeploymentNames() + "</h3>"));
        layout.add(new StaticHelpPanel(managedHelp()).asWidget());

        form = new Form<DeploymentReference>(DeploymentReference.class);
        TextBoxItem nameField = new TextBoxItem("name", Console.CONSTANTS.common_label_name());
        TextBoxItem runtimeNameField = new TextBoxItem("runtimeName", Console.CONSTANTS.common_label_runtimeName());
        CheckBoxItem enable = new CheckBoxItem("enableAfterDeployment", "Enable");
        if (Console.getBootstrapContext().isStandalone()) {
            form.setFields(nameField, runtimeNameField, enable);
        } else {
            form.setFields(nameField, runtimeNameField);
        }
        layout.add(form.asWidget());

        ClickHandler cancelHandler = new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                window.hide();
            }
        };
        ClickHandler submitHandler = new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                FormValidation validation = form.validate();
                if (!validation.hasErrors()) {
                    // proceed
                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        @Override
                        public void execute() {
                            wizard.upload();
                        }
                    });
                }
            }
        };

        DialogueOptions options = new DialogueOptions(submitHandler, cancelHandler);
        return new WindowContentBuilder(layout, options).build();
    }

    void edit(DeploymentReference ref) {
        form.edit(ref);
    }

    DeploymentReference getDeploymentReference() {
        return form.getUpdatedEntity();
    }

    private SafeHtml managedHelp() {
        // TODO I18n or take from DMR
        SafeHtmlBuilder builder = new SafeHtmlBuilder();
        builder.appendHtmlConstant("<table class='help-attribute-descriptions'>");
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

