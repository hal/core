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

import static org.jboss.as.console.client.shared.patching.wizard.apply.ApplyState.*;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.patching.PatchManager;
import org.jboss.as.console.client.shared.patching.PatchManagerPresenter;
import org.jboss.as.console.client.shared.patching.wizard.PatchWizard;
import org.jboss.as.console.client.shared.patching.wizard.StopServersFailedStep;
import org.jboss.as.console.client.shared.patching.wizard.StopServersStep;
import org.jboss.as.console.client.shared.patching.wizard.StoppingServersStep;
import org.jboss.dmr.client.dispatch.DispatchAsync;

/**
 * @author Harald Pehl
 */
public class ApplyWizard extends PatchWizard<ApplyContext, ApplyState> {

    public ApplyWizard(final PatchManagerPresenter presenter, final ApplyContext context, final String title,
            final DispatchAsync dispatcher, final PatchManager patchManager) {
        super(presenter, context, title);

        addStep(STOP_SERVERS, new StopServersStep<ApplyContext, ApplyState>(this) {
            @Override
            protected IsWidget intro(ApplyContext context) {
                FlowPanel panel = new FlowPanel();
                panel.add(new Label(Console.MESSAGES.patch_manager_stop_server_body(context.host)));
                panel.add(new HTML("<h3 class=\"patch-followup-header\">" + Console.CONSTANTS
                        .patch_manager_stop_server_question_for_apply() + "</h3>"));
                return panel;
            }
        });
        addStep(STOPPING, new StoppingServersStep<ApplyContext, ApplyState>(this, dispatcher));
        addStep(STOP_FAILED, new StopServersFailedStep<ApplyContext, ApplyState>(this));
        addStep(SELECT_PATCH, new SelectPatchStep(this));
        addStep(APPLYING, new ApplyingStep(this, patchManager));
        addStep(SUCCESS, new AppliedOkStep(this, context.standalone ? "server" : "host"));
        addStep(CONFLICT, new ConflictStep(this));
        addStep(ERROR, new AppliedFailedStep(this));
    }

    @Override
    protected ApplyState initialState() {
        return context.runningServers.isEmpty() ? SELECT_PATCH : STOP_SERVERS;
    }

    public void next() {
        switch (state) {
            case STOP_SERVERS:
                if (context.stopServers) {
                    pushState(STOPPING);
                } else {
                    pushState(SELECT_PATCH);
                }
                break;
            case STOPPING:
                if (context.stopFailed) {
                    pushState(STOP_FAILED);
                } else {
                    pushState(SELECT_PATCH);
                }
                break;
            case STOP_FAILED:
                pushState(SELECT_PATCH);
                break;
            case SELECT_PATCH:
                pushState(APPLYING);
                break;
            case APPLYING:
                if (context.conflict) {
                    pushState(CONFLICT);
                    currentStep().setEnabled(false, true);
                } else if (context.patchFailed) {
                    pushState(ERROR);
                } else {
                    pushState(SUCCESS);
                }
                break;
            case SUCCESS:
                close();
                if (context.restartToUpdate) {
                    presenter.restart();
                } else {
                    presenter.loadPatches();
                }
                break;
            case CONFLICT:
                // next == override
                pushState(APPLYING);
                break;
            case ERROR:
                // next == start over
                pushState(SELECT_PATCH);
                break;
        }
    }
}
