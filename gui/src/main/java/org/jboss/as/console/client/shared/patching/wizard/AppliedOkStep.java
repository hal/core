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

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import org.jboss.as.console.client.Console;

/**
 * @author Harald Pehl
 */
public class AppliedOkStep extends ApplyPatchWizard.Step {

    private final String serverOrHost;
    private RadioButton yes;
    private RadioButton no;
    private Label patchApplied;

    public AppliedOkStep(final ApplyPatchWizard wizard) {
        super(wizard, Console.CONSTANTS.patch_manager_applied_success_title());
        this.serverOrHost = wizard.context.standalone ? "server" : "host";
    }

    @Override
    protected IsWidget body() {
        FlowPanel body = new FlowPanel();
        patchApplied = new Label("");
        body.add(patchApplied);
        body.add(new HTML("<h3>" + Console.MESSAGES.patch_manager_restart_title(serverOrHost) + "</h3>"));
        body.add(new Label(Console.MESSAGES.patch_manager_applied_restart_body(serverOrHost)));
        yes = new RadioButton("restart_host", Console.MESSAGES.patch_manager_restart_yes(serverOrHost));
        yes.setValue(true);
        no = new RadioButton("restart_host", Console.MESSAGES.patch_manager_restart_no(serverOrHost));
        body.add(yes);
        body.add(no);
        return body;
    }

    @Override
    void onShow(final ApplyPatchWizard.Context context) {
        patchApplied.setText(Console.MESSAGES.patch_manager_applied_success_body(context.patchInfo.getId()));
    }
}
