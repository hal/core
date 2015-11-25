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

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.PropertyEditor;
import org.jboss.as.console.client.v3.widgets.SubResourceAddPropertyDialog;
import org.jboss.as.console.client.v3.widgets.SubResourcePropertyManager;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.useware.kernel.gui.behaviour.FilteringStatementContext;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.DESCRIPTION;

/**
 * @author Harald Pehl
 */
class KeyStoreEditor extends SingletonEditor {

    class KeyStoreContext extends FilteringStatementContext {

        public KeyStoreContext(final StatementContext delegate) {
            super(delegate, new Filter() {
                public String filter(String key) {
                    if ("federation".equals(key)) {
                        return presenter.getFederation();
                    }
                    return "*";
                }

                @Override
                public String[] filterTuple(String key) {
                    return null;
                }
            });
        }
    }


    final FederationPresenter presenter;
    final DispatchAsync dispatcher;
    PropertyEditor keys;

    KeyStoreEditor(final FederationPresenter presenter, final DispatchAsync dispatcher,
            final SecurityContext securityContext, final StatementContext statementContext,
            final ResourceDescription resourceDescription) {
        super(securityContext, statementContext, resourceDescription, "Key Store");
        this.presenter = presenter;
        this.dispatcher = dispatcher;
    }

    @Override
    public Widget asWidget() {
        AddressTemplate keysAddress = AddressTemplate.of("{selected.profile}/subsystem=picketlink-federation/" +
                "federation={federation}/key-store=key-store/key=*");
        ResourceDescription propertyDescription = resourceDescription.getChildDescription("key");

        SubResourcePropertyManager propertyManager = new SubResourcePropertyManager(keysAddress,
                new KeyStoreContext(statementContext), dispatcher);
        SubResourceAddPropertyDialog addDialog = new SubResourceAddPropertyDialog(propertyManager, securityContext,
                propertyDescription);
        keys = new PropertyEditor.Builder(propertyManager)
                // using parameterAddress would cause an exception
                .operationAddress(FederationPresenter.KEY_STORE_TEMPLATE.append("key=*"))
                .addDialog(addDialog)
                .build();

        OneToOneLayout layout = new OneToOneLayout()
                .setPlain(true)
                .setHeadline(resourceName)
                .setDescription(resourceDescription.get(DESCRIPTION).asString())
                .addDetail(Console.CONSTANTS.common_label_attributes(), formPanel())
                .addDetail("Host Keys", keys.asWidget());
        return layout.build();
    }

    @Override
    void onModify(final Map<String, Object> changedValues) {
        presenter.modifyKeyStore(changedValues);
    }

    @Override
    void update(final ModelNode modelNode) {
        super.update(modelNode);
        if (modelNode.hasDefined("key")) {
            List<Property> properties = modelNode.get("key").asPropertyList();
            keys.update(properties);
        } else {
            keys.clearValues();
        }
    }

    @Override
    void reset() {
        super.reset();
        keys.clearValues();
    }
}
