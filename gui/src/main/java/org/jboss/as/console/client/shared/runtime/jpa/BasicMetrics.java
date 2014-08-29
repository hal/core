package org.jboss.as.console.client.shared.runtime.jpa;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.as.console.client.shared.help.HelpSystem;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.runtime.Sampler;
import org.jboss.as.console.client.shared.runtime.charts.BulletGraphView;
import org.jboss.as.console.client.shared.runtime.charts.Column;
import org.jboss.as.console.client.shared.runtime.charts.NumberColumn;
import org.jboss.as.console.client.shared.runtime.charts.TextColumn;
import org.jboss.as.console.client.shared.runtime.jpa.model.JPADeployment;
import org.jboss.as.console.client.shared.runtime.plain.PlainColumnView;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.dmr.client.ModelDescriptionConstants;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @date 1/19/12
 */
public class BasicMetrics {

    private JPAMetricPresenter presenter;
    private JPADeployment currentUnit;

    private HTML title;

    private Sampler queryCacheSampler;
    private Sampler txSampler;
    private Sampler queryExecSampler;
    private Sampler secondLevelSampler;
    private Sampler connectionSampler;

    private String[] tokens;
    private HTML slowQuery;

    public BasicMetrics(JPAMetricPresenter presenter) {
        this.presenter = presenter;
    }

    Widget asWidget() {

        final ToolStrip toolStrip = new ToolStrip();
        toolStrip.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_refresh(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.loadMetrics(tokens);
            }
        }));

        //  ------

        NumberColumn txCount = new NumberColumn("completed-transaction-count","Completed");

        Column[] cols = new Column[] {
                txCount.setBaseline(true),
                new NumberColumn("successful-transaction-count","Successful").setComparisonColumn(txCount)

        };


        final HelpSystem.AddressCallback addressCallback = new HelpSystem.AddressCallback() {
            @Override
            public ModelNode getAddress() {
                ModelNode address = new ModelNode();
                address.get(ModelDescriptionConstants.ADDRESS).set(RuntimeBaseAddress.get());
                address.get(ModelDescriptionConstants.ADDRESS).add("deployment", "*");
                address.get(ModelDescriptionConstants.ADDRESS).add("subsystem", "jpa");
                address.get(ModelDescriptionConstants.ADDRESS).add("hibernate-persistence-unit", "*");
                return address;
            }
        };

        if(Console.protovisAvailable())
        {
            txSampler = new BulletGraphView("Transactions", "total number").setColumns(cols);
        }
        else
        {
            txSampler = new PlainColumnView("Transactions", addressCallback)
                    .setColumns(cols)
                    .setWidth(100, Style.Unit.PCT);
        }

        //  ------

        NumberColumn queryCount = new NumberColumn("query-cache-put-count","Put Count");

        Column[] queryCols = new Column[] {
                queryCount.setBaseline(true),
                new NumberColumn("query-cache-hit-count","Hit Count").setComparisonColumn(queryCount),
                new NumberColumn("query-cache-miss-count","Miss Count").setComparisonColumn(queryCount)

        };

        if(Console.protovisAvailable())
        {
            queryCacheSampler = new BulletGraphView("Query Cache", "total number", false).setColumns(queryCols);
        }
        else
        {
            queryCacheSampler = new PlainColumnView("Query Cache", addressCallback)
                    .setColumns(queryCols)
                    .setWidth(100, Style.Unit.PCT);
        }

        //  ------

        NumberColumn queryExecCount = new NumberColumn("query-execution-count","Execution Count");

        Column[] queryExecCols = new Column[] {
                queryExecCount,
                new NumberColumn("query-execution-max-time","Max Time")
        };


        if(Console.protovisAvailable())
        {
            queryExecSampler = new BulletGraphView("Query Execution", "total number", false).setColumns(queryExecCols);
        }
        else
        {
            queryExecSampler  = new PlainColumnView("Execution", addressCallback)
                    .setColumns(queryExecCols)
                    .setWidth(100, Style.Unit.PCT);
        }

        //  ------

        NumberColumn secondLevelCount = new NumberColumn("second-level-cache-put-count","Put Count");

        Column[] secondLevelCols = new Column[] {
                secondLevelCount.setBaseline(true),
                new NumberColumn("second-level-cache-hit-count","Hit Count").setComparisonColumn(secondLevelCount),
                new TextColumn("second-level-cache-miss-count","Miss Count").setComparisonColumn(secondLevelCount)

        };

        if(Console.protovisAvailable())
        {
            secondLevelSampler = new BulletGraphView("Second Level Cache", "total number").setColumns(secondLevelCols);
        }
        else
        {
            secondLevelSampler  = new PlainColumnView("Second Level Cache", addressCallback)
                    .setColumns(secondLevelCols)
                    .setWidth(100, Style.Unit.PCT);
        }


        //  ------


        NumberColumn sessionOpenCount = new NumberColumn("session-open-count", "Sessions opened");
        Column[] connectionCols = new Column[] {
                sessionOpenCount.setBaseline(true),
                new TextColumn("session-close-count","Closed").setComparisonColumn(sessionOpenCount)

        };

        if(Console.protovisAvailable())
        {
            connectionSampler = new BulletGraphView("Sessions", "total number").setColumns(connectionCols);
        }
        else
        {
            connectionSampler  = new PlainColumnView("Sessions", addressCallback)
                    .setColumns(connectionCols)
                    .setWidth(100, Style.Unit.PCT);
        }
        // ----

        title = new HTML();
        title.setStyleName("content-header-label");


        // -------

        VerticalPanel connectionPanel = new VerticalPanel();
        connectionPanel.setStyleName("fill-layout-width");
        connectionPanel.add(connectionSampler.asWidget());

        VerticalPanel txPanel = new VerticalPanel();
        txPanel.setStyleName("fill-layout-width");
        txPanel.add(txSampler.asWidget());

        VerticalPanel queryPanel = new VerticalPanel();
        queryPanel.setStyleName("fill-layout-width");

        slowQuery = new HTML();
        slowQuery.setStyleName("help-panel-open");
        slowQuery.getElement().setAttribute("style", "padding:5px");


        queryPanel.add(queryExecSampler.asWidget());
        queryPanel.add(slowQuery);

        queryPanel.add(queryCacheSampler.asWidget());



        VerticalPanel secondPanel = new VerticalPanel();
        secondPanel.setStyleName("fill-layout-width");
        secondPanel.add(secondLevelSampler.asWidget());


        SimpleLayout layout = new SimpleLayout()
                .setPlain(true)
                .setTopLevelTools(toolStrip.asWidget())
                .setHeadlineWidget(title)
                .setDescription(Console.CONSTANTS.subsys_jpa_basicMetric_desc())
                .addContent("Sessions", connectionPanel)
                .addContent("Transactions", txPanel)
                .addContent("Queries", queryPanel)
                .addContent("Second Level Cache", secondPanel);


        return layout.build();
    }

    public void setUnit(JPADeployment unit) {
        this.currentUnit = unit;
    }

    public void setContextName(String[] tokens) {
        this.tokens = tokens;
        title.setText("Persistence Unit Metrics: "+tokens[0]+"#"+tokens[1]);
    }


    public void updateMetric(UnitMetric unitMetric) {
        txSampler.addSample(unitMetric.getTxMetric());
        queryCacheSampler.addSample(unitMetric.getQueryMetric());
        queryExecSampler.addSample(unitMetric.getQueryExecMetric());

        slowQuery.setHTML("<b>Max Time Query</b>: "+ unitMetric.getQueryExecMetric().get(2));

        secondLevelSampler.addSample(unitMetric.getSecondLevelCacheMetric());
        connectionSampler.addSample(unitMetric.getConnectionMetric());
    }

    public void clearValues() {
        txSampler.clearSamples();
        queryCacheSampler.clearSamples();
        queryExecSampler.clearSamples();
        slowQuery.setHTML("<b>Max Time Query</b>: ");
        secondLevelSampler.clearSamples();
        connectionSampler.clearSamples();
    }
}
