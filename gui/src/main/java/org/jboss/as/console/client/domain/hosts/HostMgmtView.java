/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

package org.jboss.as.console.client.domain.hosts;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.widgets.TwoColumnLayout;

import java.util.List;
import java.util.Set;

/**
 * @author Heiko Braun
 * @date 3/2/11
 */
@Deprecated
public class HostMgmtView extends SuspendableViewImpl implements HostMgmtPresenter.MyView {

    private HostMgmtPresenter presenter;

    private TwoColumnLayout layout;
    private LayoutPanel contentCanvas;
    private LHSHostsNavigation lhsNavigation;

    public HostMgmtView() {


        contentCanvas = new LayoutPanel();
        lhsNavigation = new LHSHostsNavigation();

        layout = new TwoColumnLayout(lhsNavigation.asWidget(), contentCanvas);

    }

    @Override
    public void setPresenter(HostMgmtPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        return layout.asWidget();
    }

    @Override
    public void setInSlot(Object slot, IsWidget content) {

        if (slot == HostMgmtPresenter.TYPE_MainContent) {
            if(content!=null)
                setContent(content);

        }
    }

    private void setContent(IsWidget newContent) {
        contentCanvas.clear();
        contentCanvas.add(newContent);
    }


    @Override
    public void updateHosts(String selectedHost, Set<String> hostNames) {
        lhsNavigation.setHosts(selectedHost, hostNames);
    }
}
