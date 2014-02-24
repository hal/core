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

import static org.jboss.as.console.client.shared.patching.wizard.WizardState.*;

import java.util.LinkedHashMap;

import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.patching.PatchManager;
import org.jboss.as.console.client.shared.patching.PatchManagerPresenter;
import org.jboss.ballroom.client.widgets.window.TrappedFocusPanel;
import org.jboss.dmr.client.dispatch.DispatchAsync;

/**
 * @author Harald Pehl
 */
public class ApplyPatchWizard implements IsWidget {

    private final PatchManagerPresenter presenter;
    private final LinkedHashMap<WizardState, WizardStep> steps;
    private WizardState state;
    private DeckPanel deck;

    final WizardContext context;

    public ApplyPatchWizard(final PatchManagerPresenter presenter, final DispatchAsync dispatcher,
            final PatchManager patchManager, final WizardContext context) {

        this.presenter = presenter;
        this.context = context;
        this.state = context.runningServers.isEmpty() ? SELECT_PATCH : STOP_SERVERS;
        this.steps = new LinkedHashMap<WizardState, WizardStep>();
        this.steps.put(STOP_SERVERS, new StopServersStep(this));
        this.steps.put(STOPPING, new StoppingServersStep(this, dispatcher));
        this.steps.put(STOP_FAILED, new StopServersFailedStep(this));
        this.steps.put(SELECT_PATCH, new SelectPatchStep(this));
        this.steps.put(APPLYING, new ApplyingPatchStep(this, patchManager));
        this.steps.put(SUCCESS, new AppliedOkStep(this));
        this.steps.put(CONFLICT, new ConflictStep(this));
        this.steps.put(ERROR, new AppliedFailedStep(this));
    }

    @Override
    public Widget asWidget() {
        deck = new DeckPanel() {
            @Override
            public void showWidget(final int index) {
                if (index >= 0 && index < WizardState.values().length) {
                    WizardState state = WizardState.values()[index];
                    WizardStep step = steps.get(state);
                    if (step != null) {
                        step.onShow(context);
                    }
                }
                super.showWidget(index);
            }
        };
        for (WizardStep step : steps.values()) {
            deck.add(step);
        }
        deck.showWidget(state.ordinal());

        return new TrappedFocusPanel(deck);
    }

    void next() {
        switch (state) {
            case STOP_SERVERS:
                if (context.stopServers) {
                    nextState(STOPPING);
                } else {
                    nextState(SELECT_PATCH);
                }
                break;
            case STOPPING:
                if (context.stopFailed) {
                    nextState(STOP_FAILED);
                } else {
                    nextState(SELECT_PATCH);
                }
                break;
            case STOP_FAILED:
                nextState(SELECT_PATCH);
                break;
            case SELECT_PATCH:
                nextState(APPLYING);
                break;
            case APPLYING:
                if (context.conflict) {
                    nextState(CONFLICT);
                    WizardStep step = steps.get(state);
                    step.setEnabled(false, true);
                } else if (context.patchFailed) {
                    nextState(ERROR);
                } else {
                    nextState(SUCCESS);
                }
                break;
            case SUCCESS:
                if (context.restartToUpdate) {
                    // TODO Restart host / server
                }
                close();
                break;
            case CONFLICT:
                // next == override
                nextState(APPLYING);
                break;
            case ERROR:
                // next == select again
                nextState(SELECT_PATCH);
                break;
        }
    }

    private void nextState(final WizardState state) {
        this.state = state;
        deck.showWidget(state.ordinal());
    }

    void close() {
        presenter.hideWindow();
    }
}
