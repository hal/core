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

import static org.jboss.as.console.client.shared.patching.wizard.ApplyPatchWizard.State.*;

import java.util.LinkedHashMap;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.patching.PatchInfo;
import org.jboss.as.console.client.shared.patching.PatchManagerPresenter;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.TrappedFocusPanel;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;
import org.jboss.dmr.client.dispatch.DispatchAsync;

/**
 * @author Harald Pehl
 */
public class ApplyPatchWizard implements IsWidget {

    private final PatchManagerPresenter presenter;
    private final DispatchAsync dispatcher;
    private final LinkedHashMap<State, Step> steps;
    private State state;
    private DeckPanel deck;

    final Context context;

    public ApplyPatchWizard(final PatchManagerPresenter presenter, final DispatchAsync dispatcher,
            final Context context) {

        this.presenter = presenter;
        this.dispatcher = dispatcher;
        this.context = context;
        this.state = context.runningServers.isEmpty() ? SELECT_PATCH : STOP_SERVERS;
        this.steps = new LinkedHashMap<State, Step>();
        this.steps.put(STOP_SERVERS, new StopServersStep(this));
        this.steps.put(STOPPING, new StoppingServersStep(this, dispatcher));
        this.steps.put(STOP_FAILED, new StopServersFailedStep(this));
        this.steps.put(SELECT_PATCH, new SelectPatchStep(this));
        this.steps.put(APPLYING, new ApplyingPatchStep(this));
        this.steps.put(SUCCESS, new AppliedOkStep(this));
        this.steps.put(CONFLICT, new ConflictStep(this));
        this.steps.put(ERROR, new AppliedFailedStep(this));
    }

    @Override
    public Widget asWidget() {
        deck = new DeckPanel() {
            @Override
            public void showWidget(final int index) {
                if (index >= 0 && index < State.values().length) {
                    State state = State.values()[index];
                    Step step = steps.get(state);
                    if (step != null) {
                        step.onShow(context);
                    }
                }
                super.showWidget(index);
            }
        };
        for (Step step : steps.values()) {
            deck.add(step);
        }
        deck.showWidget(state.ordinal());

        return new TrappedFocusPanel(deck);
    }

    void next() {
        switch (state) {
            case STOP_SERVERS:
                if (context.stopServers) {
                    nextState(STOPPING, false, true);
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
                nextState(APPLYING, false, true);
                break;
            case APPLYING:
                if (context.conflict) {
                    nextState(CONFLICT, false, true);
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

    private void nextState(final State state) {
        this.state = state;
        deck.showWidget(state.ordinal());
    }

    private void nextState(final State state, final boolean submitEnabled, final boolean cancelEnabled) {
        nextState(state);
        steps.get(state).setEnabled(submitEnabled, cancelEnabled);
    }

    void close() {
        presenter.hideWindow();
    }


    enum State {
        STOP_SERVERS, STOPPING, STOP_FAILED, SELECT_PATCH, APPLYING, SUCCESS, CONFLICT, ERROR
    }


    public static class Context {

        final boolean standalone;
        final String host;
        final List<String> runningServers;

        String filename;
        boolean stopServers;
        boolean stopFailed;
        String stopError;
        String stopErrorDetails;
        boolean restartToUpdate;
        PatchInfo patchInfo;
        boolean conflict;
        boolean patchFailed;
        String patchFailedDetails;
        boolean overrideConflict;

        public Context(final boolean standalone, final String host, final List<String> runningServers) {
            this.standalone = standalone;
            this.host = host;
            this.runningServers = runningServers;

            this.filename = "n/a";
            this.stopServers = true;
            this.stopFailed = false;
            this.stopError = null;
            this.stopErrorDetails = null;
            this.restartToUpdate = true;
            this.patchInfo = PatchInfo.NO_PATCH;
            this.conflict = false;
            this.patchFailed = false;
            this.patchFailedDetails = null;
            this.overrideConflict = false;
        }
    }


    abstract static class Step implements IsWidget {

        final ApplyPatchWizard wizard;
        final String title;
        final String submitText;
        final String cancelText;

        private Widget widget;
        private DialogueOptions dialogOptions;

        Step(final ApplyPatchWizard wizard, final String title) {
            this(wizard, title, Console.CONSTANTS.common_label_next(), Console.CONSTANTS.common_label_cancel());
        }

        Step(final ApplyPatchWizard wizard, final String title, String submitText) {
            this(wizard, title, submitText, Console.CONSTANTS.common_label_cancel());
        }

        Step(final ApplyPatchWizard wizard, final String title, String submitText, String cancelText) {
            this.wizard = wizard;
            this.title = title;
            this.submitText = submitText;
            this.cancelText = cancelText;
        }

        @Override
        public final Widget asWidget() {
            VerticalPanel layout = new VerticalPanel();
            layout.setStyleName("window-content");
            layout.addStyleName("apply-patch-wizard");

            layout.add(header());
            layout.add(body());

            ClickHandler submitHandler = new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    onNext();
                }
            };
            ClickHandler cancelHandler = new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    onCancel();
                }
            };
            dialogOptions = new DialogueOptions(submitText, submitHandler, cancelText, cancelHandler);

            widget = new WindowContentBuilder(layout, dialogOptions).build();
            return widget;
        }

        void onShow(final Context context) {}

        void onNext() {
            wizard.next();
        }

        void onCancel() {
            wizard.close();
        }

        void setEnabled(boolean submitEnabled, boolean cancelEnabled) {
            DOM.setElementPropertyBoolean((Element) dialogOptions.getSubmit(), "disabled", !submitEnabled);
            DOM.setElementPropertyBoolean((Element) dialogOptions.getCancel(), "disabled", !cancelEnabled);
        }

        IsWidget header() {
            return new HTML("<h3>" + title + "</h3>");
        }

        abstract IsWidget body();
    }
}
