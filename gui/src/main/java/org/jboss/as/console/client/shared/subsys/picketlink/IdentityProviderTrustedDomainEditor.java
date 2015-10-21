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
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.Property;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.DESCRIPTION;

/**
 * @author Harald Pehl
 */
class IdentityProviderTrustedDomainEditor extends MasterDetailEditor {

    final FederationPresenter presenter;

    IdentityProviderTrustedDomainEditor(final FederationPresenter presenter, final SecurityContext securityContext,
            final StatementContext statementContext, final ResourceDescription lookup) {
        super(securityContext, statementContext, lookup, "Trusted Domain");
        this.presenter = presenter;
    }

    @Override
    public Widget asWidget() {
        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline("Trusted Domain")
                .setDescription(SafeHtmlUtils.fromString(resourceDescription.get(DESCRIPTION).asString()))
                .setMasterTools(tools())
                .setMaster(Console.MESSAGES.available("Trusted Domains"), table());
        return layoutBuilder.build();
    }

    @Override
    void onAdd() {
        presenter.launchNewTrustDomainDialog();
    }

    @Override
    void onModify(final String name, final Map<String, Object> changedValues) {
        presenter.modifyTrustDomain(name, changedValues);
    }

    @Override
    void onRemove(final Property item) {
        presenter.removeTrustDomain(item.getName());
    }
}
