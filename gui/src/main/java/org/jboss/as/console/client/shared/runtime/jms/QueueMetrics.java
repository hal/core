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
import org.jboss.as.console.client.shared.help.HelpSystem;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.subsys.messaging.JMSEndpointJndiColumn;
import org.jboss.as.console.client.shared.subsys.messaging.model.Queue;
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelDescriptionConstants;
import org.jboss.dmr.client.ModelNode;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 12/10/11
 */
public class QueueMetrics {


    private JMSMetricPresenter presenter;
    private DefaultCellTable<Queue> queueTable;
    private ListDataProvider<Queue> dataProvider;
    private ModelNodeForm queueForm;

    public QueueMetrics(JMSMetricPresenter presenter) {
        this.presenter = presenter;
    }

    public QueueMetrics() {

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

        // ----

        final HelpSystem.AddressCallback addressCallback = new HelpSystem.AddressCallback() {
            @Override
            public ModelNode getAddress() {
                ModelNode address = new ModelNode();
                address.get(ModelDescriptionConstants.ADDRESS).set(RuntimeBaseAddress.get());
                address.get(ModelDescriptionConstants.ADDRESS).add("subsystem", "messaging");
                address.get(ModelDescriptionConstants.ADDRESS).add("hornetq-server", "default");
                address.get(ModelDescriptionConstants.ADDRESS).add("jms-queue", "*");
                return address;
            }
        };

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

        ModelNodeFormBuilder builder = new ModelNodeFormBuilder()
                .setResourceDescription(ModelNode.fromBase64(MessagingResources.queueDescription))
                .setSecurityContext(new ReadOnlyContext())
                .setRuntimeOnly();

        ModelNodeFormBuilder.FormAssets assets = builder.build();
        queueForm = assets.getForm();

        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("fill-layout-width");
        panel.add(assets.getHelp().asWidget());
        panel.add(queueForm.asWidget());

        OneToOneLayout layout = new OneToOneLayout()
                .setTitle("Queues")
                .setPlain(true)
                .setTopLevelTools(toolStrip.asWidget())
                .setHeadline("JMS Queue Metrics")
                .setDescription(Console.CONSTANTS.subsys_messaging_queue_metric_desc())
                .setMaster("Queue Selection", tablePanel)
                .addDetail("Messages", panel);

        return layout.build();
    }

    private Queue getCurrentSelection() {
        return ((SingleSelectionModel<Queue>) queueTable.getSelectionModel()).getSelectedObject();
    }

    public void clearSamples() {
        queueForm.clearValues();

    }

    public void setQueues(List<Queue> queues) {
        dataProvider.setList(queues);
        queueTable.selectDefaultEntity();
    }



    public void updateFrom(ModelNode result) {
        queueForm.edit(result);
    }
}
