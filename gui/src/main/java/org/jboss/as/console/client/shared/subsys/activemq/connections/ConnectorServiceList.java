package org.jboss.as.console.client.shared.subsys.activemq.connections;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.properties.PropertyEditor;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.subsys.activemq.forms.ConnectorServiceForm;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnectorService;
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
public class ConnectorServiceList {

    private ContentHeaderLabel serverName;
    private DefaultCellTable<ActivemqConnectorService> table;
    private ListDataProvider<ActivemqConnectorService> provider;
    private MsgConnectionsPresenter presenter;
    private ConnectorServiceForm connectorServiceForm;
    private PropertyEditor properties;

    public ConnectorServiceList(MsgConnectionsPresenter presenter) {
        this.presenter = presenter;
    }

    @SuppressWarnings("unchecked")
    Widget asWidget() {
        serverName = new ContentHeaderLabel();

        table = new DefaultCellTable<>(10, ActivemqConnectorService::getName);

        provider = new ListDataProvider<>();
        provider.addDataDisplay(table);

        Column<ActivemqConnectorService, String> name = new Column<ActivemqConnectorService, String>(new TextCell()) {
            @Override
            public String getValue(ActivemqConnectorService object) {
                return object.getName();
            }
        };

        table.addColumn(name, "Name");

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_add(),
                        clickEvent -> presenter.launchNewConnectorServiceWizard()));

        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_remove(), clickEvent -> Feedback.confirm(
                        Console.MESSAGES.deleteTitle("ConnectorService"),
                        Console.MESSAGES.deleteConfirm("ConnectorService " + getSelectedEntity().getName()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                presenter.onDeleteConnectorService(getSelectedEntity());
                            }
                        })));

        connectorServiceForm = new ConnectorServiceForm(new FormToolStrip.FormCallback<ActivemqConnectorService>() {
            @Override
            public void onSave(Map<String, Object> changeset) {
                presenter.onSaveConnectorService(getSelectedEntity(), changeset);
            }

            @Override
            public void onDelete(ActivemqConnectorService entity) {}
        });

        properties = new PropertyEditor(presenter, true);

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadlineWidget(serverName)
                .setDescription("Class name of the factory class that can instantiate the connector service.")
                .setMaster(Console.MESSAGES.available("Services"), table)
                .setMasterTools(tools)
                .addDetail("Detail", connectorServiceForm.asWidget())
                .addDetail("Properties", properties.asWidget());

        connectorServiceForm.getForm().bind(table);

        table.getSelectionModel().addSelectionChangeHandler(selectionChangeEvent -> {
            List<PropertyRecord> props = getSelectedEntity().getParameter();

            String tokens = "connector-service_#_" + getSelectedEntity().getName();
            properties.setProperties(tokens, props);
        });

        return layout.build();

    }

    public void setConnectorServices(List<ActivemqConnectorService> ConnectorServices) {
        provider.setList(ConnectorServices);
        properties.clearValues();
        serverName.setText("ConnectorServices: Provider " + presenter.getCurrentServer());
        table.selectDefaultEntity();
    }

    @SuppressWarnings("unchecked")
    public ActivemqConnectorService getSelectedEntity() {
        SingleSelectionModel<ActivemqConnectorService> selectionModel = (SingleSelectionModel<ActivemqConnectorService>) table
                .getSelectionModel();
        return selectionModel.getSelectedObject();
    }
}
