package org.jboss.as.console.client.shared.runtime.ds;

import java.util.List;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.shared.help.HelpSystem;
import org.jboss.as.console.client.shared.runtime.Metric;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.runtime.Sampler;
import org.jboss.as.console.client.shared.runtime.charts.BulletGraphView;
import org.jboss.as.console.client.shared.runtime.charts.Column;
import org.jboss.as.console.client.shared.runtime.charts.NumberColumn;
import org.jboss.as.console.client.shared.runtime.plain.PlainColumnView;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.v3.behaviour.SelectionAwareContext;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolButtonDropdown;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.dmr.client.ModelDescriptionConstants;
import org.jboss.dmr.client.ModelNode;

import static org.jboss.as.console.client.shared.runtime.ds.DataSourceMetricPresenter.DATASOURCE_POOL_ADDRESS;

/**
 * @author Heiko Braun
 * @date 12/10/11
 */
public class DataSourceMetrics implements SelectionAwareContext {

    private static final String[] XA_ONLY = new String[]{
            "XACommitAverageTime",
            "XACommitCount",
            "XACommitMaxTime",
            "XACommitTotalTime",
            "XAEndAverageTime",
            "XAEndCount",
            "XAEndMaxTime",
            "XAEndTotalTime",
            "XAForgetAverageTime",
            "XAForgetCount",
            "XAForgetMaxTime",
            "XAForgetTotalTime",
            "XAPrepareAverageTime",
            "XAPrepareCount",
            "XAPrepareMaxTime",
            "XAPrepareTotalTime",
            "XARecoverAverageTime",
            "XARecoverCount",
            "XARecoverMaxTime",
            "XARecoverTotalTime",
            "XARollbackAverageTime",
            "XARollbackCount",
            "XARollbackMaxTime",
            "XARollbackTotalTime",
            "XAStartAverageTime",
            "XAStartCount",
            "XAStartMaxTime",
            "XAStartTotalTime"
    };

    private DataSourceMetricPresenter presenter;
    private DefaultCellTable<DataSource> table;
    private ListDataProvider<DataSource> dataProvider;
    private Sampler poolSampler;
    private Sampler cacheSampler;
    private boolean isXA;
    private ModelNodeFormBuilder.FormAssets poolStatsForm;

    public DataSourceMetrics(DataSourceMetricPresenter presenter, boolean isXA) {
        this.presenter = presenter;
        this.isXA = isXA;

    }

    Widget asWidget() {

        table = new DefaultCellTable<>(5, DataSource::getJndiName);
        table.setSelectionModel(new SingleSelectionModel<DataSource>());

        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(table);

        com.google.gwt.user.cellview.client.Column<DataSource, String> nameColumn = new com.google.gwt.user.cellview.client.Column<DataSource, String>(
                new TextCell()) {
            @Override
            public String getValue(DataSource object) {
                return object.getName();
            }
        };

        final com.google.gwt.user.cellview.client.Column<DataSource, String> protocolColumn = new com.google.gwt.user.cellview.client.Column<DataSource, String>(
                new TextCell()) {
            @Override
            public String getValue(DataSource object) {
                return object.getJndiName();
            }
        };

        com.google.gwt.user.cellview.client.Column<DataSource, String> status = new com.google.gwt.user.cellview.client.Column<DataSource, String>(
                new TextCell()) {
            @Override
            public String getValue(DataSource object) {
                return String.valueOf(object.isEnabled());
            }
        };

        com.google.gwt.user.cellview.client.Column<DataSource, String> stats = new com.google.gwt.user.cellview.client.Column<DataSource, String>(
                new TextCell()) {
            @Override
            public String getValue(DataSource object) {
                return String.valueOf(object.isStatisticsEnabled());
            }
        };

        table.addColumn(nameColumn, "Name");
        table.addColumn(protocolColumn, "JNDI");
        table.addColumn(status, "Enabled");
        table.addColumn(stats, "Statistics enabled");

        stats.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        status.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        protocolColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        table.getSelectionModel().addSelectionChangeHandler(
                event -> {
                    DataSource ds = getCurrentSelection();
                    presenter.setSelectedDS(ds, isXA);
                });

        // ----

        String title = "Connection Pool";
        final String subaddress = isXA ? "xa-data-source" : "data-source";

        final HelpSystem.AddressCallback addressCallback = new HelpSystem.AddressCallback() {
            @Override
            public ModelNode getAddress() {
                ModelNode address = new ModelNode();
                address.get(ModelDescriptionConstants.ADDRESS).set(RuntimeBaseAddress.get());
                address.get(ModelDescriptionConstants.ADDRESS).add("subsystem", "datasources");
                address.get(ModelDescriptionConstants.ADDRESS).add(subaddress, getCurrentSelection().getName());
                address.get(ModelDescriptionConstants.ADDRESS).add("statistics", "pool");
                return address;
            }
        };


        ToolStrip tools = new ToolStrip();
        final ToolButton verifyBtn = new ToolButton(Console.CONSTANTS.subsys_jca_dataSource_verify(),
                clickEvent -> presenter.verifyConnection(getCurrentSelection().getName(), isXA));
        final ToolButtonDropdown flushDropdown = new ToolButtonDropdown("Flush Gracefully",
                event -> presenter.flush(getCurrentSelection().getName(), "flush-gracefully-connection-in-pool", isXA));
        flushDropdown.addItem("Flush Idle",
                () -> presenter.flush(getCurrentSelection().getName(), "flush-idle-connection-in-pool", isXA));
        flushDropdown.addItem("Flush Invalid",
                () -> presenter.flush(getCurrentSelection().getName(), "flush-invalid-connection-in-pool", isXA));
        flushDropdown.addItem("Flush All",
                () -> presenter.flush(getCurrentSelection().getName(), "flush-all-connection-in-pool", isXA));

        final ToolButton refreshBtn = new ToolButton(Console.CONSTANTS.common_label_refresh(),
                clickEvent -> presenter.setSelectedDS(getCurrentSelection(), isXA));

        verifyBtn.setVisible(true);
        verifyBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_verify_dataSourceDetails());
        if (isXA) {
            verifyBtn.setOperationAddress("/{implicit.host}/{selected.server}/subsystem=datasources/xa-data-source=*",
                    "test-connection-in-pool");
        } else {
            verifyBtn.setOperationAddress("/{implicit.host}/{selected.server}/subsystem=datasources/data-source=*",
                    "test-connection-in-pool");
        }
        tools.addToolButtonRight(verifyBtn);
        tools.addToolWidgetRight(flushDropdown);
        tools.addToolButtonRight(refreshBtn);
        tools.setVisible(true);

        // ----

        ResourceDescription resDescription = presenter.getDescriptionRegistry()
                .lookup(AddressTemplate.of(DATASOURCE_POOL_ADDRESS));

        ModelNodeFormBuilder builder = new ModelNodeFormBuilder()
                .setRuntimeOnly()
                .setResourceDescription(resDescription)
                .setSecurityContext(
                        Console.MODULES.getSecurityFramework().getSecurityContext(presenter.getProxy().getNameToken()));
        if (!isXA) {
            builder.exclude(XA_ONLY);
        }
        poolStatsForm = builder.build();

        NumberColumn avail = new NumberColumn("AvailableCount", "Available Connections");
        Column[] cols = new Column[]{
                avail.setBaseline(true),
                new NumberColumn("AvailableCount", "Active").setComparisonColumn(avail),
                new NumberColumn("MaxUsedCount", "Max Used").setComparisonColumn(avail)
        };

        if (Console.protovisAvailable()) {
            poolSampler = new BulletGraphView(title, "total number", true)
                    .setColumns(cols);
        } else {
            poolSampler = new PlainColumnView(title, addressCallback)
                    .setColumns(cols)
                    .setWidth(100, Style.Unit.PCT);

        }

        // ----

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);

        VerticalPanel tablePanel = new VerticalPanel();
        tablePanel.setStyleName("fill-layout-width");
        tablePanel.add(table);
        tablePanel.add(pager);
        // ----

        String title2 = "Prepared Statement Cache";

        final HelpSystem.AddressCallback addressCallback2 = () -> {
            ModelNode address = new ModelNode();
            address.get(ModelDescriptionConstants.ADDRESS).set(RuntimeBaseAddress.get());
            address.get(ModelDescriptionConstants.ADDRESS).add("subsystem", "datasources");
            address.get(ModelDescriptionConstants.ADDRESS).add(subaddress, getCurrentSelection().getName());
            address.get(ModelDescriptionConstants.ADDRESS).add("statistics", "jdbc");
            return address;
        };
        // ----


        NumberColumn avail2 = new NumberColumn("PreparedStatementCacheAccessCount", "Access Count");
        Column[] cols2 = new Column[]{
                avail2.setBaseline(true),
                new NumberColumn("PreparedStatementCacheHitCount", "Hit Count").setComparisonColumn(avail2),
                new NumberColumn("PreparedStatementCacheMissCount", "Miss Count").setComparisonColumn(avail2)
        };

        if (Console.protovisAvailable()) {
            cacheSampler = new BulletGraphView(title2, "total number")
                    .setColumns(cols2);
        } else {
            cacheSampler = new PlainColumnView(title2, addressCallback2)
                    .setColumns(cols2)
                    .setWidth(100, Style.Unit.PCT);
        }

        VerticalPanel p = new VerticalPanel();
        p.setStyleName("fill-layout-width");
        p.add(poolSampler.asWidget());
        p.add(cacheSampler.asWidget());

        OneToOneLayout layout = new OneToOneLayout()
                .setPlain(true)
                .setTitle(isXA ? "XA Data Sources" : "Data Sources")
                .setHeadline(isXA ? "XA Data Source Metrics" : "Data Source Metrics")
                .setMasterTools(tools)
                .setMaster("Datasource", tablePanel)
                .setDescription(Console.CONSTANTS.subsys_jca_dataSource_metric_desc())
                .addDetail(Console.CONSTANTS.common_label_stats(), p)
                .addDetail(Console.CONSTANTS.subsys_jca_pool_statistics_tab(), poolStatsForm.asWidget());

        return layout.build();
    }

    private DataSource getCurrentSelection() {
        return ((SingleSelectionModel<DataSource>) table.getSelectionModel()).getSelectedObject();
    }

    @Override
    public String getSelection() {
        return getCurrentSelection().getName();
    }

    public void clearSamples() {
        poolSampler.clearSamples();
        cacheSampler.clearSamples();

    }

    public void setDataSources(List<DataSource> topics) {
        dataProvider.setList(topics);
        table.selectDefaultEntity();
    }

    public void setDSPoolMetric(ModelNode result) {

        long avail = result.get("AvailableCount").asLong();
        long active = result.get("ActiveCount").asLong();
        long max = result.get("MaxUsedCount").asLong();

        Metric poolMetric = new Metric(
                avail, active, max
        );

        poolSampler.addSample(poolMetric);
        poolStatsForm.getForm().edit(result);
    }

    public void setDSCacheMetric(Metric metric) {
        cacheSampler.addSample(metric);
    }
}
