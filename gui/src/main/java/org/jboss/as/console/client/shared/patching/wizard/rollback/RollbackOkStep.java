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

import static org.jboss.as.console.client.shared.util.IdHelper.asId;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.patching.ui.PatchManagementTemplates;
import org.jboss.as.console.client.shared.patching.wizard.PatchWizard;
import org.jboss.as.console.client.shared.patching.wizard.PatchWizardStep;

/**
 * @author Harald Pehl
 */
public class RollbackOkStep extends PatchWizardStep<RollbackContext, RollbackState> {

    static final PatchManagementTemplates TEMPLATES = GWT.create(PatchManagementTemplates.class);
    private final String serverOrHost;
    private RadioButton yes;

    public RollbackOkStep(final PatchWizard<RollbackContext, RollbackState> wizard, String serverOrHost) {
        super(wizard, Console.CONSTANTS.patch_manager_rolled_back_success_title(),
                Console.CONSTANTS.common_label_finish());
        this.serverOrHost = serverOrHost;
    }

    @Override
    protected IsWidget body(final RollbackContext context) {
        FlowPanel body = new FlowPanel();

        body.add(new Label(Console.MESSAGES.patch_manager_restart_needed(serverOrHost)));
        body.add(new HTML(TEMPLATES.successPanel(Console.CONSTANTS.patch_manager_rolled_back_success_body())));

        body.add(new HTML(
                "<h3 class=\"patch-followup-header\">" + Console.CONSTANTS.patch_manager_restart_now() + "</h3>"));

        yes = new RadioButton("restart_host", Console.MESSAGES.patch_manager_restart_yes(serverOrHost));
        yes.getElement().setId(asId(PREFIX, getClass(), "_RestartYes"));
        yes.addStyleName("patch-radio");
        yes.setValue(true);
        RadioButton no = new RadioButton("restart_host", Console.CONSTANTS.patch_manager_restart_no());
        no.getElement().setId(asId(PREFIX, getClass(), "_RestartNo"));
        no.addStyleName("patch-radio");
        body.add(yes);
        body.add(no);

        return body;
    }

    @Override
    protected void onNext(final RollbackContext context) {
        context.restartToUpdate = yes.getValue();
        super.onNext(context);
    }
}
