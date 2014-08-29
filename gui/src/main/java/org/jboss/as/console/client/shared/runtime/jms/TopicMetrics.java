package org.jboss.as.console.client.shared.runtime.jms;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.as.console.client.shared.runtime.charts.Column;
import org.jboss.as.console.client.shared.runtime.charts.NumberColumn;
import org.jboss.as.console.client.shared.subsys.messaging.JMSEndpointJndiColumn;
import org.jboss.as.console.client.shared.subsys.messaging.model.JMSEndpoint;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 12/10/11
 */
public class TopicMetrics {


    private JMSMetricPresenter presenter;
    private DefaultCellTable<JMSEndpoint> topicTable;
    private ListDataProvider<JMSEndpoint> dataProvider;
    private Column[] columns;
    private Grid grid;

    public TopicMetrics(JMSMetricPresenter presenter) {
        this.presenter = presenter;
    }

    Widget asWidget() {
        final ToolStrip toolStrip = new ToolStrip();
        toolStrip.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_refresh(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.setSelectedTopic(getCurrentSelection());
            }
        }));

        // ----

        topicTable = new DefaultCellTable<JMSEndpoint>(5, new ProvidesKey<JMSEndpoint>() {
            @Override
            public Object getKey(JMSEndpoint jmsEndpoint) {
                return jmsEndpoint.getName();
            }
        });
        topicTable.setSelectionModel(new SingleSelectionModel<JMSEndpoint>());

        dataProvider = new ListDataProvider<JMSEndpoint>();
        dataProvider.addDataDisplay(topicTable);

        com.google.gwt.user.cellview.client.Column<JMSEndpoint, String> nameColumn = new com.google.gwt.user.cellview.client.Column<JMSEndpoint, String>(new TextCell()) {
            @Override
            public String getValue(JMSEndpoint object) {
                return object.getName();
            }
        };

        JMSEndpointJndiColumn<JMSEndpoint> jndiColumn = new JMSEndpointJndiColumn<JMSEndpoint>();

        topicTable.addColumn(nameColumn, "Name");
        topicTable.addColumn(jndiColumn, "JNDI");

        topicTable.getSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler(){
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                JMSEndpoint topic = getCurrentSelection();
                presenter.setSelectedTopic(topic);

            }
        });

        // ----

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(topicTable);


        ToolStrip topicTools = new ToolStrip();
        topicTools.addToolButtonRight(new ToolButton("Flush", new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                SingleSelectionModel<JMSEndpoint> selectionModel =
                        (SingleSelectionModel<JMSEndpoint>) topicTable.getSelectionModel();

                final JMSEndpoint topic = selectionModel.getSelectedObject();
                Feedback.confirm("Flush Topic", "Do you really want to flush topic " + topic.getName(),
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    presenter.onFlushTopic(topic);
                                }
                            }
                        });
            }
        }));

        VerticalPanel tablePanel = new VerticalPanel();
        tablePanel.setStyleName("fill-layout-width");
        tablePanel.add(topicTools);
        tablePanel.add(topicTable);
        tablePanel.add(pager);

        columns = new Column[] {
                new NumberColumn("delivering-count", "Delivering Count"),
                new NumberColumn("durable-message-count","Durable Message Count"),
                new NumberColumn("durable-subscription-count","Durable Subscription Count"),
                new NumberColumn("message-count","Message Count"),
                new NumberColumn("messages-added ","Messages Added"),
                new NumberColumn("subscription-count","Subscription Count")

        };

        grid = new Grid(columns.length, 2);
        grid.addStyleName("metric-grid");

        // format
        for (int row = 0; row < columns.length; ++row) {
            grid.getCellFormatter().addStyleName(row, 0,  "nominal");
            grid.getCellFormatter().addStyleName(row, 1, "numerical");
        }

        VerticalPanel desc = new VerticalPanel();
        desc.addStyleName("metric-container");
        desc.add(new HTML("<h3 class='metric-label'>Topic Metrics</h3>"));
        desc.add(grid);

        SimpleLayout layout = new SimpleLayout()
                .setTitle("Topics")
                .setPlain(true)
                .setTopLevelTools(toolStrip.asWidget())
                .setHeadline("JMS Topic Metrics")
                .setDescription(Console.CONSTANTS.subsys_messaging_topic_metric_desc())
                .addContent("Topic Selection", tablePanel)
                .addContent("", desc);

        return layout.build();
    }

    private JMSEndpoint getCurrentSelection() {
        return ((SingleSelectionModel<JMSEndpoint>) topicTable.getSelectionModel()).getSelectedObject();
    }

    public void clearSamples() {
        for(int i=0; i<columns.length;i++)
        {
            grid.setText(i, 0, columns[i].getLabel());
            grid.setText(i, 1, "0");
        }
    }

    public void setTopics(List<JMSEndpoint> topics) {
        dataProvider.setList(topics);
        topicTable.selectDefaultEntity();
    }

    public void updateFrom(ModelNode result) {
        List<Property> atts = result.asPropertyList();

        for(int i=0; i<columns.length; i++)
        {
            for(Property att : atts)
            {
                if(att.getName().equals(columns[i].getDeytpedName()))
                {
                    grid.setText(i, 0, columns[i].getLabel());
                    grid.setText(i, 1, att.getValue().asString());
                }
            }
        }
    }
}
