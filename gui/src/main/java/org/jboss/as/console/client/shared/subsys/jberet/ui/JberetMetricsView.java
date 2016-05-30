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
package org.jboss.as.console.client.shared.subsys.jberet.ui;

import java.util.List;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.jberet.JberetMetricsPresenter;
import org.jboss.as.console.client.shared.subsys.jberet.Job;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;
import org.useware.kernel.gui.behaviour.StatementContext;

/**
 * @author Harald Pehl
 */
public class JberetMetricsView extends SuspendableViewImpl implements JberetMetricsPresenter.MyView {

    private ThreadPoolRuntimePanel threadPoolPanel;
    private JobsRuntimePanel jobsPanel;

    @Inject
    public JberetMetricsView(final Dispatcher circuit, final ResourceDescriptionRegistry resourceDescriptionRegistry,
            final SecurityFramework securityFramework, StatementContext statementContext) {

        threadPoolPanel = new ThreadPoolRuntimePanel(circuit, resourceDescriptionRegistry, securityFramework);
        jobsPanel = new JobsRuntimePanel(circuit, resourceDescriptionRegistry, securityFramework);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget createWidget() {
        DefaultTabLayoutPanel tabs = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        tabs.addStyleName("default-tabpanel");
        tabs.add(threadPoolPanel.asWidget(), "Batch");
        tabs.add(jobsPanel.asWidget(), "Jobs");
        tabs.selectTab(0);

        return tabs;
    }

    @Override
    public void refresh(ModelNode metric) {
        threadPoolPanel.refresh(metric);
    }

    @Override
    public void refresh(final List<Property> metrics) {
        threadPoolPanel.refresh(metrics);
    }

    @Override
    public void refreshJobs(final List<Job> metrics) {
        jobsPanel.refresh(metrics);
    }

    @Override
    public void setPresenter(final JberetMetricsPresenter presenter) {
        threadPoolPanel.setPresenter(presenter);
        jobsPanel.setPresenter(presenter);
    }
}
