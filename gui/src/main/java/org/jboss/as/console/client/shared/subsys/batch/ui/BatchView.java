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
package org.jboss.as.console.client.shared.subsys.batch.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.batch.BatchPresenter;
import org.jboss.as.console.client.shared.subsys.batch.store.BatchStore;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.List;

/**
 * @author Harald Pehl
 */
public class BatchView extends SuspendableViewImpl implements BatchPresenter.MyView {

    private final StatementContext statementContext;
    private final SecurityFramework securityFramework;

    private BatchPresenter presenter;
    private BatchPanel batchPanel;
    private ThreadPoolPanel threadPoolPanel;
    private ThreadFactoriesPanel threadFactoriesPanel;

    @Inject
    public BatchView(BatchStore batchStore, SecurityFramework securityFramework) {
        this.statementContext = batchStore.getStatementContext();
        this.securityFramework = securityFramework;
    }

    @Override
    public void setPresenter(BatchPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());
        batchPanel = new BatchPanel(statementContext, securityContext, presenter);
        threadPoolPanel = new ThreadPoolPanel(statementContext, securityContext, presenter);
        threadFactoriesPanel = new ThreadFactoriesPanel(statementContext, securityContext, presenter);

        DefaultTabLayoutPanel tabs = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        tabs.addStyleName("default-tabpanel");
        tabs.add(batchPanel, "Batch");
        tabs.add(threadPoolPanel, "Thread Pool");
        tabs.add(threadFactoriesPanel, "Thread Factories");
        tabs.selectTab(0);

        return tabs;
    }

    @Override
    public void select(ResourceAddress resourceAddress, String key) {
        if (resourceAddress.getResourceType().equals("thread-factory")) {
            threadFactoriesPanel.select(key);
        }
    }

    @Override
    public void update(ResourceAddress resourceAddress, ModelNode model) {
        if (resourceAddress.getResourceType().equals("subsystem")) {
            batchPanel.updateBatch(model);
        } else if (resourceAddress.getResourceType().equals("job-repository")) {
            batchPanel.updateJobRepository(model);
        } else if (resourceAddress.getResourceType().equals("thread-pool")) {
            threadPoolPanel.update(model);
        }
    }

    @Override
    public void update(ResourceAddress resourceAddress, List<Property> model) {
        if (resourceAddress.getResourceType().equals("thread-factory")) {
            threadFactoriesPanel.update(model);
        }
    }
}
