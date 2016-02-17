package org.jboss.as.console.client.shared.subsys.activemq.connections;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.properties.PropertyEditor;
import org.jboss.as.console.client.shared.properties.PropertyRecord;
import org.jboss.as.console.client.shared.subsys.activemq.forms.ConnectorForm;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnector;
import org.jboss.as.console.client.shared.subsys.activemq.model.ConnectorType;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;

import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 * @date 4/2/12
 */
public class ConnectorList {

    private ContentHeaderLabel serverName;
    private DefaultCellTable<ActivemqConnector> table;
    private ListDataProvider<ActivemqConnector> provider;
    private MsgConnectionsPresenter presenter;
    private ConnectorForm connectorForm;
    private ConnectorType type;
    private PropertyEditor properties;

    public ConnectorList(MsgConnectionsPresenter presenter, ConnectorType type) {
        this.presenter = presenter;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    Widget asWidget() {
        serverName = new ContentHeaderLabel();

        table = new DefaultCellTable<>(10, ActivemqConnector::getName);
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);

        provider = new ListDataProvider<>();
        provider.addDataDisplay(table);

        Column<ActivemqConnector, String> name = new Column<ActivemqConnector, String>(new TextCell()) {
            @Override
            public String getValue(ActivemqConnector object) {
                return object.getName();
            }
        };

        table.addColumn(name, "Name");

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_add(),
                        clickEvent -> presenter.launchNewConnectorWizard(type)));

        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_remove(), clickEvent -> Feedback.confirm(
                        Console.MESSAGES.deleteTitle("Connector"),
                        Console.MESSAGES.deleteConfirm("Connector " + getSelectedEntity().getSocketBinding()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                presenter.onDeleteConnector(getSelectedEntity());
                            }
                        })));

        connectorForm = new ConnectorForm(presenter, new FormToolStrip.FormCallback<ActivemqConnector>() {
            @Override
            public void onSave(Map<String, Object> changeset) {
                presenter.onSaveConnector(getSelectedEntity(), changeset);
            }

            @Override
            public void onDelete(ActivemqConnector entity) {}
        }, type);

        properties = new PropertyEditor(presenter, true);

        VerticalPanel layout = new VerticalPanel();
        layout.add(tools);
        layout.add(table);
        layout.add(pager);

        connectorForm.getForm().bind(table);

        TabPanel tabs = new TabPanel();
        tabs.setStyleName("default-tabpanel");
        tabs.getElement().setAttribute("style", "margin-top:15px;");
        tabs.addStyleName("master_detail-detail");

        tabs.add(connectorForm.asWidget(), Console.CONSTANTS.common_label_details());
        tabs.add(properties.asWidget(), Console.CONSTANTS.common_label_properties());

        layout.add(tabs);
        tabs.selectTab(0);

        table.getSelectionModel().addSelectionChangeHandler(selectionChangeEvent -> {
            List<PropertyRecord> props = getSelectedEntity().getParameter();
            String tokens = getSelectedEntity().getType().getResource() + "_#_" + getSelectedEntity().getName();
            properties.setProperties(tokens, props);
        });

        return layout;
    }

    public void setConnectors(List<ActivemqConnector> Connectors) {
        properties.clearValues();
        provider.setList(Connectors);
        serverName.setText("Connectors: Provider " + presenter.getCurrentServer());
        table.selectDefaultEntity();

        // populate oracle
        presenter.loadSocketBindings(
                new AsyncCallback<List<String>>() {
                    @Override
                    public void onFailure(Throwable throwable) {}

                    @Override
                    public void onSuccess(List<String> names) {
                        connectorForm.setSocketBindings(names);
                    }
                });
    }

    @SuppressWarnings("unchecked")
    public ActivemqConnector getSelectedEntity() {
        SingleSelectionModel<ActivemqConnector> selectionModel = (SingleSelectionModel<ActivemqConnector>) table.getSelectionModel();
        return selectionModel.getSelectedObject();
    }


}
