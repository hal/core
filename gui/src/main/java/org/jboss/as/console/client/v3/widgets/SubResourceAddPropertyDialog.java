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
package org.jboss.as.console.client.v3.widgets;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import static org.jboss.dmr.client.ModelDescriptionConstants.NAME;

/**
 * @author Harald Pehl
 */
public class SubResourceAddPropertyDialog extends AddPropertyDialog {

    private final AddResourceDialog dialog;

    public SubResourceAddPropertyDialog(final PropertyManager propertyManager, final SecurityContext securityContext,
            final ResourceDescription resourceDescription) {
        super(Console.CONSTANTS.common_label_add());
        this.dialog = new AddResourceDialog(securityContext, resourceDescription,
                new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        Property property = new Property(payload.get(NAME).asString(), payload);
                        propertyManager.onAdd(property, SubResourceAddPropertyDialog.this);
                    }

                    @Override
                    public void onCancel() {
                        hide();
                    }
                });
        setWidget(dialog);
    }

    @Override
    public void clearValues() {
        dialog.clearValues();
    }
}
