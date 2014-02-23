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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.IsWidget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.patching.PatchInfo;
import org.jboss.as.console.client.shared.patching.PatchType;

/**
 * @author Harald Pehl
 */
public class ApplyingPatchStep extends ApplyPatchWizard.Step {

    public ApplyingPatchStep(final ApplyPatchWizard wizard) {
        super(wizard, Console.CONSTANTS.patch_manager_applying_patch_title());
    }

    @Override
    protected IsWidget body() {
        return new Pending(Console.CONSTANTS.patch_manager_applying_patch_body());
    }

    @Override
    void onShow(final ApplyPatchWizard.Context context) {
        // reset old state
        context.restartToUpdate = true;
        context.patchInfo = PatchInfo.NO_PATCH;
        context.conflict = false;
        context.patchFailedDetails = null;
        context.overrideConflict = false;

        // TODO Implement patch operation
        // TODO Take conflict.overrideConflict into account
        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                BeanFactory beanFactory = GWT.create(BeanFactory.class);
                PatchInfo appliedPatch = beanFactory.patchInfo().as();
                appliedPatch.setId("0815");
                appliedPatch.setType(PatchType.CUMULATIVE);
                appliedPatch.setVersion("1.234");
                context.patchInfo = appliedPatch;
                wizard.next();
                return false;
            }
        }, 1500);
    }
}
