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
package org.jboss.as.console.client.shared.subsys.batch.ui;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.dmr.ResourceDefinition;
import org.jboss.as.console.mbui.widgets.ModelDrivenWidget;
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.dmr.client.ModelNode;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.Map;

/**
 * @author Harald Pehl
 */
abstract class BatchResourceForm extends ModelDrivenWidget {

    private final SecurityContext securityContext;
    private final String[] fields;
    private ModelNodeFormBuilder.FormAssets formAssets;

    BatchResourceForm(String resourceAddress, StatementContext statementContext, SecurityContext securityContext,
                      String... fields) {
        super(resourceAddress, statementContext);
        this.securityContext = securityContext;
        this.fields = fields;
    }

    @Override
    public Widget buildWidget(ResourceAddress address, ResourceDefinition definition) {
        ModelNodeFormBuilder builder = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .setSecurityContext(securityContext);
        if (fields != null && fields.length != 0) {
            builder = builder.setFields(fields);
        }
        formAssets = builder.build();
        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeSet) {
                BatchResourceForm.this.onSave(formAssets.getForm().getChangedValues());
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

    ModelNodeForm getForm() {
        return formAssets.getForm();
    }

    void update(ModelNode model) {
        if (formAssets != null && formAssets.getForm() != null) {
            formAssets.getForm().edit(model);
        }
    }

    abstract void onSave(Map<String, Object> changedValues);
}
