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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.runtime.charts.Column;
import org.jboss.as.console.client.shared.runtime.charts.NumberColumn;
import org.jboss.as.console.client.shared.subsys.jberet.JberetMetricsPresenter;
import org.jboss.as.console.client.shared.subsys.jberet.Job;
import org.jboss.as.console.client.shared.subsys.jberet.store.JberetStore;
import org.jboss.as.console.client.shared.subsys.jberet.store.LoadJobsMetrics;
import org.jboss.as.console.client.shared.subsys.jberet.store.RestartJob;
import org.jboss.as.console.client.shared.subsys.jberet.store.StartJob;
import org.jboss.as.console.client.shared.subsys.jberet.store.StopJob;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.dmr.client.ModelDescriptionConstants;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

/**
 * @author Claudio Miranda
 */
public class JobsRuntimePanel implements IsWidget {

    private final static String[] METRIC_ATTRIBUTES = {
        "batch-status",
        "create-time",
        "end-time",
        "exit-status",
        "instance-id",
        "last-updated-time",
        "start-time",
    };

    private final Dispatcher circuit;
    private final ResourceDescriptionRegistry resourceDescriptionRegistry;
    private final SecurityFramework securityFramework;
    private JberetMetricsPresenter presenter;

    private DefaultCellTable<Job> table;
    private ListDataProvider<Job> dataProvider;
    private SingleSelectionModel<Job> selectionModel;
    private List<Job> jobs = new ArrayList<>();
    private Column[] columns;
    private Grid grid;
    private ToolButton btnStart;
    private ToolButton btnStop;
    private ToolButton btnRestart;
    
    JobsRuntimePanel(final Dispatcher circuit, final ResourceDescriptionRegistry resourceDescriptionRegistry,
    final SecurityFramework securityFramework) {
        this.circuit = circuit;
        this.resourceDescriptionRegistry = resourceDescriptionRegistry;
        this.securityFramework = securityFramework;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Widget asWidget() {
        ProvidesKey<Job> providesKey = job1 -> job1.getDeploymentName() + job1.getName() + job1.getExecutionId();
        table = new DefaultCellTable<>(5, providesKey);
        dataProvider = new ListDataProvider<>(providesKey);
        dataProvider.addDataDisplay(table);
        selectionModel = new SingleSelectionModel<>(providesKey);
        selectionModel.addSelectionChangeHandler(event -> {
            Job job = selectionModel.getSelectedObject();
            if (job != null) {
                refresh(job);
            }
        });
        table.setSelectionModel(selectionModel);

        TextColumn<Job> deploymentColumn = new TextColumn<Job>() {
            @Override
            public String getValue(Job job) {
                String deploymentName = job.getDeploymentName();
                if (job.getSubdeploymentName().length() > 0) {
                    deploymentName = deploymentName + "/" + job.getSubdeploymentName();
                }
                return deploymentName;
            }
        };
        TextColumn<Job> jobColumn = new TextColumn<Job>() {
            @Override
            public String getValue(Job node) {
                return node.getJobXmlName();
            }
        };
        TextColumn<Job> idColumn = new TextColumn<Job>() {
            @Override
            public String getValue(Job node) {
                return node.getInstanceId();
            }
        };
        TextColumn<Job> executionIdColumn = new TextColumn<Job>() {
            @Override
            public String getValue(Job node) {
                return node.getExecutionId();
            }
        };
        TextColumn<Job> statusColumn = new TextColumn<Job>() {
            @Override
            public String getValue(Job node) {
                return node.getCurrentStatus();
            }
        };
        TextColumn<Job> startColumn = new TextColumn<Job>() {
            @Override
            public String getValue(Job node) {
                return node.getStartTime();
            }
        };
        executionIdColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        idColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        statusColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        table.addColumn(deploymentColumn, "Deployment");
        table.addColumn(jobColumn, "Job-Xml-Name");
        table.addColumn(executionIdColumn, "Execution Id");
        table.addColumn(idColumn, "Instance Id");
        table.addColumn(statusColumn, "Batch Status");
        table.addColumn(startColumn, "Start time");

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);

        VerticalPanel tableAndPager = new VerticalPanel();
        tableAndPager.setStyleName("fill-layout-width");
        tableAndPager.add(table);
        tableAndPager.add(pager);

        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());
        ResourceDescription jobDescription = resourceDescriptionRegistry.lookup(JberetStore.JOB_DEPLOYMENT_ADDRESS);
        ModelNodeFormBuilder.FormAssets formAssets = new ModelNodeFormBuilder()
            .setResourceDescription(jobDescription)
            .setSecurityContext(securityContext)
            .include(METRIC_ATTRIBUTES)
            .build();
        Widget help = formAssets.getHelp().asWidget();
        help.getElement().getStyle().setMarginTop(40, Style.Unit.PX);

        HorizontalPanel header = new HorizontalPanel();
        header.addStyleName("fill-layout-width");
        header.add(new HTML("<h3 class='metric-label-embedded'>Job Status</h3>"));
        
        columns = new Column[]{
            new NumberColumn("instance-id", "Instance Id"),
            new NumberColumn("batch-status", "Batch Status"),
            new NumberColumn("exit-status", "Exit Status"),
            new NumberColumn("create-time", "Create Time"),
            new NumberColumn("start-time", "Start Time"),
            new NumberColumn("end-time", "End Time"),
            new NumberColumn("last-updated-time", "Last Updated Time")
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
            .setHeadline("Job Status")
            .setDescription(SafeHtmlUtils.fromString(jobDescription.get(ModelDescriptionConstants.DESCRIPTION).asString()))
            .addContent("", buttonsTool())
            .addContent("", tableAndPager)
            .addContent("", help)
            .addContent("", metricsPanel);
        
        return layout.build().asWidget();
    }

    private ToolStrip buttonsTool() {
        ToolStrip tools = new ToolStrip();

        final TextBox filter = new TextBox();
        filter.setMaxLength(30);
        filter.setVisibleLength(20);
        filter.getElement().setAttribute("style", "float:right; width:120px;");
        filter.addKeyUpHandler(keyUpEvent -> {
            String word = filter.getText();
            if(word != null && word.trim().length() > 0) {
                filter(word);
            }
            else {
                clearFilter();
            }
        });

        this.btnStart = new ToolButton(Console.CONSTANTS.common_label_start(),
                event -> {
                    Job job = selectionModel.getSelectedObject();
                    circuit.dispatch(
                            new StartJob(job.getDeploymentName(), job.getSubdeploymentName(),
                                    job.getJobXmlName()));
                });

        this.btnStop = new ToolButton(Console.CONSTANTS.common_label_stop(),
                event -> {
                    Job job = selectionModel.getSelectedObject();
                    circuit.dispatch(
                            new StopJob(job.getDeploymentName(), job.getSubdeploymentName(),
                                job.getName(), job.getExecutionId()));
                });

        this.btnRestart = new ToolButton(Console.CONSTANTS.common_label_restart(),
                event -> {
                    Job job = selectionModel.getSelectedObject();
                    if ("COMPLETED".equals(job.getCurrentStatus())) {
                        Console.warning(Console.MESSAGES.batch_cannot_restart(job.getExecutionId()));
                    } else {
                        circuit.dispatch(
                                new RestartJob(job.getDeploymentName(), job.getSubdeploymentName(),
                                        job.getName(), job.getExecutionId()));
                    }
                });

        final HTML label = new HTML(Console.CONSTANTS.commom_label_filter()+":&nbsp;");
        label.getElement().setAttribute("style", "padding-top:8px;");
        tools.addToolWidget(label);
        tools.addToolWidget(filter);
        
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_refresh(), 
                event -> circuit.dispatch(new LoadJobsMetrics())));

        tools.addToolButtonRight(btnStart);
        tools.addToolButtonRight(btnStop);
        tools.addToolButtonRight(btnRestart);
        return tools;
    }

    public void refresh(Job job) {
        if (job.getInstanceId().length() > 0) {
            if ("STARTED".equals(job.getCurrentStatus())) {
                this.btnStop.setEnabled(true);
                this.btnRestart.setEnabled(false);
            } else {
                this.btnStop.setEnabled(false);
                this.btnRestart.setEnabled(true);
            }
        } else {
            this.btnRestart.setEnabled(false);
            this.btnStop.setEnabled(false);
            this.btnStart.setEnabled(true);
        }
        grid.clear(true);
        List<Property> attributes = job.asPropertyList();
        for (int i = 0; i < columns.length; i++) {
            for (Property attribute : attributes) {
                if (attribute.getName().equals(columns[i].getDeytpedName())) {
                    grid.setText(i, 0, columns[i].getLabel());
                    String val = "";
                    if (attribute.getValue().isDefined()) {
                        val = attribute.getValue().asString();
                    }
                    grid.setText(i, 1, val);
                }
            }
        }
    }

    public void refresh(List<Job> metrics) {
        selectionModel.clear();
        dataProvider.setList(metrics);
        this.jobs.clear();
        this.jobs.addAll(metrics);
        table.selectDefaultEntity();
        grid.clear(true);
        if (metrics.size() > 0) {
            this.btnRestart.setEnabled(true);
            this.btnStop.setEnabled(true);
            this.btnStart.setEnabled(true);
        } else {
            this.btnRestart.setEnabled(false);
            this.btnStop.setEnabled(false);
            this.btnStart.setEnabled(false);
        }
    }

    public void setPresenter(final JberetMetricsPresenter presenter) {
        this.presenter = presenter;
    }

    public void filter(String word) {
        final List<Job> filteredJobList  = new ArrayList<>();
        word = word.toLowerCase();
        for(Job job : jobs) {
            if (job.getDeploymentName().toLowerCase().contains(word)
                    || job.getSubdeploymentName().toLowerCase().contains(word)
                    || job.getCurrentStatus().toLowerCase().contains(word)
                    || job.getName().toLowerCase().contains(word)
                    || job.getJobXmlName().toLowerCase().contains(word))
                filteredJobList.add(job);
        }
        List<Job> propList = dataProvider.getList();
        propList.clear(); // cannot call setList() as that breaks the sort handler
        propList.addAll(filteredJobList);

    }

    public void clearFilter() {
        List<Job> propList = dataProvider.getList();
        propList.clear(); // cannot call setList() as that breaks the sort handler
        propList.addAll(jobs);
    }

}
