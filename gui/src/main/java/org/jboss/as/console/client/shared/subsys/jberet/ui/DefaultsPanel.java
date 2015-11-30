/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.shared.subsys.jberet.ui;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.shared.subsys.jberet.store.ModifyDefaults;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.Map;

/**
 * @author Harald Pehl
 */
class DefaultsPanel implements IsWidget {

    private final Dispatcher circuit;
    private final ResourceDescription resourceDescription;
    private final SecurityContext securityContext;
    private ModelNodeFormBuilder.FormAssets formAssets;

    public DefaultsPanel(final Dispatcher circuit, final ResourceDescription resourceDescription,
            final SecurityContext securityContext) {
        this.circuit = circuit;
        this.resourceDescription = resourceDescription;
        this.securityContext = securityContext;
    }

    @Override
    public Widget asWidget() {
        formAssets = new ModelNodeFormBuilder()
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext)
                .setConfigOnly()
                .build();
        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                circuit.dispatch(new ModifyDefaults(changeset));
            }

            @Override
            public void onCancel(final Object entity) {
                formAssets.getForm().cancel();
            }
        });

        OneToOneLayout layout = new OneToOneLayout()
                .setPlain(true)
                .setHeadline("Batch Subsystem")
                .setDescription(Console.CONSTANTS.batchSubsystemDescription())
                .setDetail(null, formAssets.asWidget());
        return layout.build();
    }

    void update(final ModelNode defaults) {
        formAssets.getForm().edit(defaults);
    }
}
