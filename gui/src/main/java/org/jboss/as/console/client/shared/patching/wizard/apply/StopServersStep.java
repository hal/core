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

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.patching.wizard.PatchWizard;
import org.jboss.as.console.client.shared.patching.wizard.PatchWizardStep;

/**
 * @author Harald Pehl
 */
public class StopServersStep extends PatchWizardStep<ApplyContext, ApplyState> {

    private RadioButton yes;
    private RadioButton no;

    public StopServersStep(final PatchWizard<ApplyContext, ApplyState> wizard) {
        super(wizard, Console.CONSTANTS.patch_manager_stop_server_title());
    }

    @Override
    protected IsWidget body(final ApplyContext context) {
        FlowPanel body = new FlowPanel();
        body.add(new Label(Console.MESSAGES.patch_manager_stop_server_body(context.host)));
        yes = new RadioButton("stop_servers", Console.CONSTANTS.patch_manager_stop_server_yes());
        yes.getElement().setId(asId(PREFIX, getClass(), "_Yes"));
        yes.addStyleName("apply-patch-radio");
        yes.setValue(true);
        no = new RadioButton("stop_servers", Console.CONSTANTS.patch_manager_stop_server_no());
        no.getElement().setId(asId(PREFIX, getClass(), "_No"));
        no.addStyleName("apply-patch-radio");
        body.add(yes);
        body.add(no);
        return body;
    }

    @Override
    protected void onNext(ApplyContext context) {
        context.stopServers = yes.getValue();
        super.onNext(context);
    }
}
