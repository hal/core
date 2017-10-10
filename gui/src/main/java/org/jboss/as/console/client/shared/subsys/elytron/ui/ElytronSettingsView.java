/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.console.client.shared.subsys.elytron.ui;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.elytron.ElytronSettingsPresenter;
import org.jboss.as.console.client.shared.subsys.elytron.store.ElytronStore;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.gwt.circuit.Dispatcher;

/**
 * Created by Marek Marusic <mmarusic@redhat.com> on 10/4/17.
 */
public class ElytronSettingsView extends SuspendableViewImpl implements ElytronSettingsPresenter.MyView {
    private final Dispatcher circuit;
    private final ResourceDescriptionRegistry resourceDescriptionRegistry;
    private final SecurityFramework securityFramework;
    private ElytronSettingsPresenter presenter;

    private SettingsView settingsView;

    @Inject
    public ElytronSettingsView(final Dispatcher circuit, final ResourceDescriptionRegistry resourceDescriptionRegistry,
                       final SecurityFramework securityFramework) {
        this.circuit = circuit;
        this.resourceDescriptionRegistry = resourceDescriptionRegistry;
        this.securityFramework = securityFramework;
    }

    @Override
    public void initSettings(ModelNode rootNode) {
        settingsView.update(rootNode);
    }

    @Override
    public void setPresenter(final ElytronSettingsPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());
        ResourceDescription rootDescription = resourceDescriptionRegistry.lookup(ElytronStore.ROOT_ADDRESS);

        settingsView = new SettingsView(circuit, rootDescription, securityContext, "Settings", ElytronStore.ROOT_ADDRESS);

        return settingsView.asWidget();
    }
}
