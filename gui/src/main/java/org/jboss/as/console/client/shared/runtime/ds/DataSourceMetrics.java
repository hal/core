package org.jboss.as.console.client.shared.runtime.ds;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.as.console.client.shared.help.HelpSystem;
import org.jboss.as.console.client.shared.runtime.Metric;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.runtime.Sampler;
import org.jboss.as.console.client.shared.runtime.charts.BulletGraphView;
import org.jboss.as.console.client.shared.runtime.charts.Column;
import org.jboss.as.console.client.shared.runtime.charts.NumberColumn;
import org.jboss.as.console.client.shared.runtime.plain.PlainColumnView;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.dmr.client.ModelDescriptionConstants;
import org.jboss.dmr.client.ModelNode;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 12/10/11
 */
public class DataSourceMetrics {


    private DataSourceMetricPresenter presenter;
    private DefaultCellTable<DataSource> table;
    private ListDataProvider<DataSource> dataProvider;
    private Sampler poolSampler;
    private Sampler cacheSampler;
    private boolean isXA;

    public DataSourceMetrics(DataSourceMetricPresenter presenter, boolean isXA) {
        this.presenter = presenter;
        this.isXA = isXA;

    }

    Widget asWidget() {
        final ToolStrip toolStrip = new ToolStrip();
        toolStrip.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_refresh(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {

                presenter.setSelectedDS(getCurrentSelection(), isXA);
            }
        }));

        // ----

        table = new DefaultCellTable<DataSource>(5, new ProvidesKey<DataSource>() {
            @Override
            public Object getKey(DataSource item) {
                return item.getJndiName();
            }
        });
        table.setSelectionModel(new SingleSelectionModel<DataSource>());

        dataProvider = new ListDataProvider<DataSource>();
        dataProvider.addDataDisplay(table);

        com.google.gwt.user.cellview.client.Column<DataSource, String> nameColumn = new com.google.gwt.user.cellview.client.Column<DataSource, String>(new TextCell()) {
            @Override
            public String getValue(DataSource object) {
                return object.getName();
            }
        };


        com.google.gwt.user.cellview.client.Column<DataSource, String> protocolColumn = new com.google.gwt.user.cellview.client.Column<DataSource, String>(new TextCell()) {
            @Override
            public String getValue(DataSource object) {
                return object.getJndiName();
            }
        };

        com.google.gwt.user.cellview.client.Column<DataSource, String> status = new com.google.gwt.user.cellview.client.Column<DataSource, String>(new TextCell()) {
            @Override
            public String getValue(DataSource object) {
                return String.valueOf(object.isEnabled());
            }
        };

        table.addColumn(nameColumn, "Name");
        table.addColumn(protocolColumn, "JNDI");
        table.addColumn(status, "Enabled?");

        table.getSelectionModel().addSelectionChangeHandler(
                new SelectionChangeEvent.Handler(){
                    @Override
                    public void onSelectionChange(SelectionChangeEvent event) {
                        DataSource ds = getCurrentSelection();
                        presenter.setSelectedDS(ds, isXA);

                    }
        });
        table.getElement().setAttribute("style", "margin-top:15px;margin-bottom:0px;");

        // ----

        String title = "Connection Pool";

        final String subaddress = isXA ? "xa-data-source":"data-source";
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
        final ToolButton verifyBtn = new ToolButton(Console.CONSTANTS.subsys_jca_dataSource_verify(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                presenter.verifyConnection(getCurrentSelection().getName(), isXA);
            }
        });
        final ToolButton flushBtn = new ToolButton("Flush", new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                presenter.flush(getCurrentSelection().getName(), isXA);
            }
        });
        verifyBtn.setVisible(true);
        verifyBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_verify_dataSourceDetails());
        flushBtn.setVisible(true);
        flushBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_verify_dataSourceDetails());
        tools.addToolButtonRight(verifyBtn);
        tools.addToolButtonRight(flushBtn);
        tools.setVisible(true);

        // ----


        NumberColumn avail = new NumberColumn("AvailableCount", "Available Connections");
        Column[] cols = new Column[] {
                avail.setBaseline(true),
                new NumberColumn("AvailableCount","Active").setComparisonColumn(avail),
                new NumberColumn("MaxUsedCount","Max Used").setComparisonColumn(avail)
        };

        if(Console.protovisAvailable())
        {
            poolSampler = new BulletGraphView(title, "total number")
                    .setColumns(cols);
        }
        else
        {
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

        final HelpSystem.AddressCallback addressCallback2 = new HelpSystem.AddressCallback() {
            @Override
            public ModelNode getAddress() {
                ModelNode address = new ModelNode();
                address.get(ModelDescriptionConstants.ADDRESS).set(RuntimeBaseAddress.get());
                address.get(ModelDescriptionConstants.ADDRESS).add("subsystem", "datasources");
                address.get(ModelDescriptionConstants.ADDRESS).add(subaddress, getCurrentSelection().getName());
                address.get(ModelDescriptionConstants.ADDRESS).add("statistics", "jdbc");
                return address;
            }
        };

        // ----


        NumberColumn avail2 = new NumberColumn("PreparedStatementCacheAccessCount", "Access Count");
        Column[] cols2 = new Column[] {
                avail2.setBaseline(true),
                new NumberColumn("PreparedStatementCacheHitCount","Hit Count").setComparisonColumn(avail2),
                new NumberColumn("PreparedStatementCacheMissCount","Miss Count").setComparisonColumn(avail2)
        };

        if(Console.protovisAvailable())
        {
            cacheSampler = new BulletGraphView(title2, "total number")
                    .setColumns(cols2);
        }
        else
        {
            cacheSampler = new PlainColumnView(title2, addressCallback2)
                    .setColumns(cols2)
                    .setWidth(100, Style.Unit.PCT);
        }

        SimpleLayout layout = new SimpleLayout()
                .setPlain(true)
                .setTitle(isXA? "XA Data Sources":"Data Sources")
                .setHeadline(isXA ? "XA Data Source Metrics":"Data Source Metrics")
                .setDescription(Console.CONSTANTS.subsys_jca_dataSource_metric_desc())
                .setTopLevelTools(toolStrip.asWidget())
                .addContent("",tools)
                .addContent("Datasource", table)
                .addContent("Pool Usage", poolSampler.asWidget())
                .addContent("Prepared Statement Cache", cacheSampler.asWidget());
        
        return layout.build();
    }

    private DataSource getCurrentSelection() {
        return ((SingleSelectionModel<DataSource>) table.getSelectionModel()).getSelectedObject();
    }

    public void clearSamples() {
        poolSampler.clearSamples();
        cacheSampler.clearSamples();

    }

    public void setDataSources(List<DataSource> topics) {
        dataProvider.setList(topics);
        table.selectDefaultEntity();
    }

    public void setDSPoolMetric(Metric poolMetric) {
        poolSampler.addSample(poolMetric);
    }

    public void setDSCacheMetric(Metric metric) {
        cacheSampler.addSample(metric);
    }
}
