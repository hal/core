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

import org.jboss.as.console.client.shared.patching.PatchManagerPresenter;
import org.jboss.as.console.client.shared.patching.wizard.PatchWizard;

/**
 * @author Harald Pehl
 */
public class RollbackWizard extends PatchWizard<RollbackContext, RollbackState> {

    protected RollbackWizard(final PatchManagerPresenter presenter, final RollbackContext context) {
        super(presenter, context);

        addStep(CHOOSE_OPTIONS, new ChooseOptionsStep(this));
        addStep(ROLLING_BACK, new RollingBackStep(this));
        addStep(SUCCESS, new RollbackOkStep(this));
        addStep(ERROR, new RollbackFailedStep(this));
    }

    @Override
    protected RollbackState initialState() {
        return CHOOSE_OPTIONS;
    }

    @Override
    public void next() {
        switch (state) {
            case CHOOSE_OPTIONS:
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
                presenter.loadPatches();
                close();
                break;
            case ERROR:
                // next == start again
                pushState(CHOOSE_OPTIONS);
                break;
        }
    }
}
