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
package org.jboss.as.console.client.shared.patching.ui;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.core.Updateable;
import org.jboss.as.console.client.shared.patching.PatchManagementPresenter;
import org.jboss.as.console.client.shared.patching.PatchManager;
import org.jboss.as.console.client.shared.patching.Patches;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.List;

/**
 * @author Harald Pehl
 */
public class PatchManagementView extends SuspendableViewImpl implements PatchManagementPresenter.MyView {

    private final IsWidget isWidget;
    private final HasPresenter<PatchManagementPresenter> hasPresenter;
    private final Updateable<List<Patches>> updateable;

    @Inject
    public PatchManagementView(Dispatcher circuit, ProductConfig productConfig, BootstrapContext bootstrapContext,
                               PatchManager patchManager) {
        if (bootstrapContext.isStandalone()) {
            StandalonePanel standalonePanel = new StandalonePanel(productConfig, patchManager);
            isWidget = standalonePanel;
            hasPresenter = standalonePanel;
            updateable = standalonePanel;
        } else {
            DomainPanel domainPanel = new DomainPanel(circuit, productConfig, patchManager);
            isWidget = domainPanel;
            hasPresenter = domainPanel;
            updateable = domainPanel;
        }
    }

    @Override
    public Widget createWidget() {
        return isWidget.asWidget();
    }

    @Override
    public void setPresenter(final PatchManagementPresenter presenter) {
        hasPresenter.setPresenter(presenter);
    }

    @Override
    public void update(List<Patches> patches) {
        updateable.update(patches);
    }
}
