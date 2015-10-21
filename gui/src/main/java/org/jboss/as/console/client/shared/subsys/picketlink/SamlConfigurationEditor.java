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

import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.Map;

/**
 * @author Harald Pehl
 */
class SamlConfigurationEditor extends SingletonEditor {

    final FederationPresenter presenter;

    SamlConfigurationEditor(final FederationPresenter presenter,
            final SecurityContext securityContext, final StatementContext statementContext,
            final ResourceDescription resourceDescription) {
        super(securityContext, statementContext, resourceDescription, "SAML Configuration");
        this.presenter = presenter;
    }

    @Override
    void onModify(final Map<String, Object> changedValues) {
        presenter.modifySamlConfiguration(changedValues);
    }
}
