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
package org.jboss.as.console.client.shared.patching.wizard.rollback;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.patching.PatchManager;
import org.jboss.as.console.client.shared.patching.ui.Pending;
import org.jboss.as.console.client.shared.patching.wizard.PatchWizard;
import org.jboss.as.console.client.shared.patching.wizard.PatchWizardStep;
import org.jboss.as.console.client.shared.patching.wizard.WizardButton;

/**
 * @author Harald Pehl
 */
public class RollingBackStep extends PatchWizardStep<RollbackContext, RollbackState> {

    private final PatchManager patchManager;
    private Pending pending;

    public RollingBackStep(final PatchWizard<RollbackContext, RollbackState> wizard, final PatchManager patchManager) {
        super(wizard, null, new WizardButton(false), new WizardButton(Console.CONSTANTS.common_label_cancel()));
        this.patchManager = patchManager;
    }

    @Override
    protected IsWidget body(final RollbackContext context) {
        FlowPanel body = new FlowPanel();
        pending = new Pending("");
        body.add(pending);
        return body;
    }

    @Override
    protected void onShow(final RollbackContext context) {
        pending.setTitle(Console.MESSAGES.patch_manager_rolling_back_body(context.patchInfo.getId()));
        patchManager.rollback(context.patchInfo, context.resetConfiguration, context.overrideAll, new AsyncCallback<Void>() {
            @Override
            public void onFailure(final Throwable caught) {
                context.rollbackError = true;
                context.rollbackErrorDetails = caught.getMessage();
                wizard.next();
            }

            @Override
            public void onSuccess(final Void result) {
                context.rollbackError = false;
                wizard.next();
            }
        });
    }
}