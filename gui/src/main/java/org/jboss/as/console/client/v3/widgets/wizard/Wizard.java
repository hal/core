/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.v3.widgets.wizard;

import com.google.common.collect.Iterables;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.shared.util.IdHelper;
import org.jboss.ballroom.client.widgets.common.DefaultButton;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.TrappedFocusPanel;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * General purpose wizard relying on a context for the common data and an enum representing the states of the different
 * steps.
 * <p>
 * Concrete wizards must inherit from this class and
 * <ol>
 * <li>add steps in the constructor using {@link #addStep(Enum, WizardStep)}</li>
 * <li>provide the initial and last step(s) by overriding {@link #initialState()} and {@link #lastStates()}</li>
 * <li>decide how to move back and forth when {@link #back(Enum)} and {@link #next(Enum)} are called</li>
 * </ol>
 *
 * @param <C> The context
 * @param <S> The state enum
 *
 * @author Harald Pehl
 */
public abstract class Wizard<C, S extends Enum<S>> implements IsWidget {

    interface Template extends SafeHtmlTemplates {

        @Template("<h3>{0}</h3>")
        SafeHtml header(String text);
    }


    private class StateDeckPanel extends DeckPanel {
        public void showWidget(final S state) {
            WizardStep<C, S> step = steps.get(state);
            Integer index = stateIndex.get(state);
            if (step != null && index != null && index >= 0 && index < getWidgetCount()) {
                step.onShow(context);
                showWidget(stateIndex.get(state));
            }
        }
    }


    private class Footer implements IsWidget {

        final FlowPanel panel;
        final DefaultButton cancel;
        final DefaultButton back;
        final DefaultButton next;

        public Footer() {
            cancel = new DefaultButton(CONSTANTS.common_label_cancel());
            cancel.addClickHandler(clickEvent -> onCancel());
            cancel.addStyleName("wizard-cancel");
            IdHelper.setId(cancel, id(), "cancel");

            back = new DefaultButton(CONSTANTS.wizard_back());
            back.addClickHandler(clickEvent -> onBack());
            IdHelper.setId(back, id(), "back");

            next = new DefaultButton(CONSTANTS.common_label_next());
            next.addClickHandler(clickEvent -> onNext());
            next.addStyleName("primary");
            IdHelper.setId(next, id(), "next");

            panel = new FlowPanel();
            panel.addStyleName("wizard-footer");
            panel.add(cancel);
            panel.add(back);
            panel.add(next);
        }

        @Override
        public Widget asWidget() {
            return panel;
        }
    }


    private static final UIConstants CONSTANTS = Console.CONSTANTS;
    private static final Template TEMPLATE = GWT.create(Template.class);

    private final String id;
    protected final C context;
    private final LinkedHashMap<S, WizardStep<C, S>> steps;
    private final Map<S, Integer> stateIndex;
    private S state;

    private DefaultWindow window;
    private HTML header;
    private HTML errorMessages;
    private StateDeckPanel body;
    private Footer footer;

    protected Wizard(final String id, final C context) {
        this.id = id;
        this.context = context;
        this.steps = new LinkedHashMap<>();
        this.stateIndex = new HashMap<>();
    }

    @Override
    public Widget asWidget() {
        assertSteps();
        state = initialState();

        VerticalPanel root = new VerticalPanel();
        root.setStyleName("window-content");

        header = new HTML();
        errorMessages = new HTML();
        errorMessages.setVisible(false);
        errorMessages.setStyleName("error-panel");
        body = new StateDeckPanel();
        footer = new Footer();

        int index = 0;
        for (Map.Entry<S, WizardStep<C, S>> entry : steps.entrySet()) {
            stateIndex.put(entry.getKey(), index);
            body.add(entry.getValue());
            index++;
        }

        root.add(header);
        root.add(errorMessages);
        root.add(body);
        pushState(state);

        return new TrappedFocusPanel(new WindowContentBuilder(root, footer.asWidget()).build());
    }

    protected void addStep(final S state, final WizardStep<C, S> step) {
        steps.put(state, step);
    }


    // ------------------------------------------------------ public API

    /**
     * Opens the wizard and reset the state, context and UI. If you override this method please make sure to call
     * {@code super.open()} <em>before</em> you access or modify the context.
     */
    public void open(String title) {
        assertSteps();
        if (window == null) {
            window = new DefaultWindow(title);
            window.setWidth(520);
            window.setHeight(450);
            window.trapWidget(asWidget());
            window.setGlassEnabled(true);
        } else {
            window.setTitle(title);
        }
        resetContext();
        for (WizardStep<C, S> step : steps.values()) {
            step.reset();
        }
        state = initialState();
        pushState(state);
        window.center();
    }

    public void close() {
        if (window != null) {
            window.hide();
        }
    }

    public void showError(String message) {
        errorMessages.setText(message);
        errorMessages.setVisible(true);
    }

    public void clearError() {
        errorMessages.setHTML("");
        errorMessages.setVisible(false);
    }


    // ------------------------------------------------------ workflow

    private void onCancel() {
        if (currentStep().onCancel(context)) {
            cancel();
        }
    }

    private void onBack() {
        if (currentStep().onBack(context)) {
            final S previousState = back(state);
            if (previousState != null) {
                pushState(previousState);
            }
        }
    }

    private void onNext() {
        if (currentStep().onNext(context)) {
            final S nextState = next(state);
            if (nextState != null) {
                pushState(nextState);
            } else {
                finish();
            }
        }
    }

    /**
     * Sets the current state to the specified state and updates the UI to reflect the current state.
     *
     * @param state the next state
     */
    private void pushState(final S state) {
        this.state = state;

        header.setHTML(TEMPLATE.header(currentStep().getTitle()));
        clearError();
        body.showWidget(state); // will call onShow(C) for the current step
        footer.back.setEnabled(state != initialState());
        footer.next
                .setHTML(
                        lastStates().contains(state) ? CONSTANTS.common_label_finish() : CONSTANTS.common_label_next());
    }

    /**
     * @return the initial state which is the state of the first added step by default.
     */
    protected S initialState() {
        assertSteps();
        return steps.keySet().iterator().next();
    }

    /**
     * @return the last state(s) which is the state of the last added step by default.
     */
    protected EnumSet<S> lastStates() {
        assertSteps();
        return EnumSet.of(Iterables.getLast(steps.keySet()));
    }

    /**
     * Subclasses must provide the previous state or {@code null} if there's no previous state.
     */
    protected abstract S back(final S state);

    /**
     * Subclasses must provide the next state or {@code null} if there's no next state (signals the 'finished' state)
     */
    protected abstract S next(final S state);

    /**
     * Subclasses can override this method to reset the context. This method is called just before the
     * wizard is opened. You don't need to reset the state or the UI though, the {@link #open(String)} method will take
     * care of this.
     */
    protected void resetContext() {

    }

    /**
     * Closes the wizard.
     */
    protected void finish() {
        close();
    }

    /**
     * Closes the wizard.
     */
    protected void cancel() {
        close();
    }


    // ------------------------------------------------------ helper methods

    private WizardStep<C, S> currentStep() {
        assertSteps();
        return steps.get(state);
    }

    /**
     * @return the unique id of this wizard.
     */
    protected String id() {
        return id;
    }

    private void assertSteps() {
        if (steps.isEmpty()) {
            throw new IllegalStateException("No steps found for wizard " + getClass()
                    .getName() + ". Please add steps in the constructor before using this wizard");
        }
    }
}
