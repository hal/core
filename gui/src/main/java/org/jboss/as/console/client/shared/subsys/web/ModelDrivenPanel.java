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
package org.jboss.as.console.client.shared.subsys.web;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.dmr.ResourceDefinition;
import org.jboss.as.console.mbui.widgets.ModelDrivenWidget;
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.dmr.client.ModelNode;

import java.util.Map;

/**
 * @author Harald Pehl
 */
class ModelDrivenPanel extends ModelDrivenWidget {

    private final WebPresenter presenter;
    private final String[] attributes;
    private ModelNodeForm form;

    ModelDrivenPanel(String address, WebPresenter presenter, String... attributes) {
        super(address);
        this.presenter = presenter;
        this.attributes = attributes;
    }

    void setData(final ModelNode data) {
        if (form != null) {
            form.edit(data);
        } else {
            Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
                @Override
                public boolean execute() {
                    form.edit(data);
                    return false;
                }
            }, 500);
        }
    }

    @Override
    public Widget buildWidget(final ResourceAddress address, final ResourceDefinition definition) {
        SecurityContext securityContext = Console.MODULES.getSecurityFramework().getSecurityContext(presenter.getProxy().getNameToken());

        ModelNodeFormBuilder builder = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .setSecurityContext(securityContext);
        if (attributes != null && attributes.length != 0) {
            builder.setFields(attributes);
        }
        final ModelNodeFormBuilder.FormAssets formAssets = builder.build();

        form = formAssets.getForm();
        form.setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changedValues) {
                presenter.onSaveResource(address.asString(), null, changedValues);
            }

            @Override
            public void onCancel(Object entity) {
                form.cancel();
            }
        });

        VerticalPanel formPanel = new VerticalPanel();
        formPanel.setStyleName("fill-layout-width");
        formPanel.add(formAssets.getHelp().asWidget());
        formPanel.add(form.asWidget());

        return formPanel;
    }
}
