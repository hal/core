/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

package org.jboss.as.console.client.shared.jvm;

import static org.jboss.dmr.client.ModelDescriptionConstants.DEFAULT;
import static org.jboss.dmr.client.ModelDescriptionConstants.NAME;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.Map;



/**
 * @author Heiko Braun
 * @date 4/20/11
 */
public class JvmEditor {

    private JvmManagement presenter;

    private ModelNodeFormBuilder.FormAssets formAssets;
    private ResourceDescription resourceDescription;
    private SecurityContext securityContext;

    private boolean enableClearButton = false;
    private String reference;
    private Property editedEntity;
    private String resourceAddress;

    private ToolButton clearBtn;
    private VerticalPanel rootPanel;
    private ToolStrip toolStrip;

    public JvmEditor(JvmManagement presenter, ResourceDescription resourceDescription, SecurityContext securityContext,
                     String resourceAddress) {
        this.presenter = presenter;
        this.resourceDescription = resourceDescription;
        this.securityContext = securityContext;
        this.resourceAddress = resourceAddress;
    }

    public Widget asWidget() {
        rootPanel = new VerticalPanel();
        rootPanel.setStyleName("fill-layout-width");

        formAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext)
                .setAddress(resourceAddress)
                .build();
        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeset) {
                onSaveJvm();
            }

            @Override
            public void onCancel(Object entity) {
            }
        });

        toolStrip = new ToolStrip(resourceAddress);

        clearBtn = new ToolButton(Console.CONSTANTS.common_label_clear(), event -> Feedback.confirm(
                Console.MESSAGES.deleteTitle("JVM Configuration"),
                Console.MESSAGES.deleteConfirm("JVM Configuration"),
                isConfirmed -> {
                    if (isConfirmed) {
                        presenter.onDeleteJvm(reference, editedEntity.getName());
                    }
                }));

        if(enableClearButton)
            toolStrip.addToolButtonRight(clearBtn);

        rootPanel.add(toolStrip.asWidget());

        // ---

        Widget formWidget = formAssets.asWidget();
        rootPanel.add(formWidget);

        return rootPanel;
    }

    private void onSaveJvm() {

        FormValidation validation = formAssets.getForm().validate();
        if(!validation.hasErrors())
        {
            formAssets.getForm().setEnabled(false);
            ModelNode jvm = formAssets.getForm().getUpdatedEntity();

            if(editedEntity != null) {
                presenter.onUpdateJvm(reference, editedEntity.getName(), formAssets.getForm().getChangedValues());
            } else {
                jvm.get(NAME).set(DEFAULT);
                presenter.onCreateJvm(reference, jvm);
            }
        }
    }

    public void setSelectedRecord(String reference, Property jvm) {
        this.reference = reference;
        this.editedEntity = jvm;

        boolean hasJvm = jvm != null;

        clearBtn.setVisible(hasJvm);

        if(hasJvm) {
            formAssets.getForm().edit(jvm.getValue());
        } else {
            formAssets.getForm().edit(new ModelNode());
        }
    }

    public void clearValues() {
        formAssets.getForm().clearValues();
    }

    public void setSecurityContextFilter(final String resourceAddress) {
        if (formAssets != null && formAssets.getForm() != null) {
            formAssets.getForm().setSecurityContextFilter(resourceAddress);
        }
    }

    public void setEnableClearButton(boolean enable) {
        this.enableClearButton = enable;
    }

}
