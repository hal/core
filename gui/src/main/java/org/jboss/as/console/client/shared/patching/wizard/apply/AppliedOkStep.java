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

import static org.jboss.as.console.client.shared.patching.PatchType.CUMULATIVE;
import static org.jboss.as.console.client.shared.patching.PatchType.ONE_OFF;
import static org.jboss.as.console.client.shared.util.IdHelper.asId;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.form.EnumFormItem;
import org.jboss.as.console.client.shared.patching.PatchInfo;
import org.jboss.as.console.client.shared.patching.PatchType;
import org.jboss.as.console.client.shared.patching.ui.PatchManagementTemplates;
import org.jboss.as.console.client.shared.patching.wizard.PatchWizard;
import org.jboss.as.console.client.shared.patching.wizard.PatchWizardStep;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.TextItem;

/**
 * @author Harald Pehl
 */
public class AppliedOkStep extends PatchWizardStep<ApplyContext, ApplyState> {

    static final PatchManagementTemplates TEMPLATES = GWT.create(PatchManagementTemplates.class);

    private final String serverOrHost;
    private RadioButton yes;
    private Form<PatchInfo> form;

    public AppliedOkStep(final PatchWizard<ApplyContext, ApplyState> wizard, String serverOrHost) {
        super(wizard, Console.CONSTANTS.patch_manager_update(), Console.CONSTANTS.common_label_finish());
        this.serverOrHost = serverOrHost;
    }

    @Override
    protected IsWidget body(final ApplyContext context) {
        FlowPanel body = new FlowPanel();

        body.add(new Label(Console.MESSAGES.patch_manager_restart_needed(serverOrHost)));
        body.add(new HTML(TEMPLATES.successPanel(Console.CONSTANTS.patch_manager_applied_success())));

        form = new Form<PatchInfo>(PatchInfo.class);
        form.setEnabled(false);
        TextItem id = new TextItem("id", "ID");
        TextItem version = new TextItem("version", "Version");
        Map<PatchType, String> values = new HashMap<PatchType, String>();
        values.put(CUMULATIVE, CUMULATIVE.label());
        values.put(ONE_OFF, ONE_OFF.label());
        EnumFormItem<PatchType> type = new EnumFormItem<PatchType>("type", Console.CONSTANTS.common_label_type());
        type.setValues(values);
        form.setFields(id, version, type);
        body.add(form);

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
    protected void onShow(final ApplyContext context) {
        if (context.patchInfo != null) {
            form.edit(context.patchInfo);
        }
    }

    @Override
    protected void onNext(final ApplyContext context) {
        context.restartToUpdate = yes.getValue();
        super.onNext(context);
    }
}
