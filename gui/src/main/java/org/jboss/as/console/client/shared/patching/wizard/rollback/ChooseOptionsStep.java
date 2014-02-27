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

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.patching.RollbackOptions;
import org.jboss.as.console.client.shared.patching.wizard.PatchWizard;
import org.jboss.as.console.client.shared.patching.wizard.PatchWizardStep;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;

/**
 * @author Harald Pehl
 */
public class ChooseOptionsStep extends PatchWizardStep<RollbackContext, RollbackState> {

    private CheckBoxItem resetConfiguration;
    private CheckBoxItem overrideAll;

    public ChooseOptionsStep(final PatchWizard<RollbackContext, RollbackState> wizard) {
        super(wizard, Console.CONSTANTS.patch_manager_rollback_options_title());
    }

    @Override
    protected IsWidget body(final RollbackContext context) {
        FlowPanel body = new FlowPanel();
        body.add(new Label(Console.CONSTANTS.patch_manager_rollback_options_body()));
        Label resetConfigurationDesc = new Label(Console.CONSTANTS.patch_manager_rollback_options_reset_configuration_desc());
        resetConfigurationDesc.getElement().getStyle().setMarginTop(1, EM);
        body.add(resetConfigurationDesc);
        Label overrideAllDesc = new Label(Console.CONSTANTS.patch_manager_rollback_options_override_all_desc());
        overrideAllDesc.getElement().getStyle().setMarginTop(1, EM);
        body.add(overrideAllDesc);

        final Form<RollbackOptions> form = new Form<RollbackOptions>(RollbackOptions.class);
        resetConfiguration = new CheckBoxItem("resetConfiguration",
                Console.CONSTANTS.patch_manager_rollback_options_reset_configuration());
        overrideAll = new CheckBoxItem("overrideAll",
                Console.CONSTANTS.patch_manager_rollback_options_override_all());
        form.setFields(resetConfiguration, overrideAll);

        body.add(form);

        return body;
    }

    @Override
    protected void onNext(final RollbackContext context) {
        context.resetConfiguration = resetConfiguration.getValue();
        context.overrideAll = overrideAll.getValue();
        super.onNext(context);
    }
}
