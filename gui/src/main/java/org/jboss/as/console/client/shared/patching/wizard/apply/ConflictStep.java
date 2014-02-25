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
package org.jboss.as.console.client.shared.patching.wizard.apply;

import static org.jboss.as.console.client.shared.util.IdHelper.asId;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.patching.ui.ErrorDetails;
import org.jboss.as.console.client.shared.patching.wizard.PatchWizard;
import org.jboss.as.console.client.shared.patching.wizard.PatchWizardStep;

/**
 * @author Harald Pehl
 */
public class ConflictStep extends PatchWizardStep<ApplyContext, ApplyState> {

    final static ActionsTemplate ACTIONS_TEMPLATE = GWT.create(ActionsTemplate.class);

    private ErrorDetails errorDetails;

    public ConflictStep(final PatchWizard<ApplyContext, ApplyState> wizard) {
        super(wizard, Console.CONSTANTS.patch_manager_conflict_title());
    }

    @Override
    protected IsWidget header(final ApplyContext context) {
        return new HTML("<h3 class=\"error\"><i class\"icon-exclamation-sign icon-large\"></i> " + title + "</h3>");
    }

    @Override
    protected IsWidget body(final ApplyContext context) {
        FlowPanel body = new FlowPanel();
        body.add(new Label(Console.CONSTANTS.patch_manager_conflict_body()));

        errorDetails = new ErrorDetails(Console.CONSTANTS.patch_manager_show_details(),
                Console.CONSTANTS.patch_manager_hide_details());
        body.add(errorDetails);

        body.add(new HTML("<h3 class=\"apply-patch-followup-header\">" + Console.CONSTANTS
                .patch_manager_possible_actions() + "</h3>"));
        HTMLPanel actions = new HTMLPanel(ACTIONS_TEMPLATE
                .actions(Console.CONSTANTS.patch_manager_conflict_cancel_title(),
                        Console.CONSTANTS.patch_manager_conflict_cancel_body(),
                        Console.MESSAGES.patch_manager_conflict_override_title(),
                        Console.CONSTANTS.patch_manager_conflict_override_body()));
        CheckBox overrideCheck = new CheckBox(Console.CONSTANTS.patch_manager_conflict_override_check());
        overrideCheck.getElement().setId(asId(PREFIX, getClass(), "_Override"));
        overrideCheck.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(final ValueChangeEvent<Boolean> event) {
                setEnabled(event.getValue(), true);
            }
        });
        actions.add(overrideCheck, "apply-patch-conflict-override");
        body.add(actions);

        return body;
    }

    @Override
    protected void onShow(final ApplyContext context) {
        errorDetails.setDetails(context.patchFailedDetails);
    }


    interface ActionsTemplate extends SafeHtmlTemplates {

        @Template("<ul class=\"apply-patch-actions\">" +
                "<li><div class=\"title\">{0}</div><div class=\"body\">{1}</div></li>" +
                "<li><div class=\"title\">{2}</div><div class=\"body\">{3}</div><div id=\"apply-patch-conflict-override\"></div></li>" +
                "</ul>")
        SafeHtml actions(String cancelTitle, String cancelBody, SafeHtml overrideTitle, String overrideBody);
    }
}
