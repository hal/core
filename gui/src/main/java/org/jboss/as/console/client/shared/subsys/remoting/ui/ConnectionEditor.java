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

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.subsys.remoting.store.CreateConnection;
import org.jboss.as.console.client.shared.subsys.remoting.store.DeleteConnection;
import org.jboss.as.console.client.shared.subsys.remoting.store.RemotingStore;
import org.jboss.as.console.client.shared.subsys.remoting.store.UpdateConnection;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.circuit.Dispatcher;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.dmr.client.ModelDescriptionConstants.NAME;

/**
 * Editor for {@link RemotingStore#LOCAL_OUTBOUND_CONNECTION}, {@link RemotingStore#OUTBOUND_CONNECTION} and
 * {@link RemotingStore#REMOTE_OUTBOUND_CONNECTION}.
 *
 * @author Harald Pehl
 */
class ConnectionEditor extends RemotingEditor {

    private final Dispatcher circuit;
    private final AddressTemplate addressTemplate;
    private final String title;

    ConnectionEditor(DispatchAsync dispatcher, Dispatcher circuit, SecurityContext securityContext,
                     StatementContext statementContext, AddressTemplate addressTemplate,
                     ResourceDescription resourceDescription, String title) {
        super(dispatcher, securityContext, statementContext, addressTemplate, resourceDescription);
        this.circuit = circuit;
        this.addressTemplate = addressTemplate;
        this.title = title;
    }

    @Override
    public Widget asWidget() {
        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline(title + "s")
                .setDescription(SafeHtmlUtils.fromString(resourceDescription.get(DESCRIPTION).asString()))
                .setMasterTools(tools())
                .setMaster(Console.MESSAGES.available(title), table())
                .addDetail(Console.CONSTANTS.common_label_attributes(), formPanel())
                .addDetail(Console.CONSTANTS.properties_global_desc(), propertyEditor().asWidget());
        return layoutBuilder.build();
    }

    @Override
    protected void onAdd() {
        final DefaultWindow dialog = new DefaultWindow(title);
        AddResourceDialog addDialog = new AddResourceDialog(securityContext, resourceDescription, new AddResourceDialog.Callback() {
            @Override
            public void onAdd(ModelNode payload) {
                // The instance name must be part of the model node!
                String instanceName = payload.get(NAME).asString();
                circuit.dispatch(new CreateConnection(addressTemplate, instanceName, payload));
                dialog.hide();
            }

            @Override
            public void onCancel() {
                dialog.hide();
            }
        });
        dialog.setWidth(480);
        dialog.setHeight(360);
        dialog.setWidget(addDialog);
        dialog.setGlassEnabled(true);
        dialog.center();
    }

    @Override
    protected void onRemove(final String name) {
        Feedback.confirm(Console.MESSAGES.deleteTitle(title),
                Console.MESSAGES.deleteConfirm(title + " '" + name + "'"),
                new Feedback.ConfirmationHandler() {
                    @Override
                    public void onConfirmation(boolean isConfirmed) {
                        if (isConfirmed) {
                            circuit.dispatch(new DeleteConnection(addressTemplate, name));
                        }
                    }
                });
    }

    @Override
    protected void onModify(String name, Map<String, Object> changedValues) {
        circuit.dispatch(new UpdateConnection(addressTemplate, name, changedValues));
    }
}
