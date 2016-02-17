package org.jboss.as.console.client.shared.subsys.activemq.connections;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.subsys.activemq.forms.BridgeConnectionsForm;
import org.jboss.as.console.client.shared.subsys.activemq.forms.DefaultBridgeForm;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqBridge;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;

import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 * @date 4/2/12
 */
public class BridgesList {

    private ContentHeaderLabel serverName;
    private DefaultCellTable<ActivemqBridge> factoryTable;
    private ListDataProvider<ActivemqBridge> factoryProvider;
    private MsgConnectionsPresenter presenter;
    private DefaultBridgeForm defaultAttributes;

    public BridgesList(MsgConnectionsPresenter presenter) {
        this.presenter = presenter;
    }

    @SuppressWarnings("unchecked")
    Widget asWidget() {
        serverName = new ContentHeaderLabel();

        factoryTable = new DefaultCellTable<>(10, ActivemqBridge::getName);
        factoryProvider = new ListDataProvider<ActivemqBridge>();
        factoryProvider.addDataDisplay(factoryTable);

        Column<ActivemqBridge, String> nameColumn = new Column<ActivemqBridge, String>(new TextCell()) {
            @Override
            public String getValue(ActivemqBridge object) {
                return object.getName();
            }
        };

        Column<ActivemqBridge, String> queueColumn = new Column<ActivemqBridge, String>(new TextCell()) {
            @Override
            public String getValue(ActivemqBridge object) {
                return object.getQueueName();
            }
        };

        Column<ActivemqBridge, String> toColumn = new Column<ActivemqBridge, String>(new TextCell()) {
            @Override
            public String getValue(ActivemqBridge object) {
                return object.getForwardingAddress();
            }
        };

        factoryTable.addColumn(nameColumn, "Name");
        factoryTable.addColumn(queueColumn, "Queue");
        factoryTable.addColumn(toColumn, "Forward");

        // defaultAttributes
        defaultAttributes = new DefaultBridgeForm(presenter,
                new FormToolStrip.FormCallback<ActivemqBridge>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                        presenter.onSaveBridge(getSelectedEntity().getName(), changeset);
                    }

                    @Override
                    public void onDelete(ActivemqBridge entity) {}
                });

        BridgeConnectionsForm connectionAttributes = new BridgeConnectionsForm(presenter,
                new FormToolStrip.FormCallback<ActivemqBridge>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                        presenter.onSaveBridge(getSelectedEntity().getName(), changeset);
                    }

                    @Override
                    public void onDelete(ActivemqBridge entity) {}
                });


        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_add(), clickEvent -> presenter.launchNewBridgeWizard()));

        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_remove(), clickEvent -> Feedback.confirm(
                        Console.MESSAGES.deleteTitle("Bridge"),
                        Console.MESSAGES.deleteConfirm("Bridge " + getSelectedEntity().getName()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                presenter.onDeleteBridge(getSelectedEntity().getName());
                            }
                        })));

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadlineWidget(serverName)
                .setDescription(
                        Console.CONSTANTS.bridgeDescription())
                .setMaster("Bridges", factoryTable)
                .setMasterTools(tools)
                .addDetail("Common", defaultAttributes.asWidget())
                .addDetail("Connection Management", connectionAttributes.asWidget());

        defaultAttributes.getForm().bind(factoryTable);
        defaultAttributes.getForm().setEnabled(false);

        connectionAttributes.getForm().bind(factoryTable);
        connectionAttributes.getForm().setEnabled(false);

        return layout.build();
    }

    public void setBridges(List<ActivemqBridge> bridges) {
        factoryProvider.setList(bridges);
        serverName.setText("Bridges: Provider " + presenter.getCurrentServer());

        factoryTable.selectDefaultEntity();

        // populate oracle
        presenter.loadExistingQueueNames(new AsyncCallback<List<String>>() {
            @Override
            public void onFailure(Throwable throwable) {

            }

            @Override
            public void onSuccess(List<String> names) {
                defaultAttributes.setQueueNames(names);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public ActivemqBridge getSelectedEntity() {
        SingleSelectionModel<ActivemqBridge> selectionModel = (SingleSelectionModel<ActivemqBridge>) factoryTable
                .getSelectionModel();
        return selectionModel.getSelectedObject();
    }
}
