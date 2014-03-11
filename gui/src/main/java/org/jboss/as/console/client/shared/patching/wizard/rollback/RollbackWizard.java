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

import static org.jboss.as.console.client.shared.patching.wizard.rollback.RollbackState.*;

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
public class RollbackWizard extends PatchWizard<RollbackContext, RollbackState> {

    public RollbackWizard(final PatchManagerPresenter presenter, final RollbackContext context, final String title,
            final DispatchAsync dispatcher, PatchManager patchManager) {
        super(presenter, context, title);

        addStep(STOP_SERVERS, new StopServersStep<RollbackContext, RollbackState>(this) {
            @Override
            protected IsWidget intro(RollbackContext context) {
                FlowPanel panel = new FlowPanel();
                panel.add(new Label(Console.MESSAGES.patch_manager_stop_server_body(context.host)));
                panel.add(new HTML("<h3 class=\"patch-followup-header\">" + Console.CONSTANTS
                        .patch_manager_stop_server_question_for_rollback() + "</h3>"));
                return panel;
            }
        });
        addStep(STOPPING, new StoppingServersStep<RollbackContext, RollbackState>(this, dispatcher));
        addStep(STOP_FAILED, new StopServersFailedStep<RollbackContext, RollbackState>(this));
        addStep(CHOOSE_OPTIONS, new ChooseOptionsStep(this));
        addStep(CONFIRM_ROLLBACK, new ConfirmRollbackStep(this));
        addStep(ROLLING_BACK, new RollingBackStep(this, patchManager));
        addStep(SUCCESS, new RollbackOkStep(this, context.standalone ? "server" : "host"));
        addStep(ERROR, new RollbackFailedStep(this));
    }

    @Override
    protected RollbackState initialState() {
        return context.runningServers.isEmpty() ? CHOOSE_OPTIONS : STOP_SERVERS;
    }

    @Override
    public void next() {
        switch (state) {
            case STOP_SERVERS:
                if (context.stopServers) {
                    pushState(STOPPING);
                } else {
                    pushState(CHOOSE_OPTIONS);
                }
                break;
            case STOPPING:
                if (context.stopFailed) {
                    pushState(STOP_FAILED);
                } else {
                    pushState(CHOOSE_OPTIONS);
                }
                break;
            case STOP_FAILED:
                pushState(CHOOSE_OPTIONS);
                break;
            case CHOOSE_OPTIONS:
                pushState(CONFIRM_ROLLBACK);
                break;
            case CONFIRM_ROLLBACK:
                pushState(ROLLING_BACK);
                break;
            case ROLLING_BACK:
                if (context.rollbackError) {
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
            case ERROR:
                // next == start again
                pushState(CHOOSE_OPTIONS);
                break;
        }
    }
}
