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

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.jberet.JberetPresenter;
import org.jboss.as.console.client.shared.subsys.jberet.store.AddInMemoryRepository;
import org.jboss.as.console.client.shared.subsys.jberet.store.AddJdbcRepository;
import org.jboss.as.console.client.shared.subsys.jberet.store.AddThreadFactory;
import org.jboss.as.console.client.shared.subsys.jberet.store.AddThreadPool;
import org.jboss.as.console.client.shared.subsys.jberet.store.JberetStore;
import org.jboss.as.console.client.shared.subsys.jberet.store.ModifyJdbcRepository;
import org.jboss.as.console.client.shared.subsys.jberet.store.ModifyComplexAttribute;
import org.jboss.as.console.client.shared.subsys.jberet.store.ModifyThreadFactory;
import org.jboss.as.console.client.shared.subsys.jberet.store.ModifyThreadPool;
import org.jboss.as.console.client.shared.subsys.jberet.store.RemoveInMemoryRepository;
import org.jboss.as.console.client.shared.subsys.jberet.store.RemoveJdbcRepository;
import org.jboss.as.console.client.shared.subsys.jberet.store.RemoveThreadFactory;
import org.jboss.as.console.client.shared.subsys.jberet.store.RemoveThreadPool;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.List;
import java.util.Map;

/**
 * @author Harald Pehl
 */
public class JberetView extends SuspendableViewImpl implements JberetPresenter.MyView {

    private final Dispatcher circuit;
    private final ResourceDescriptionRegistry resourceDescriptionRegistry;
    private final SecurityFramework securityFramework;
    private JberetPresenter presenter;

    private DefaultsPanel defaultsPanel;
    private MasterDetailPanel inMemoryRepositoriesPanel;
    private MasterDetailPanel jdbcRepositoriesPanel;
    private MasterDetailPanel threadFactoriesPanel;
    private MasterDetailPanel threadPoolsPanel;

    @Inject
    public JberetView(final Dispatcher circuit, final ResourceDescriptionRegistry resourceDescriptionRegistry,
            final SecurityFramework securityFramework) {
        this.circuit = circuit;
        this.resourceDescriptionRegistry = resourceDescriptionRegistry;
        this.securityFramework = securityFramework;
    }

    @Override
    public Widget createWidget() {
        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());
        ResourceDescription rootDescription = resourceDescriptionRegistry.lookup(JberetStore.ROOT_ADDRESS);
        ResourceDescription inMemoryRepositoriesDescription = rootDescription
                .getChildDescription("in-memory-job-repository");
        ResourceDescription jdbcRepositoriesDescription = rootDescription.getChildDescription("jdbc-job-repository");
        ResourceDescription threadFactoriesDescription = rootDescription.getChildDescription("thread-factory");
        ResourceDescription threadPoolsDescription = rootDescription.getChildDescription("thread-pool");

        defaultsPanel = new DefaultsPanel(circuit, rootDescription, securityContext);
        inMemoryRepositoriesPanel = new MasterDetailPanel("In Memory Job Repository",
                circuit, inMemoryRepositoriesDescription, securityContext) {
            @Override
            protected void dispatchAdd(final Dispatcher circuit, final Property property) {
                circuit.dispatch(new AddInMemoryRepository(property));
            }

            @Override
            protected void dispatchModify(final Dispatcher circuit, final String name,
                    final Map<String, Object> changedValues) {
                // not supported
            }

            @Override
            protected void dispatchRemove(final Dispatcher circuit, final String name) {
                circuit.dispatch(new RemoveInMemoryRepository(name));
            }
        };
        jdbcRepositoriesPanel = new MasterDetailPanel("JDBC Job Repository",
                circuit, jdbcRepositoriesDescription, securityContext) {
            @Override
            protected void dispatchAdd(final Dispatcher circuit, final Property property) {
                circuit.dispatch(new AddJdbcRepository(property));
            }

            @Override
            protected void dispatchModify(final Dispatcher circuit, final String name,
                    final Map<String, Object> changedValues) {
                circuit.dispatch(new ModifyJdbcRepository(name, changedValues));
            }

            @Override
            protected void dispatchRemove(final Dispatcher circuit, final String name) {
                circuit.dispatch(new RemoveJdbcRepository(name));
            }
        };
        threadFactoriesPanel = new MasterDetailPanel("Thread Factory",
                circuit, threadFactoriesDescription, securityContext) {
            @Override
            protected void dispatchAdd(final Dispatcher circuit, final Property property) {
                circuit.dispatch(new AddThreadFactory(property));
            }

            @Override
            protected void dispatchModify(final Dispatcher circuit, final String name,
                    final Map<String, Object> changedValues) {
                circuit.dispatch(new ModifyThreadFactory(name, changedValues));
            }

            @Override
            protected void dispatchRemove(final Dispatcher circuit, final String name) {
                circuit.dispatch(new RemoveThreadFactory(name));
            }
        };
        threadPoolsPanel = new MasterDetailPanel("Thread Pool",
                circuit, threadPoolsDescription, securityContext, "keepalive-time") {
            @Override
            protected void dispatchAdd(final Dispatcher circuit, final Property property) {
                circuit.dispatch(new AddThreadPool(property));
            }

            @Override
            protected void dispatchModify(final Dispatcher circuit, final String name,
                    final Map<String, Object> changedValues) {
                circuit.dispatch(new ModifyThreadPool(name, changedValues));
            }

            @Override
            protected void dispatchRemove(final Dispatcher circuit, final String name) {
                circuit.dispatch(new RemoveThreadPool(name));
            }

            @Override
            protected void dispatchWriteAttribute(Dispatcher circuit, String parentName, String attributeName, ModelNode payload) {
                circuit.dispatch(new ModifyComplexAttribute(parentName, attributeName, payload));
            }
        };

        PagedView panel = new PagedView(true);
        panel.addPage("Defaults", defaultsPanel.asWidget());
        panel.addPage("In Memory", inMemoryRepositoriesPanel.asWidget());
        panel.addPage("JDBC", jdbcRepositoriesPanel.asWidget());
        panel.addPage("Thread Factories", threadFactoriesPanel.asWidget());
        panel.addPage("Thread Pools", threadPoolsPanel.asWidget());
        panel.showPage(0);

        return panel.asWidget();
    }

    @Override
    public void setPresenter(final JberetPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void init(final ModelNode defaults, final List<Property> inMemoryRepositories,
            final List<Property> jdbcRepositories,
            final List<Property> threadFactories, final List<Property> threadPools) {
        defaultsPanel.update(defaults);
        inMemoryRepositoriesPanel.update(inMemoryRepositories);
        jdbcRepositoriesPanel.update(jdbcRepositories);
        threadFactoriesPanel.update(threadFactories);
        threadPoolsPanel.update(threadPools);
    }
}
