package org.jboss.as.console.client.shared.runtime.jms;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.rbac.ReadOnlyContext;
import org.jboss.as.console.client.shared.subsys.messaging.JMSEndpointJndiColumn;
import org.jboss.as.console.client.shared.subsys.messaging.model.JMSEndpoint;
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 12/10/11
 */
public class TopicMetrics {


    private JMSMetricPresenter presenter;
    private DefaultCellTable<JMSEndpoint> topicTable;
    private ListDataProvider<JMSEndpoint> dataProvider;
    private ModelNodeForm topicForm;

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


        ModelNodeFormBuilder builder = new ModelNodeFormBuilder()
                .setResourceDescription(ModelNode.fromBase64(MessagingResources.queueDescription))
                .setSecurityContext(new ReadOnlyContext())
                .setRuntimeOnly();

        ModelNodeFormBuilder.FormAssets assets = builder.build();
        topicForm = assets.getForm();

        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("fill-layout");
        panel.add(assets.getHelp().asWidget());
        panel.add(topicForm.asWidget());

        VerticalPanel messagePanel = new VerticalPanel();
        messagePanel.setStyleName("fill-layout-width");
        messagePanel.add(panel);

        OneToOneLayout layout = new OneToOneLayout()
                .setTitle("Topics")
                .setPlain(true)
                .setTopLevelTools(toolStrip.asWidget())
                .setHeadline("JMS Topic Metrics")
                .setDescription(Console.CONSTANTS.subsys_messaging_topic_metric_desc())
                .setMaster("Topic Selection", tablePanel)
                .addDetail("Messages", panel);

        return layout.build();
    }

    private JMSEndpoint getCurrentSelection() {
        return ((SingleSelectionModel<JMSEndpoint>) topicTable.getSelectionModel()).getSelectedObject();
    }

    public void clearSamples() {
        topicForm.clearValues();

    }

    public void setTopics(List<JMSEndpoint> topics) {
        dataProvider.setList(topics);
        topicTable.selectDefaultEntity();
    }

    public void updateFrom(ModelNode result) {
        topicForm.edit(result);
    }
}
