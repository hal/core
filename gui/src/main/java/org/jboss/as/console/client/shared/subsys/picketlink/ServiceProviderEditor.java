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
package org.jboss.as.console.client.shared.subsys.picketlink;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.dmr.client.ModelNode;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.Map;

/**
 * @author Harald Pehl
 */
class ServiceProviderEditor implements IsWidget {

    final ServiceProviderPresenter presenter;
    final SecurityContext securityContext;
    final StatementContext statementContext;
    final ResourceDescription resourceDescription;

    ModelNodeFormBuilder.FormAssets formAssets;

    ServiceProviderEditor(final ServiceProviderPresenter presenter,
            final SecurityContext securityContext,
            final StatementContext statementContext,
            final ResourceDescription resourceDescription) {
        this.presenter = presenter;
        this.securityContext = securityContext;
        this.statementContext = statementContext;
        this.resourceDescription = resourceDescription;
    }

    @Override
    public Widget asWidget() {
        OneToOneLayout layoutBuilder = new OneToOneLayout()
                .setPlain(true)
                .setHeadline("Service Provider")
                .setDescription(((UIConstants) GWT.create(UIConstants.class)).serviceProviderDescription())
                .setDetail(Console.CONSTANTS.common_label_attributes(), formPanel());
        return layoutBuilder.build();
    }

    Widget formPanel() {
        formAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext).build();
        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeSet) {
                presenter.modifyServiceProvider(formAssets.getForm().getChangedValues());
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

    void update(ModelNode serviceProvider) {
        formAssets.getForm().edit(serviceProvider);
    }
}
