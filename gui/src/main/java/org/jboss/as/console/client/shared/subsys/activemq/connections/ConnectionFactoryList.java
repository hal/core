package org.jboss.as.console.client.shared.subsys.activemq.connections;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.subsys.activemq.forms.CFConnectionsForm;
import org.jboss.as.console.client.shared.subsys.activemq.forms.DefaultCFForm;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnectionFactory;
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
public class ConnectionFactoryList {

    private ContentHeaderLabel serverName;
    private DefaultCellTable<ActivemqConnectionFactory> factoryTable;
    private ListDataProvider<ActivemqConnectionFactory> factoryProvider;
    private MsgConnectionsPresenter presenter;

    public ConnectionFactoryList(MsgConnectionsPresenter presenter) {
        this.presenter = presenter;
    }

    @SuppressWarnings("unchecked")
    Widget asWidget() {
        serverName = new ContentHeaderLabel();

        factoryTable = new DefaultCellTable<>(10, ActivemqConnectionFactory::getName);
        factoryProvider = new ListDataProvider<>();
        factoryProvider.addDataDisplay(factoryTable);

        Column<ActivemqConnectionFactory, String> nameColumn = new Column<ActivemqConnectionFactory, String>(
                new TextCell()) {
            @Override
            public String getValue(ActivemqConnectionFactory object) {
                return object.getName();
            }
        };

        Column<ActivemqConnectionFactory, String> jndiColumn = new Column<ActivemqConnectionFactory, String>(
                new TextCell()) {
            @Override
            public String getValue(ActivemqConnectionFactory endpoint) {
                StringBuilder builder = new StringBuilder();
                List<String> jndiNames = endpoint.getEntries();
                if (!jndiNames.isEmpty()) {
                    builder.append("[");
                    for (Iterator<String> iterator = jndiNames.iterator(); iterator.hasNext(); ) {
                        String jndiName = iterator.next();
                        builder.append(jndiName);
                        if (iterator.hasNext()) {
                            builder.append(", ");
                        }
                    }
                    builder.append("]");
                }
                return builder.toString();
            }
            
        };

        factoryTable.addColumn(nameColumn, "Name");
        factoryTable.addColumn(jndiColumn, "JNDI");

        // defaultAttributes
        DefaultCFForm defaultAttributes = new DefaultCFForm(presenter,
                new FormToolStrip.FormCallback<ActivemqConnectionFactory>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                        
                        presenter.saveConnnectionFactory(getSelectedFactory().getName(), changeset);
                    }

                    @Override
                    public void onDelete(ActivemqConnectionFactory entity) {

                    }
                });

        CFConnectionsForm connectionAttributes = new CFConnectionsForm(presenter,
                new FormToolStrip.FormCallback<ActivemqConnectionFactory>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                        presenter.saveConnnectionFactory(getSelectedFactory().getName(), changeset);
                    }

                    @Override
                    public void onDelete(ActivemqConnectionFactory entity) {}
                });

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_add(), clickEvent -> presenter.launchNewCFWizard()));

        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_remove(), clickEvent -> Feedback.confirm(
                        Console.MESSAGES.deleteTitle("Connection Factory"),
                        Console.MESSAGES.deleteConfirm("Connection Factory " + getSelectedFactory().getName()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                presenter.onDeleteCF(getSelectedFactory().getName());
                            }
                        })));

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadlineWidget(serverName)
                .setDescription(
                        Console.CONSTANTS.jmsConnectionFactoryDescription())
                .setMaster("Connection Factories", factoryTable)
                .setMasterTools(tools)
                .addDetail("Common", defaultAttributes.asWidget())
                .addDetail("Connection Management", connectionAttributes.asWidget());

        defaultAttributes.getForm().bind(factoryTable);
        defaultAttributes.getForm().setEnabled(false);

        connectionAttributes.getForm().bind(factoryTable);
        connectionAttributes.getForm().setEnabled(false);

        return layout.build();
    }

    public void setFactories(List<ActivemqConnectionFactory> factories) {
        factoryProvider.setList(factories);
        serverName.setText("Connection Factories: Provider " + presenter.getCurrentServer());
        factoryTable.selectDefaultEntity();
    }

    @SuppressWarnings("unchecked")
    public ActivemqConnectionFactory getSelectedFactory() {
        SingleSelectionModel<ActivemqConnectionFactory> selectionModel = (SingleSelectionModel<ActivemqConnectionFactory>) factoryTable
                .getSelectionModel();
        return selectionModel.getSelectedObject();
    }
}
