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

import static com.google.gwt.dom.client.Style.Unit.EM;
import static org.jboss.as.console.client.shared.patching.PatchType.CUMULATIVE;
import static org.jboss.as.console.client.shared.patching.PatchType.ONE_OFF;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.form.EnumFormItem;
import org.jboss.as.console.client.shared.patching.PatchInfo;
import org.jboss.as.console.client.shared.patching.PatchType;
import org.jboss.as.console.client.shared.patching.wizard.PatchWizard;
import org.jboss.as.console.client.shared.patching.wizard.PatchWizardStep;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.TextItem;

/**
 * @author Harald Pehl
 */
public class ConfirmRollbackStep extends PatchWizardStep<RollbackContext, RollbackState> {

    private Label resetConfiguration;
    private Label overrideAll;
    private Form<PatchInfo> form;

    public ConfirmRollbackStep(final PatchWizard<RollbackContext, RollbackState> wizard) {
        super(wizard, Console.CONSTANTS.patch_manager_rollback_confirm_title());
    }

    @Override
    protected IsWidget body(final RollbackContext context) {
        FlowPanel body = new FlowPanel();
        Label confirmLabel = new Label(Console.CONSTANTS.patch_manager_rollback_confirm_body());
        confirmLabel.getElement().getStyle().setMarginBottom(1, EM);
        body.add(confirmLabel);

        resetConfiguration = new Label("");
        body.add(resetConfiguration);
        overrideAll = new Label("");
        overrideAll.getElement().getStyle().setMarginBottom(1, EM);
        body.add(overrideAll);

        form = new Form<PatchInfo>(PatchInfo.class);
        form.setEnabled(false);
        TextItem id = new TextItem("id", "ID");
        TextItem date = new TextItem("appliedAt", Console.CONSTANTS.patch_manager_applied_at());
        Map<PatchType, String> values = new HashMap<PatchType, String>();
        values.put(CUMULATIVE, CUMULATIVE.label());
        values.put(ONE_OFF, ONE_OFF.label());
        EnumFormItem<PatchType> type = new EnumFormItem<PatchType>("type", Console.CONSTANTS.common_label_type());
        type.setValues(values);
        form.setFields(id, date, type);
        body.add(form);

        return body;
    }

    @Override
    protected void onShow(final RollbackContext context) {
        if (context.patchInfo != null) {
            form.edit(context.patchInfo);
        }
        resetConfiguration.setText(Console.CONSTANTS
                .patch_manager_rollback_options_reset_configuration() + ": " + (context.resetConfiguration ? "true" : "false"));
        overrideAll.setText(Console.CONSTANTS
                .patch_manager_rollback_options_override_all() + ": " + (context.overrideAll ? "true" : "false"));
    }
}
