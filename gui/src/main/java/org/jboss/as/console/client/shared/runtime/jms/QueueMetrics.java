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
import org.jboss.as.console.client.shared.subsys.messaging.model.Queue;
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
public class QueueMetrics {


    private JMSMetricPresenter presenter;
    private DefaultCellTable<Queue> queueTable;
    private ListDataProvider<Queue> dataProvider;
    private Grid grid;
    private Column[] columns;

    public QueueMetrics(JMSMetricPresenter presenter) {
        this.presenter = presenter;
    }

    Widget asWidget() {
        final ToolStrip toolStrip = new ToolStrip();
        toolStrip.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_refresh(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.setSelectedQueue(getCurrentSelection());
            }
        }));

        // ----

        queueTable = new DefaultCellTable<Queue>(5, new ProvidesKey<Queue>() {
            @Override
            public Object getKey(Queue queue) {
                return queue.getName();
            }
        });
        queueTable.setSelectionModel(new SingleSelectionModel<Queue>());

        dataProvider = new ListDataProvider<Queue>();
        dataProvider.addDataDisplay(queueTable);

        com.google.gwt.user.cellview.client.Column<Queue, String> nameColumn = new com.google.gwt.user.cellview.client.Column<Queue, String>(new TextCell()) {
            @Override
            public String getValue(Queue object) {
                return object.getName();
            }
        };

        JMSEndpointJndiColumn<Queue> jndiColumn = new JMSEndpointJndiColumn<Queue>();

        queueTable.addColumn(nameColumn, "Name");
        queueTable.addColumn(jndiColumn, "JNDI");

        queueTable.getSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler(){
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Queue queue = getCurrentSelection();
                presenter.setSelectedQueue(queue);

            }
        });


        // -------

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(queueTable);

        ToolStrip queueTools = new ToolStrip();
        queueTools.addToolButtonRight(new ToolButton("Flush", new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                SingleSelectionModel<Queue> selectionModel =
                        (SingleSelectionModel<Queue>)queueTable.getSelectionModel();

                final Queue queue = selectionModel.getSelectedObject();
                Feedback.confirm("Flush Queue", "Do you really want to flush queue "+queue.getName(),
                        new Feedback.ConfirmationHandler(){
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if(isConfirmed)
                                    presenter.onFlushQueue(queue);
                            }
                        });
            }
        }));

        VerticalPanel tablePanel = new VerticalPanel();
        tablePanel.setStyleName("fill-layout-width");
        tablePanel.add(queueTools.asWidget());
        tablePanel.add(queueTable);
        tablePanel.add(pager);

        columns = new Column[] {
                new NumberColumn("consumer-count", "Consumer Count"),
                new NumberColumn("message-count","Message Count"),
                new NumberColumn("messages-added","Messages Added"),
                new NumberColumn("scheduled-count","Scheduled Count")
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
        desc.add(new HTML("<h3 class='metric-label'>Queue Metrics</h3>"));
        desc.add(grid);

        // init
        clearSamples();

        SimpleLayout layout = new SimpleLayout()
                .setTitle("Queues")
                .setPlain(true)
                .setTopLevelTools(toolStrip.asWidget())
                .setHeadline("JMS Queue Metrics")
                .setDescription(Console.CONSTANTS.subsys_messaging_queue_metric_desc())
                .addContent("Queue Selection", tablePanel)
                .addContent("Metrics", desc);

        return layout.build();
    }

    private Queue getCurrentSelection() {
        return ((SingleSelectionModel<Queue>) queueTable.getSelectionModel()).getSelectedObject();
    }

    public void clearSamples() {
        for(int i=0; i<columns.length;i++)
        {
            grid.setText(i, 0, columns[i].getLabel());
            grid.setText(i, 1, "0");
        }

    }

    public void setQueues(List<Queue> queues) {
        dataProvider.setList(queues);
        queueTable.selectDefaultEntity();
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
