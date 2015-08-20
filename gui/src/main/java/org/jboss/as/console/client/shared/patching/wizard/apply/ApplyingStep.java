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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.patching.PatchManager;
import org.jboss.as.console.client.shared.patching.Patches;
import org.jboss.as.console.client.shared.patching.ui.Pending;
import org.jboss.as.console.client.shared.patching.wizard.PatchWizard;
import org.jboss.as.console.client.shared.patching.wizard.PatchWizardStep;
import org.jboss.as.console.client.shared.patching.wizard.WizardButton;
import org.jboss.as.console.client.widgets.forms.UploadForm;
import org.jboss.dmr.client.ModelNode;

import static org.jboss.dmr.client.ModelDescriptionConstants.OP;
import static org.jboss.dmr.client.ModelDescriptionConstants.OUTCOME;
import static org.jboss.dmr.client.ModelDescriptionConstants.SUCCESS;

/**
 * @author Harald Pehl
 */
public class ApplyingStep extends PatchWizardStep<ApplyContext, ApplyState> {

    private final PatchManager patchManager;
    private Pending pending;
    private PatchAppliedHandler patchAppliedHandler;

    public ApplyingStep(final PatchWizard<ApplyContext, ApplyState> wizard, PatchManager patchManager) {
        super(wizard, null, new WizardButton(false), new WizardButton(Console.CONSTANTS.common_label_cancel()));
        this.patchManager = patchManager;
    }

    @Override
    protected IsWidget body(final ApplyContext context) {
        FlowPanel body = new FlowPanel();
        pending = new Pending("");
        body.add(pending);
        return body;
    }

    @Override
    protected void onShow(final ApplyContext context) {
        pending.setTitle(Console.MESSAGES.patch_manager_applying_patch_body(context.filename));

        ModelNode patchOp = context.patchAddress.clone();
        patchOp.get(OP).set("patch");
        patchOp.get("content").add().get("input-stream-index").set(0);
        if (context.overrideConflict) {
            patchOp.get("override-all").set(true);
        }
        context.form.setOperation(patchOp);

        // only one handler please!
        if (patchAppliedHandler == null) {
            patchAppliedHandler = new PatchAppliedHandler();
            context.form.onUploadComplete(patchAppliedHandler);
            context.form.onUploadFailed(error -> {
                context.patchFailed = true;
                context.patchFailedDetails = Console.MESSAGES.patch_manager_error_parse_result(error, "n/a");
                wizard.next();
            });
        }
        patchAppliedHandler.context = context;
        context.form.submit();

        // reset old state
        context.restartToUpdate = true;
        context.patchInfo = null;
        context.conflict = false;
        context.patchFailed = false;
        context.patchFailedDetails = null;
        context.overrideConflict = false;
    }


    class PatchAppliedHandler implements UploadForm.UploadCompleteCallback {

        ApplyContext context;

        @Override
        public void onUploadComplete(final String payload) {
            try {
                JSONObject response = JSONParser.parseLenient(payload).isObject();
                JSONString outcome = response.get(OUTCOME).isString();
                if (outcome != null && SUCCESS.equalsIgnoreCase(outcome.stringValue())) {
                    patchManager.getPatchOfHost(context.host, new SimpleCallback<Patches>() {
                        @Override
                        public void onSuccess(final Patches result) {
                            context.patchInfo = result.getLatest();
                            wizard.next();
                        }
                    });
                } else {
                    context.patchFailedDetails = stringify(response.getJavaScriptObject(), 2);
                    // TODO conflict detection could be improved!?
                    if (context.patchFailedDetails.contains("conflicts")) {
                        context.conflict = true;
                    } else {
                        context.patchFailed = true;
                    }
                    wizard.next();
                }
            } catch (Throwable t) {
                context.patchFailed = true;
                context.patchFailedDetails = Console.MESSAGES.patch_manager_error_parse_result(t.getMessage(), payload);
                wizard.next();
            }
        }

        private native String stringify(JavaScriptObject json, int indent) /*-{
            return JSON.stringify(json, null, indent);
        }-*/;
    }
}
