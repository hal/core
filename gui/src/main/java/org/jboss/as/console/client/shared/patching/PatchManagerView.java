/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.console.client.shared.patching;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.patching.ui.PatchPanel;
import org.jboss.as.console.client.v3.stores.domain.HostStore;

/**
 * @author Harald Pehl
 */
public class PatchManagerView extends SuspendableViewImpl
        implements PatchManagerPresenter.MyView, PatchManagerElementId {

    private final PatchPanel patchPanel;


    @Inject
    public PatchManagerView(ProductConfig productConfig, BootstrapContext bootstrapContext,
            HostStore hostStore) {
        this.patchPanel = new PatchPanel(productConfig, bootstrapContext, hostStore);
    }

    @Override
    public Widget createWidget() {
        return patchPanel.asWidget();
    }

    @Override
    public void setPresenter(final PatchManagerPresenter presenter) {
        patchPanel.setPresenter(presenter);
    }

    @Override
    public void update(final Patches patches) {
        patchPanel.update(patches);
    }
}
