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

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.runtime.charts.Column;
import org.jboss.as.console.client.shared.runtime.charts.NumberColumn;
import org.jboss.as.console.client.shared.subsys.jberet.JberetMetricsPresenter;
import org.jboss.as.console.client.shared.subsys.jberet.store.JberetStore;
import org.jboss.as.console.client.shared.subsys.jberet.store.RefreshThreadPoolMetric;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

import static com.google.gwt.dom.client.Style.Float.RIGHT;
import static com.google.gwt.dom.client.Style.Unit.PX;

/**
 * @author Claudio Miranda
 */
public class ThreadPoolRuntimePanel implements IsWidget {

    private final static String[] METRIC_ATTRIBUTES = {
            "active-count",
            "completed-task-count",
            "current-thread-count",
            "largest-thread-count",
            "queue-size",
            "rejected-count",
            "task-count",
    };

    private final Dispatcher circuit;
    private final ResourceDescriptionRegistry resourceDescriptionRegistry;
    private final SecurityFramework securityFramework;
    private JberetMetricsPresenter presenter;

    private DefaultCellTable<Property> table;
    private ListDataProvider<Property> dataProvider;
    private SingleSelectionModel<Property> selectionModel;
    private Column[] columns;
    private Grid grid;
    
    ThreadPoolRuntimePanel(final Dispatcher circuit, final ResourceDescriptionRegistry resourceDescriptionRegistry,
    final SecurityFramework securityFramework) {
        this.circuit = circuit;
        this.resourceDescriptionRegistry = resourceDescriptionRegistry;
        this.securityFramework = securityFramework;
    }

    @Override
    public Widget asWidget() {
        ProvidesKey<Property> providesKey = Property::getName;
        table = new DefaultCellTable<>(5, providesKey);
        dataProvider = new ListDataProvider<>(providesKey);
        dataProvider.addDataDisplay(table);
        selectionModel = new SingleSelectionModel<>(providesKey);
        selectionModel.addSelectionChangeHandler(event -> {
            Property selection = selectionModel.getSelectedObject();
            if (selection != null) {
                ModelNode metric = selection.getValue();
                refresh(metric);
            }
        });
        table.setSelectionModel(selectionModel);
        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };
        table.addColumn(nameColumn, "Name");

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);

        VerticalPanel tableAndPager = new VerticalPanel();
        tableAndPager.setStyleName("fill-layout-width");
        tableAndPager.add(table);
        tableAndPager.add(pager);

        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());
        ResourceDescription rootDescription = resourceDescriptionRegistry.lookup(JberetStore.METRICS_ROOT_ADDRESS);
        ResourceDescription threadPoolsDescription = rootDescription.getChildDescription("thread-pool");
        ModelNodeFormBuilder.FormAssets formAssets = new ModelNodeFormBuilder()
                .setResourceDescription(threadPoolsDescription)
                .setSecurityContext(securityContext)
                .include(METRIC_ATTRIBUTES)
                .build();
        Widget help = formAssets.getHelp().asWidget();
        help.getElement().getStyle().setMarginTop(40, PX);

        HTML refreshBtn = new HTML("<i class='icon-refresh'></i> Refresh Results");
        refreshBtn.setStyleName("html-link");
        refreshBtn.getElement().getStyle().setPaddingRight(10, PX);
        refreshBtn.getElement().getStyle().setFloat(RIGHT);
        refreshBtn.addClickHandler(event -> {
            Property selection = selectionModel.getSelectedObject();
            if (selection != null) {
                circuit.dispatch(new RefreshThreadPoolMetric(selection.getName()));
            }
        });
        HorizontalPanel header = new HorizontalPanel();
        header.addStyleName("fill-layout-width");
        header.add(new HTML("<h3 class='metric-label-embedded'>Thread Pool</h3>"));
        header.add(refreshBtn);

        columns = new Column[]{
                new NumberColumn("active-count", "Active Count"),
                new NumberColumn("completed-task-count", "Completed Task Count"),
                new NumberColumn("current-thread-count", "Current Thread Count"),
                new NumberColumn("largest-thread-count", "Largest Thread Count"),
                new NumberColumn("queue-size", "Queue Size"),
                new NumberColumn("rejected-count", "Rejected Count"),
                new NumberColumn("task-count", "Task Count")
        };
        grid = new Grid(columns.length, 2);
        grid.addStyleName("metric-grid");
        for (int row = 0; row < columns.length; ++row) {
            grid.getCellFormatter().addStyleName(row, 0, "nominal");
            grid.getCellFormatter().addStyleName(row, 1, "numerical");
        }

        VerticalPanel metricsPanel = new VerticalPanel();
        metricsPanel.addStyleName("metric-container");
        metricsPanel.add(header);
        metricsPanel.add(grid);

        SimpleLayout layout = new SimpleLayout()
                .setPlain(true)
                .setHeadline("Thread Pools")
                .setDescription(SafeHtmlUtils.fromString(threadPoolsDescription.get("description").asString()))
                .addContent("", tableAndPager)
                .addContent("", help)
                .addContent("", metricsPanel);

        
        
        return layout.build().asWidget();

    }

    public void refresh(ModelNode metric) {
        if (metric != null) {
            List<Property> attributes = metric.asPropertyList();
            for (int i = 0; i < columns.length; i++) {
                for (Property attribute : attributes) {
                    if (attribute.getName().equals(columns[i].getDeytpedName())) {
                        grid.setText(i, 0, columns[i].getLabel());
                        grid.setText(i, 1, attribute.getValue().asString());
                    }
                }
            }
        }
    }

    public void refresh(final List<Property> metrics) {
        selectionModel.clear();
        dataProvider.setList(metrics);
        table.selectDefaultEntity();
    }

    public void setPresenter(final JberetMetricsPresenter presenter) {
        this.presenter = presenter;
    }
}
