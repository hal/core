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

import static org.jboss.as.console.client.shared.subsys.picketlink.PicketLinkDirectory.*;

/**
 * @author Harald Pehl
 */
public class FederationView extends SuspendableViewImpl implements FederationPresenter.MyView  {

    private final SecurityFramework securityFramework;
    private final StatementContext statementContext;
    private final ResourceDescriptionRegistry descriptionRegistry;
    private final DispatchAsync dispatcher;

    private FederationPresenter presenter;
    private IdentityProviderEditor identityProviderEditor;
    private KeyStoreEditor keyStoreEditor;
    private SamlConfigurationEditor samlConfigurationEditor;
    private IdentityProviderHandlerEditor handlerEditor;
    private IdentityProviderTrustedDomainEditor trustedDomainEditor;

    @Inject
    public FederationView(final SecurityFramework securityFramework, final StatementContext statementContext,
            final ResourceDescriptionRegistry descriptionRegistry, final DispatchAsync dispatcher) {
        this.securityFramework = securityFramework;
        this.statementContext = statementContext;
        this.descriptionRegistry = descriptionRegistry;
        this.dispatcher = dispatcher;
    }

    @Override
    public Widget createWidget() {
        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());

        identityProviderEditor = new IdentityProviderEditor(presenter, securityContext, statementContext,
                descriptionRegistry.lookup(IDENTITY_PROVIDER_TEMPLATE));
        samlConfigurationEditor = new SamlConfigurationEditor(presenter, securityContext, statementContext,
                descriptionRegistry.lookup(SAML_TEMPLATE));
        handlerEditor = new IdentityProviderHandlerEditor(presenter, dispatcher, securityContext, statementContext,
                descriptionRegistry.lookup(IDENTITY_PROVIDER_HANDLER_TEMPLATE), "SAML Handler");
        keyStoreEditor = new KeyStoreEditor(presenter, dispatcher, securityContext, statementContext,
                descriptionRegistry.lookup(KEY_STORE_TEMPLATE));
        trustedDomainEditor = new IdentityProviderTrustedDomainEditor(presenter, securityContext, statementContext,
                descriptionRegistry.lookup(IDENTITY_PROVIDER_TRUST_DOMAIN_TEMPLATE));


        PagedView pagedView = new PagedView(true);
        pagedView.addPage("Identity Provider", identityProviderEditor.asWidget());
        pagedView.addPage("SAML Configuration", samlConfigurationEditor.asWidget());
        pagedView.addPage("SAML Handlers", handlerEditor.asWidget());
        pagedView.addPage("Key Store", keyStoreEditor.asWidget());
        pagedView.addPage("Trusted Domains", trustedDomainEditor.asWidget());

        pagedView.showPage(0);

        DefaultTabLayoutPanel root = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        root.addStyleName("default-tabpanel");
        root.add(pagedView.asWidget(), "Federation");
        root.selectTab(0);

        return root.asWidget();
    }

    @Override
    public void setPresenter(final FederationPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void update(final ModelNode federation, List<String> securityDomains) {
        ModelNode identityProvider = federation.get("identity-provider").asProperty().getValue();
        identityProviderEditor.update(identityProvider, securityDomains);

        if (federation.hasDefined("saml")) {
            samlConfigurationEditor.update(federation.get("saml").asProperty().getValue());
        } else {
            samlConfigurationEditor.reset();
        }
        if (federation.hasDefined("key-store")) {
            keyStoreEditor.update(federation.get("key-store").asProperty().getValue());
        } else {
            keyStoreEditor.reset();
        }

        if (identityProvider.hasDefined("handler")) {
            handlerEditor.updateMaster(identityProvider.get("handler").asPropertyList());
        } else {
            handlerEditor.updateMaster(Collections.emptyList());
        }
        if (identityProvider.hasDefined("trust-domain")) {
            trustedDomainEditor.updateMaster(identityProvider.get("trust-domain").asPropertyList());
        } else {
            trustedDomainEditor.updateMaster(Collections.emptyList());
        }
    }
}
