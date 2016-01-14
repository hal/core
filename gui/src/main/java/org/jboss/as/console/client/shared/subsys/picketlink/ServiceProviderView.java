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

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.Collections;
import java.util.List;

import static org.jboss.as.console.client.shared.subsys.picketlink.PicketLinkDirectory.SERVICE_PROVIDER_HANDLER_TEMPLATE;
import static org.jboss.as.console.client.shared.subsys.picketlink.PicketLinkDirectory.SERVICE_PROVIDER_TEMPLATE;

/**
 * @author Harald Pehl
 */
public class ServiceProviderView extends SuspendableViewImpl implements ServiceProviderPresenter.MyView {

    private final SecurityFramework securityFramework;
    private final StatementContext statementContext;
    private final ResourceDescriptionRegistry descriptionRegistry;
    private final DispatchAsync dispatcher;

    private ServiceProviderPresenter presenter;
    private ServiceProviderEditor serviceProviderEditor;
    private ServiceProviderHandlerEditor handlerEditor;

    @Inject
    public ServiceProviderView(final SecurityFramework securityFramework, final StatementContext statementContext,
            final ResourceDescriptionRegistry descriptionRegistry, final DispatchAsync dispatcher) {
        this.securityFramework = securityFramework;
        this.statementContext = statementContext;
        this.descriptionRegistry = descriptionRegistry;
        this.dispatcher = dispatcher;
    }

    @Override
    public Widget createWidget() {
        //noinspection Duplicates
        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());

        serviceProviderEditor = new ServiceProviderEditor(presenter, securityContext, statementContext,
                descriptionRegistry.lookup(SERVICE_PROVIDER_TEMPLATE));
        handlerEditor = new ServiceProviderHandlerEditor(presenter, dispatcher, securityContext, statementContext,
                descriptionRegistry.lookup(SERVICE_PROVIDER_HANDLER_TEMPLATE), "SAML Handler");

        PagedView pagedView = new PagedView(true);
        pagedView.addPage("Service Provider", serviceProviderEditor.asWidget());
        pagedView.addPage("SAML Handlers", handlerEditor.asWidget());

        pagedView.showPage(0);

        DefaultTabLayoutPanel root = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        root.addStyleName("default-tabpanel");
        root.add(pagedView.asWidget(), "Service Provider");
        root.selectTab(0);

        return root.asWidget();
    }

    @Override
    public void setPresenter(final ServiceProviderPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void update(final ModelNode serviceProvider, final List<String> securityDomains) {
        serviceProviderEditor.update(serviceProvider, securityDomains);

        if (serviceProvider.hasDefined("handler")) {
            handlerEditor.updateMaster(serviceProvider.get("handler").asPropertyList());
        } else {
            handlerEditor.updateMaster(Collections.emptyList());
        }
    }
}
