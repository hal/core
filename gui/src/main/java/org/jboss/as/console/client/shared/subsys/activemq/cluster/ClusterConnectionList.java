package org.jboss.as.console.client.shared.subsys.activemq.cluster;

import java.util.List;
import java.util.Map;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.subsys.activemq.forms.ClusterConnectionForm;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqClusterConnection;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;

/**
 * @author Heiko Braun
 * @date 4/2/12
 */
public class ClusterConnectionList {

    private ContentHeaderLabel serverName;
    private DefaultCellTable<ActivemqClusterConnection> factoryTable;
    private ListDataProvider<ActivemqClusterConnection> factoryProvider;
    private MsgClusteringPresenter presenter;
    private ClusterConnectionForm defaultAttributes;

    public ClusterConnectionList(MsgClusteringPresenter presenter) {
        this.presenter = presenter;
    }

    @SuppressWarnings("unchecked")
    Widget asWidget() {
        serverName = new ContentHeaderLabel();

        factoryTable = new DefaultCellTable<>(10, ActivemqClusterConnection::getName);
        factoryProvider = new ListDataProvider<>();
        factoryProvider.addDataDisplay(factoryTable);

        Column<ActivemqClusterConnection, String> nameColumn = new Column<ActivemqClusterConnection, String>(
                new TextCell()) {
            @Override
            public String getValue(ActivemqClusterConnection object) {
                return object.getName();
            }
        };

        factoryTable.addColumn(nameColumn, "Name");

        // defaultAttributes
        defaultAttributes = new ClusterConnectionForm(presenter,
                new FormToolStrip.FormCallback<ActivemqClusterConnection>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                        presenter.saveClusterConnection(getSelectedEntity().getName(), changeset);
                    }

                    @Override
                    public void onDelete(ActivemqClusterConnection entity) {

                    }
                });

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_add(),
                        clickEvent -> presenter.launchNewClusterConnectionWizard()));

        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_remove(), clickEvent -> Feedback.confirm(
                        Console.MESSAGES.deleteTitle("ClusterConnection"),
                        Console.MESSAGES.deleteConfirm("ClusterConnection " + getSelectedEntity().getName()),
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    presenter.onDeleteClusterConnection(getSelectedEntity().getName());
                                }
                            }
                        })));

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadlineWidget(serverName)
                .setDescription(
                        Console.CONSTANTS.clusterConnectionDescription())
                .setMaster("ClusterConnections", factoryTable)
                .setMasterTools(tools)
                .setDetail(Console.CONSTANTS.common_label_details(), defaultAttributes.asWidget());

        defaultAttributes.getForm().bind(factoryTable);
        defaultAttributes.getForm().setEnabled(false);

        return layout.build();
    }

    public void setClusterConnections(List<ActivemqClusterConnection> ClusterConnections) {
        factoryProvider.setList(ClusterConnections);
        serverName.setText("ClusterConnections: Provider " + presenter.getCurrentServer());
        factoryTable.selectDefaultEntity();

    }

    @SuppressWarnings("unchecked")
    public ActivemqClusterConnection getSelectedEntity() {
        SingleSelectionModel<ActivemqClusterConnection> selectionModel = (SingleSelectionModel<ActivemqClusterConnection>) factoryTable
                .getSelectionModel();
        return selectionModel.getSelectedObject();
    }


}
