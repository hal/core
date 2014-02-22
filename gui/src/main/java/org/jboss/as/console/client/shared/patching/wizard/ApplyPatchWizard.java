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

import com.google.gwt.core.client.GWT;
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
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.patching.PatchInfo;
import org.jboss.as.console.client.shared.patching.PatchManagerPresenter;
import org.jboss.as.console.client.shared.patching.PatchType;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.TrappedFocusPanel;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

/**
 * @author Harald Pehl
 */
public class ApplyPatchWizard implements IsWidget {

    private final PatchManagerPresenter presenter;
    private final LinkedHashMap<State, Step> steps;
    private State state;
    private DeckPanel deck;

    final Context context;

    public ApplyPatchWizard(final PatchManagerPresenter presenter, final Context context) {
        this.presenter = presenter;
        this.context = context;
        this.state = context.runningServers ? STOP_SERVERS : SELECT_PATCH;
        this.steps = new LinkedHashMap<State, Step>();
        this.steps.put(STOP_SERVERS, new StopServersStep(this));
        this.steps.put(STOPPING, new StoppingServersStep(this));
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
                super.showWidget(index);
                if (index >= 0 && index < State.values().length) {
                    State state = State.values()[index];
                    Step step = steps.get(state);
                    if (step != null) {
                        step.onShow(context);
                    }
                }
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
                state = STOPPING;
                deck.showWidget(state.ordinal());
//                steps.get(state).setEnabled(true, false);
                break;
            case STOPPING:
                state = SELECT_PATCH;
                deck.showWidget(state.ordinal());
                break;
            case SELECT_PATCH:
                state = APPLYING;
                deck.showWidget(state.ordinal());
                break;
            case APPLYING:
                if (context.errorMessage == null) {
                    // Test code
                    BeanFactory beanFactory = GWT.create(BeanFactory.class);
                    context.patchInfo = beanFactory.patchInfo().as();
                    context.patchInfo.setId("0815");
                    context.patchInfo.setType(PatchType.CUMULATIVE);
                    context.patchInfo.setVersion("1.234");
                    state = SUCCESS;
                }
                deck.showWidget(state.ordinal());
                break;
            case SUCCESS:
                close();
                break;
            case CONFLICT:
                if (context.overwriteConflict) {
                    state = APPLYING;
                    deck.showWidget(state.ordinal());
                } else {
                    close();
                }
                break;
            case ERROR:
                close();
                break;
        }
    }

    void close() {
        presenter.hideWindow();
    }


    enum State {
        STOP_SERVERS, STOPPING, SELECT_PATCH, APPLYING, SUCCESS, CONFLICT, ERROR
    }


    public static class Context {

        final boolean standalone;
        final String host;
        final boolean runningServers;

        String filename;
        boolean shutdownServers;
        boolean restart;
        PatchInfo patchInfo;
        boolean conflict;
        String errorMessage;
        boolean overwriteConflict;

        public Context(final boolean standalone, final String host, final boolean runningServers) {
            this.standalone = standalone;
            this.host = host;
            this.runningServers = runningServers;
            this.filename = "n/a";
            this.shutdownServers = true;
            this.restart = true;
            this.patchInfo = PatchInfo.NO_PATCH;
            this.conflict = false;
            this.errorMessage = null;
            this.overwriteConflict = false;
        }
    }


    abstract static class Step implements IsWidget {

        private final String title;
        private Widget widget;
        private DialogueOptions dialogOptions;

        final ApplyPatchWizard wizard;

        Step(final ApplyPatchWizard wizard, final String title) {
            this.title = title;
            this.wizard = wizard;
        }

        @Override
        public final Widget asWidget() {
            VerticalPanel layout = new VerticalPanel();
            layout.setStyleName("window-content");
            layout.add(new HTML("<h3>" + title + "</div>"));
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
            dialogOptions = new DialogueOptions(Console.CONSTANTS.common_label_next(), submitHandler,
                    Console.CONSTANTS.common_label_cancel(), cancelHandler);

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

        void setEnabled(boolean cancel, boolean next) {
            DOM.setElementPropertyBoolean((Element) dialogOptions.getCancel(), "disabled", !cancel);
            DOM.setElementPropertyBoolean((Element) dialogOptions.getSubmit(), "disabled", !next);
        }

        abstract IsWidget body();
    }
}
