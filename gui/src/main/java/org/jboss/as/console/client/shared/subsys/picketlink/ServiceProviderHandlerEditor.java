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

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.PropertyEditor;
import org.jboss.as.console.client.v3.widgets.SubResourceAddPropertyDialog;
import org.jboss.as.console.client.v3.widgets.SubResourcePropertyManager;
import org.jboss.ballroom.client.rbac.SecurityContext;
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
class ServiceProviderHandlerEditor extends MasterDetailEditor {

    class HandlerContext extends FilteringStatementContext {

        public HandlerContext(final StatementContext delegate) {
            super(delegate, new Filter() {
                public String filter(String key) {
                    if ("federation".equals(key)) {
                        return presenter.getFederation();
                    } else if ("service.provider".equals(key)) {
                        return presenter.getServiceProvider();
                    } else if ("handler".equals(key)) {
                        return selection().getName();
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


    final ServiceProviderPresenter presenter;
    final DispatchAsync dispatcher;
    PropertyEditor parameter;

    ServiceProviderHandlerEditor(final ServiceProviderPresenter presenter,
            final DispatchAsync dispatcher,
            final SecurityContext securityContext,
            final StatementContext statementContext,
            final ResourceDescription resourceDescription,
            final String resourceName) {
        super(securityContext, statementContext, resourceDescription, resourceName);
        this.presenter = presenter;
        this.dispatcher = dispatcher;
    }

    @Override
    public Widget asWidget() {
        AddressTemplate parameterAddress = AddressTemplate.of("{selected.profile}/subsystem=picketlink-federation/" +
                "federation={federation}/service-provider={service.provider}/handler={handler}/handler-parameter=*");
        ResourceDescription propertyDescription = resourceDescription.getChildDescription("handler-parameter");

        SubResourcePropertyManager propertyManager = new SubResourcePropertyManager(parameterAddress,
                new HandlerContext(statementContext), dispatcher);
        SubResourceAddPropertyDialog addDialog = new SubResourceAddPropertyDialog(propertyManager, securityContext,
                propertyDescription);
        parameter = new PropertyEditor.Builder(propertyManager)
                // using parameterAddress would cause an exception
                .operationAddress(FederationPresenter.SERVICE_PROVIDER_HANDLER_TEMPLATE.append("handler-parameter=*"))
                .addDialog(addDialog)
                .build();

        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline("SAML Handler")
                .setDescription(SafeHtmlUtils.fromString(resourceDescription.get(DESCRIPTION).asString()))
                .setMasterTools(tools())
                .setMaster(Console.MESSAGES.available("SAML Handler"), table())
                .addDetail("Attributes", formPanel())
                .addDetail("Handler Parameters", parameter.asWidget());
        return layoutBuilder.build();
    }

    @Override
    void onAdd() {
        presenter.launchNewHandlerDialog();
    }

    @Override
    void onModify(final String name, final Map<String, Object> changedValues) {
        // not supported
    }

    @Override
    void onRemove(final Property item) {
        presenter.removeHandler(item.getName());
    }

    @Override
    void updateDetail(final Property property) {
        super.updateDetail(property);
        if (property.getValue().hasDefined("handler-parameter")) {
            List<Property> properties = property.getValue().get("handler-parameter").asPropertyList();
            parameter.update(properties);
        } else {
            parameter.clearValues();
        }
    }

    @Override
    void clearDetail() {
        super.clearDetail();
        parameter.clearValues();
    }
}
