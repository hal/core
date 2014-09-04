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
package org.jboss.as.console.client.administration;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.widgets.TwoColumnLayout;

/**
 * @author Harald Pehl
 */
public class AdministrationView extends SuspendableViewImpl implements AdministrationPresenter.MyView {

    private TwoColumnLayout layout;
    private LayoutPanel contentCanvas;

    @Inject
    public AdministrationView() {

        contentCanvas = new LayoutPanel();
        LHSAdministrationNavigation lhsNavigation = new LHSAdministrationNavigation();
        layout = new TwoColumnLayout(lhsNavigation.asWidget(), contentCanvas.asWidget());
    }

    @Override
    public Widget createWidget() {
        return layout.asWidget();
    }

    @Override
    public void setInSlot(Object slot, IsWidget content) {
        if (slot == AdministrationPresenter.TYPE_MainContent) {
            if (content != null) { setContent(content); }
        }
    }

    private void setContent(IsWidget newContent) {
        contentCanvas.clear();
        contentCanvas.add(newContent);
    }
}
