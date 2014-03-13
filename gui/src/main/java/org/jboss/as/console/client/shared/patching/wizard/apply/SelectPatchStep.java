/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.jboss.as.console.client.shared.patching.wizard.apply;

import static com.google.gwt.user.client.ui.FormPanel.ENCODING_MULTIPART;
import static com.google.gwt.user.client.ui.FormPanel.METHOD_POST;
import static org.jboss.as.console.client.shared.util.IdHelper.asId;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.patching.wizard.PatchWizard;
import org.jboss.as.console.client.shared.patching.wizard.PatchWizardStep;

/**
 * @author Harald Pehl
 */
public class SelectPatchStep extends PatchWizardStep<ApplyContext, ApplyState> {

    private HTML info;
    private FileUpload upload;
    private HTML errorMessages;

    public SelectPatchStep(final PatchWizard<ApplyContext, ApplyState> wizard) {
        super(wizard, Console.CONSTANTS.patch_manager_select_patch_title());
    }

    @Override
    protected IsWidget body(final ApplyContext context) {
        FormPanel form = new FormPanel();
        form.setAction(context.patchUrl);
        form.setEncoding(ENCODING_MULTIPART);
        form.setMethod(METHOD_POST);
        FlowPanel panel = new FlowPanel();
        form.setWidget(panel);
        context.form = form;

        Hidden operation = new Hidden("operation");
        panel.add(operation);
        context.operation = operation;

        panel.add(new Label(Console.CONSTANTS.patch_manager_select_patch_body()));

        if (!context.standalone) {
            info = new HTML("");
            info.getElement().getStyle().setMarginTop(2, Style.Unit.EM);
            panel.add(info);
        }

        FlowPanel uploadPanel = new FlowPanel();
        uploadPanel.getElement().getStyle().setMarginTop(2, Style.Unit.EM);
        InlineLabel uploadLabel = new InlineLabel(Console.CONSTANTS.patch_manager_select_patch_upload());
        uploadLabel.getElement().getStyle().setMarginRight(1, Style.Unit.EM);
        uploadPanel.add(uploadLabel);
        upload = new FileUpload();
        upload.setName("patch_file");
        upload.getElement().setId(asId(PREFIX, getClass(), "_Upload"));
        uploadPanel.add(upload);
        panel.add(uploadPanel);

        errorMessages = new HTML(
                "<i class=\"icon-exclamation-sign\"></i> " + Console.CONSTANTS.patch_manager_select_file());
        errorMessages.addStyleName("error");
        errorMessages.setVisible(false);
        panel.add(errorMessages);

        return form;
    }

    @Override
    protected void onShow(final ApplyContext context) {
        if (!context.standalone) {
            if (context.serversStopped) {
                info.setText("Host: " + context.host + " (" + Console.CONSTANTS.patch_manager_servers_shutdown() + ")");
            } else {
                info.setHTML("Host: " + context.host + " (" + Console.CONSTANTS
                        .patch_manager_servers_still_running_warning() + ")");
            }
            info.getElement().getStyle().setMarginTop(2, Style.Unit.EM);
        }
        errorMessages.setVisible(false);
    }

    @Override
    protected void onNext(ApplyContext context) {
        errorMessages.setVisible(false);
        context.filename = upload.getFilename();
        if (context.filename == null || context.filename.length() == 0) {
            errorMessages.setVisible(true);
        } else {
            super.onNext(context);
        }
    }
}
