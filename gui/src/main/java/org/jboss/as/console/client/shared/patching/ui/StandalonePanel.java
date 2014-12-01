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
package org.jboss.as.console.client.shared.patching.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.Updateable;
import org.jboss.as.console.client.shared.patching.PatchManagementPresenter;
import org.jboss.as.console.client.shared.patching.PatchManager;
import org.jboss.as.console.client.shared.patching.Patches;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;

import java.util.List;

/**
 * Panel which wraps a {@link org.jboss.as.console.client.shared.patching.ui.PatchInfoPanel} with a header to be used
 * in standalone mode.
 *
 * @author Harald Pehl
 */
public class StandalonePanel implements IsWidget, HasPresenter<PatchManagementPresenter>,Updateable<List<Patches>> {

    private final PatchInfoPanel patchInfoPanel;

    public StandalonePanel(final ProductConfig productConfig, PatchManager patchManager) {
        this.patchInfoPanel = new PatchInfoPanel(productConfig, patchManager);
    }

    @Override
    public Widget asWidget() {
        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("rhs-content-panel");
        panel.add(patchInfoPanel);

        DefaultTabLayoutPanel tabLayoutPanel = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        tabLayoutPanel.addStyleName("default-tabpanel");
        tabLayoutPanel.add(new ScrollPanel(panel), "Patch Management");
        return tabLayoutPanel;
    }

    @Override
    public void setPresenter(final PatchManagementPresenter presenter) {
        patchInfoPanel.setPresenter(presenter);
    }

    @Override
    public void update(List<Patches> update) {
        if (!update.isEmpty()) {
            patchInfoPanel.update(update.get(0));
        }
    }
}
