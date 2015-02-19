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
package org.jboss.as.console.client.shared.subsys.remoting.ui;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.Map;

/**
 * @author Harald Pehl
 */
abstract class SaslSingletonEditor implements IsWidget {

    private final SecurityContext securityContext;
    private final ResourceDescription resourceDescription;
    private String connectorName;

    protected final Dispatcher circuit;
    protected final AddressTemplate connectorAddress;
    protected ModelNodeFormBuilder.FormAssets formAssets;

    public SaslSingletonEditor(Dispatcher circuit, SecurityContext securityContext,
                               AddressTemplate connectorAddress, ResourceDescription resourceDescription) {
        this.circuit = circuit;
        this.securityContext = securityContext;
        this.connectorAddress = connectorAddress;
        this.resourceDescription = resourceDescription;

    }

    @Override
    public Widget asWidget() {
        formAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext).build();
        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(Map changeSet) {
                if (connectorName != null) {
                    SaslSingletonEditor.this.onSave(connectorName, changeSet);
                }
            }

            @Override
            public void onCancel(Object entity) {
                formAssets.getForm().cancel();
            }
        });

        VerticalPanel formPanel = new VerticalPanel();
        formPanel.setStyleName("fill-layout-width");
        formPanel.add(formAssets.getHelp().asWidget());
        formPanel.add(formAssets.getForm().asWidget());

        return formPanel;
    }

    protected abstract void onSave(String connectorName, Map<String, Object> changeSet);

    final void update(Property parentConnector) {
        connectorName = parentConnector.getName();
        onUpdate(parentConnector);
    }

    protected abstract void onUpdate(Property parentConnector);

    public void clearDetail() {
        formAssets.getForm().clearValues();
    }
}
