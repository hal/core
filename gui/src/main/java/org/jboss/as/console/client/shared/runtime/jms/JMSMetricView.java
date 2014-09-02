package org.jboss.as.console.client.shared.runtime.jms;

import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.as.console.client.shared.subsys.messaging.AddressingDetails;
import org.jboss.as.console.client.shared.subsys.messaging.ConnectionFactoryList;
import org.jboss.as.console.client.shared.subsys.messaging.DivertList;
import org.jboss.as.console.client.shared.subsys.messaging.JMSEditor;
import org.jboss.as.console.client.shared.subsys.messaging.ProviderList;
import org.jboss.as.console.client.shared.subsys.messaging.SecurityDetails;
import org.jboss.as.console.client.shared.subsys.messaging.model.JMSEndpoint;
import org.jboss.as.console.client.shared.subsys.messaging.model.Queue;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.as.console.client.widgets.tables.ViewLinkCell;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.dmr.ResourceDefiniton;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tabs.FakeTabPanel;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 12/10/11
 */
public class JMSMetricView extends SuspendableViewImpl implements JMSMetricPresenter.MyView{

    private JMSMetricPresenter presenter;
    private TopicMetrics topicMetrics;
    private QueueMetrics queueMetrics;
    private PagedView panel;
    private DefaultCellTable table;
    private ListDataProvider<Property> dataProvider;

    @Override
    public Widget createWidget() {

        this.topicMetrics = new TopicMetrics(presenter);
        this.queueMetrics= new QueueMetrics(presenter);

        LayoutPanel layout = new LayoutPanel();

        FakeTabPanel titleBar = new FakeTabPanel("Messaging Statistics");
        layout.add(titleBar);

        panel = new PagedView();

        this.table = new DefaultCellTable(5);
        this.dataProvider = new ListDataProvider<Property>();
        this.dataProvider.addDataDisplay(table);
        this.table.setSelectionModel(new SingleSelectionModel<Property>());

        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };

        Column<Property, String> option = new Column<Property, String>(
                new ViewLinkCell<String>(Console.CONSTANTS.common_label_view(), new ActionCell.Delegate<String>() {
                    @Override
                    public void execute(String selection) {
                        presenter.getPlaceManager().revealPlace(
                                new PlaceRequest(NameTokens.JmsMetricPresenter).with("name", selection)
                        );
                    }
                })
        ) {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };

        table.addColumn(nameColumn, "Name");
        table.addColumn(option, "Option");


        Widget frontPage = new SimpleLayout()
                .setPlain(true)
                .setHeadline("JMS Messaging Provider")
                .setDescription("Please chose a provider from below for specific metrics.")
                .addContent("", table.asWidget())
                .build();

        panel.addPage(Console.CONSTANTS.common_label_back(), frontPage);


        panel.addPage("Queues", queueMetrics.asWidget()) ;
        panel.addPage("Topics", topicMetrics.asWidget()) ;

        // default page
        panel.showPage(0);

        Widget panelWidget = panel.asWidget();
        layout.add(panelWidget);

        layout.setWidgetTopHeight(titleBar, 0, Style.Unit.PX, 40, Style.Unit.PX);
        layout.setWidgetTopHeight(panelWidget, 40, Style.Unit.PX, 100, Style.Unit.PCT);

        return layout;
    }

    @Override
    public void setPresenter(JMSMetricPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void updateProvider(List<Property> provider) {
        dataProvider.setList(provider);
        table.selectDefaultEntity();
    }

    @Override
    public void setSelectedProvider(String selectedProvider) {
        if(null==selectedProvider)
        {
            panel.showPage(0);
        }
        else{

            queueMetrics.setProviderName(selectedProvider);
            topicMetrics.setProviderName(selectedProvider);
            presenter.refreshQueuesAndTopics(selectedProvider);

            // move to first page if still showing topology
            if(0==panel.getPage())
                panel.showPage(1);
        }
    }

    @Override
    public void clearSamples() {
        topicMetrics.clearSamples();
    }

    @Override
    public void setTopics(List<JMSEndpoint> topics) {
        topicMetrics.setTopics(topics);
    }

    @Override
    public void setQueues(List<Queue> queues) {
        queueMetrics.setQueues(queues);
    }

    @Override
    public void updateQueueMetrics(ModelNode result) {
        queueMetrics.updateFrom(result);
    }

    @Override
    public void updateTopicMetrics(ModelNode result) {
        topicMetrics.updateFrom(result);
    }
}
