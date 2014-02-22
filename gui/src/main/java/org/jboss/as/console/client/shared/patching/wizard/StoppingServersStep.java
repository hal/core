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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import org.jboss.as.console.client.Console;

/**
 * @author Harald Pehl
 */
public class StoppingServersStep extends ApplyPatchWizard.Step {

    public StoppingServersStep(final ApplyPatchWizard wizard) {
        super(wizard, Console.CONSTANTS.patch_manager_stop_server_title());
    }

    @Override
    protected IsWidget body() {
        return new HTMLPanel(
                "<center><div><img src='images/loading_lite.gif' style='padding-top:3px;vertical-align:middle'/>Stopping servers...</div></center>");
    }

    @Override
    void onShow(final ApplyPatchWizard.Context context) {
        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                onNext();
                return true;
            }
        }, 1500);
    }
}
