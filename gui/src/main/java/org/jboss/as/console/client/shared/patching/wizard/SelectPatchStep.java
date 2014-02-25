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
package org.jboss.as.console.client.shared.patching.wizard;

import static com.google.gwt.user.client.ui.FormPanel.ENCODING_MULTIPART;
import static com.google.gwt.user.client.ui.FormPanel.METHOD_POST;
import static org.jboss.as.console.client.shared.util.IdHelper.asId;
import static org.jboss.dmr.client.ModelDescriptionConstants.OP;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import org.jboss.as.console.client.Console;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Harald Pehl
 */
public class SelectPatchStep extends WizardStep {

    private Hidden operation;
    private HTML errorMessages;
    private FileUpload upload;

    public SelectPatchStep(final ApplyPatchWizard wizard) {
        super(wizard, Console.CONSTANTS.patch_manager_select_patch_title());
    }

    @Override
    protected IsWidget body() {
        FormPanel form = new FormPanel();
        form.setAction(wizard.context.patchUrl);
        form.setEncoding(ENCODING_MULTIPART);
        form.setMethod(METHOD_POST);
        FlowPanel panel = new FlowPanel();
        form.setWidget(panel);
        wizard.context.form = form;

        operation = new Hidden("operation");
        panel.add(operation);

        Label label = new Label(Console.CONSTANTS.patch_manager_select_patch_body());
        label.getElement().getStyle().setMarginBottom(1, Style.Unit.EM);
        panel.add(label);

        upload = new FileUpload();
        upload.setName("patch_file");
        upload.getElement().setId(asId(PREFIX, getClass(), "_Upload"));
        panel.add(upload);

        errorMessages = new HTML("<i class=\"icon-exclamation-sign\"></i> " + Console.CONSTANTS.patch_manager_select_file());
        errorMessages.addStyleName("error");
        errorMessages.setVisible(false);
        panel.add(errorMessages);

        return form;
    }

    @Override
    void onShow(final WizardContext context) {
        errorMessages.setVisible(false);
        ModelNode patchOp = wizard.context.patchAddress.clone();
        patchOp.get(OP).set("patch");
        patchOp.get("content").add().get("input-stream-index").set(0);
        if (context.overrideConflict) {
            patchOp.get("override-all").set(true);
        }
        operation.setValue(patchOp.toJSONString());
    }

    @Override
    void onNext() {
        errorMessages.setVisible(false);
        String filename = upload.getFilename();
        if (filename == null || filename.length() == 0) {
            errorMessages.setVisible(true);
        } else {
            super.onNext();
            wizard.context.form.submit();
        }
    }
}
