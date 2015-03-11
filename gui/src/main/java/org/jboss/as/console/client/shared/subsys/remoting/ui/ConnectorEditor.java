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
import org.jboss.as.console.client.shared.subsys.remoting.store.CreateConnector;
import org.jboss.as.console.client.shared.subsys.remoting.store.DeleteConnector;
import org.jboss.as.console.client.shared.subsys.remoting.store.RemotingStore;
import org.jboss.as.console.client.shared.subsys.remoting.store.UpdateConnector;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.client.v3.widgets.PropertyEditor;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.circuit.Dispatcher;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.List;
import java.util.Map;

import static org.jboss.as.console.client.shared.subsys.remoting.ui.RemotingSelectionAwareContext.SELECTED_ENTITY;
import static org.jboss.dmr.client.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.dmr.client.ModelDescriptionConstants.NAME;

/**
 * Editor for {@link RemotingStore#REMOTE_CONNECTOR} and {@link RemotingStore#REMOTE_HTTP_CONNECTOR}.
 *
 * @author Harald Pehl
 */
class ConnectorEditor extends RemotingEditor {

    private final Dispatcher circuit;
    private final AddressTemplate addressTemplate;
    private final String title;
    private final SaslSecurityEditor saslSecurityEditor;
    private final PropertyEditor securityProperties;
    private final SaslPolicyEditor saslPolicyEditor;

    ConnectorEditor(DispatchAsync dispatcher, Dispatcher circuit, SecurityContext securityContext,
                    StatementContext statementContext, AddressTemplate addressTemplate,
                    ResourceDescription resourceDescription, String title) {
        super(dispatcher, securityContext, statementContext, addressTemplate, resourceDescription);
        this.circuit = circuit;
        this.addressTemplate = addressTemplate;
        this.title = title;

        ResourceDescription securityDescription = resourceDescription.getChildDescription("security", "sasl");
        saslSecurityEditor = new SaslSecurityEditor(circuit, securityContext, addressTemplate, securityDescription);
        saslPolicyEditor = new SaslPolicyEditor(circuit, securityContext, addressTemplate,
                securityDescription.getChildDescription("sasl-policy", "policy"));

        RemotingSelectionAwareContext context = new RemotingSelectionAwareContext(statementContext, this);
        AddressTemplate address = addressTemplate.replaceWildcards(SELECTED_ENTITY).append("security=sasl/property=*");
        ResourceDescription description = securityDescription.getChildDescription("property");
        securityProperties = new PropertyEditor.Builder(dispatcher, context, securityContext, address, description)
                .operationAddress(addressTemplate.append("security=*/property=*")) // this address is used in the security context
                .build();
    }

    @Override
    public Widget asWidget() {
        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline(title + "s")
                .setDescription(SafeHtmlUtils.fromString(resourceDescription.get(DESCRIPTION).asString()))
                .setMasterTools(tools())
                .setMaster(Console.MESSAGES.available(title), table())
                .addDetail("Attributes", formPanel())
                .addDetail("Properties", propertyEditor().asWidget())
                .addDetail("Security", saslSecurityEditor.asWidget())
                .addDetail("Security Properties", securityProperties.asWidget())
                .addDetail("Policy", saslPolicyEditor.asWidget());
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
                circuit.dispatch(new CreateConnector(addressTemplate, instanceName, payload));
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
                            circuit.dispatch(new DeleteConnector(addressTemplate, name));
                        }
                    }
                });
    }

    @Override
    protected void onModify(String name, Map<String, Object> changedValues) {
        circuit.dispatch(new UpdateConnector(addressTemplate, name, changedValues));
    }

    @Override
    protected void updateDetail(Property property) {
        super.updateDetail(property);
        saslSecurityEditor.update(property);
        saslPolicyEditor.update(property);

        ModelNode node = property.getValue();
        if (node.hasDefined("security")) {
            node = node.get("security").get("sasl");
            if (node.hasDefined("property")) {
                List<Property> properties = node.get("property").asPropertyList();
                securityProperties.update(properties);
            } else {
                securityProperties.clearValues();
            }
        } else {
            securityProperties.clearValues();
        }
    }

    @Override
    protected void clearDetail() {
        super.clearDetail();
        saslSecurityEditor.clearDetail();
        securityProperties.clearValues();
        saslPolicyEditor.clearDetail();
    }
}
