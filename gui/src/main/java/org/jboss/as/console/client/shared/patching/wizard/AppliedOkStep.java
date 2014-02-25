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

import static org.jboss.as.console.client.shared.patching.PatchType.CUMULATIVE;
import static org.jboss.as.console.client.shared.patching.PatchType.ONE_OFF;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.form.EnumFormItem;
import org.jboss.as.console.client.shared.patching.PatchInfo;
import org.jboss.as.console.client.shared.patching.PatchType;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.TextItem;

/**
 * @author Harald Pehl
 */
public class AppliedOkStep extends WizardStep {

    private final String serverOrHost;
    private RadioButton yes;
    private RadioButton no;
    private Form<PatchInfo> form;

    public AppliedOkStep(final ApplyPatchWizard wizard) {
        super(wizard, Console.CONSTANTS.patch_manager_applied_success_title(), Console.CONSTANTS.common_label_finish());
        this.serverOrHost = wizard.context.standalone ? "server" : "host";
    }

    @Override
    IsWidget header() {
        return new HTML("<h3 class=\"success\"><i class=\"icon-ok icon-large\"></i> " + title + "</h3>");
    }

    @Override
    protected IsWidget body() {
        FlowPanel body = new FlowPanel();

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

        body.add(new HTML("<h3 class=\"apply-patch-followup-header\">" + Console.MESSAGES
                .patch_manager_restart_title(serverOrHost) + "</h3>"));
        body.add(new Label(Console.MESSAGES.patch_manager_applied_restart_body(serverOrHost)));

        yes = new RadioButton("restart_host", Console.MESSAGES.patch_manager_restart_yes(serverOrHost));
        yes.addStyleName("apply-patch-radio");
        yes.setValue(true);
        no = new RadioButton("restart_host", Console.MESSAGES.patch_manager_restart_no(serverOrHost));
        no.addStyleName("apply-patch-radio");
        body.add(yes);
        body.add(no);

        return body;
    }

    @Override
    void onShow(final WizardContext context) {
        if (context.patchInfo != null) {
            form.edit(context.patchInfo);
        }
    }

    @Override
    void onNext() {
        wizard.context.restartToUpdate = yes.getValue();
        super.onNext();
    }
}
