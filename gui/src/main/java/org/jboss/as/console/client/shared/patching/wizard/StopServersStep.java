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

import static org.jboss.as.console.client.shared.util.IdHelper.asId;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.RadioButton;
import org.jboss.as.console.client.Console;

/**
 * @author Harald Pehl
 */
public abstract class StopServersStep<C extends CommonPatchContext, S extends Enum<S>> extends PatchWizardStep<C, S> {

    private RadioButton yes;

    protected StopServersStep(final PatchWizard<C, S> wizard) {
        super(wizard, Console.CONSTANTS.patch_manager_stop_server_title());
    }

    @Override
    protected IsWidget body(final C context) {
        FlowPanel body = new FlowPanel();
        body.add(intro(context));
        yes = new RadioButton("stop_servers", Console.CONSTANTS.patch_manager_stop_server_yes());
        yes.getElement().setId(asId(PREFIX, getClass(), "_Yes"));
        yes.addStyleName("patch-radio");
        yes.setValue(true);
        RadioButton no = new RadioButton("stop_servers", Console.CONSTANTS.patch_manager_stop_server_no());
        no.getElement().setId(asId(PREFIX, getClass(), "_No"));
        no.addStyleName("patch-radio");
        body.add(yes);
        body.add(no);
        return body;
    }

    protected abstract IsWidget intro(C context);

    @Override
    protected void onNext(C context) {
        context.stopServers = yes.getValue();
        super.onNext(context);
    }
}
