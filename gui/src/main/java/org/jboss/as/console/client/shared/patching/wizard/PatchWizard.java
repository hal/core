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

import static org.jboss.as.console.client.shared.util.IdHelper.asId;

import java.util.Iterator;
import java.util.LinkedHashMap;

import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.patching.PatchManagerElementId;
import org.jboss.as.console.client.shared.patching.PatchManagerPresenter;
import org.jboss.ballroom.client.widgets.window.TrappedFocusPanel;

/**
 * @param <C> The context
 * @param <S> The state enum
 *
 * @author Harald Pehl
 */
public abstract class PatchWizard<C, S extends Enum<S>> implements IsWidget, PatchManagerElementId {

    protected final PatchManagerPresenter presenter;
    protected final C context;
    protected final String title;
    private final LinkedHashMap<S, PatchWizardStep<C, S>> steps;
    protected S state;
    private DeckPanel deck;

    protected PatchWizard(final PatchManagerPresenter presenter, final C context, final String title) {
        this.presenter = presenter;
        this.context = context;
        this.title = title;
        this.steps = new LinkedHashMap<S, PatchWizardStep<C, S>>();
        this.state = initialState();
    }

    protected abstract S initialState();

    @Override
    public Widget asWidget() {
        deck = new DeckPanel() {
            @Override
            public void showWidget(final int index) {
                shrink(); // some steps might have expanded the window
                S state = lookupState(index);
                if (state != null) {
                    PatchWizardStep<C, S> step = steps.get(state);
                    if (step != null) {
                        step.onShow(context);
                    }
                }
                super.showWidget(index);
            }
        };
        for (PatchWizardStep<C, S> step : steps.values()) {
            deck.add(step);
        }
        deck.showWidget(state.ordinal());

        TrappedFocusPanel trap = new TrappedFocusPanel(deck);
        trap.getElement().setId(asId(PREFIX, getClass()));
        return trap;
    }

    private S lookupState(final int index) {
        int counter = 0;
        for (Iterator<S> iterator = steps.keySet().iterator(); iterator.hasNext(); counter++) {
            S s = iterator.next();
            if (counter == index) {
                return s;
            }
        }
        return null;
    }

    protected void addStep(final S state, final PatchWizardStep<C, S> step) {
        steps.put(state, step);

    }

    public abstract void next();

    protected void pushState(final S state) {
        this.state = state;
        deck.showWidget(state.ordinal());
    }

    protected PatchWizardStep<C, S> currentStep() {
        return steps.get(state);
    }

    public void close() {
        presenter.hideWindow();
    }

    public void grow() {
        presenter.biggerWindow();
    }

    public void shrink() {
        presenter.normalWindow();
    }
}
