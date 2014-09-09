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
package org.jboss.as.console.client.shared.subsys.io;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.io.bufferpool.BufferPoolPanel;
import org.jboss.as.console.client.shared.subsys.io.worker.WorkerPanel;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Harald Pehl
 */
public class IOView extends SuspendableViewImpl implements IOPresenter.MyView {

    private final SecurityFramework securityFramework;

    private IOPresenter presenter;
    private WorkerPanel workerPanel;
    private BufferPoolPanel bufferPoolPanel;

    @Inject
    public IOView(SecurityFramework securityFramework) {
        this.securityFramework = securityFramework;
    }

    @Override
    public void setPresenter(IOPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        workerPanel = new WorkerPanel(presenter, securityFramework);
        bufferPoolPanel = new BufferPoolPanel(presenter, securityFramework);

        DefaultTabLayoutPanel tabs = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        tabs.addStyleName("default-tabpanel");
        tabs.add(workerPanel, "Worker");
        tabs.add(bufferPoolPanel, "Buffer Pool");
        tabs.selectTab(0);

        return tabs;
    }

    @Override
    public void select(ResourceAddress resourceAddress, String key) {
        if (resourceAddress.getResourceType().equals("buffer-pool")) {
            bufferPoolPanel.select(resourceAddress, key);
        } else if (resourceAddress.getResourceType().equals("worker")) {
            workerPanel.select(resourceAddress, key);
        }
    }

    @Override
    public void update(ResourceAddress resourceAddress, Property model) {
        throw new UnsupportedOperationException("Update of single property not supported by " + IOView.class.getName());
    }

    @Override
    public void update(ResourceAddress resourceAddress, List<Property> model) {
        if (resourceAddress.getResourceType().equals("buffer-pool")) {
            bufferPoolPanel.update(model);
        } else if (resourceAddress.getResourceType().equals("worker")) {
            workerPanel.update(model);
        }
    }
}
