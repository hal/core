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
import org.jboss.as.console.client.shared.subsys.activemq.forms.DiscoveryGroupForm;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqDiscoveryGroup;
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
public class DiscoveryGroupList {

    private ContentHeaderLabel serverName;
    private DefaultCellTable<ActivemqDiscoveryGroup> factoryTable;
    private ListDataProvider<ActivemqDiscoveryGroup> factoryProvider;
    private MsgClusteringPresenter presenter;
    private DiscoveryGroupForm defaultAttributes;

    public DiscoveryGroupList(MsgClusteringPresenter presenter) {
        this.presenter = presenter;
    }

    @SuppressWarnings("unchecked")
    Widget asWidget() {
        serverName = new ContentHeaderLabel();

        factoryTable = new DefaultCellTable<>(10, ActivemqDiscoveryGroup::getName);
        factoryProvider = new ListDataProvider<>();
        factoryProvider.addDataDisplay(factoryTable);

        Column<ActivemqDiscoveryGroup, String> nameColumn = new Column<ActivemqDiscoveryGroup, String>(new TextCell()) {
            @Override
            public String getValue(ActivemqDiscoveryGroup object) {
                return object.getName();
            }
        };

        factoryTable.addColumn(nameColumn, "Name");

        // defaultAttributes
        defaultAttributes = new DiscoveryGroupForm(presenter,
                new FormToolStrip.FormCallback<ActivemqDiscoveryGroup>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                        presenter.saveDiscoveryGroup(getSelectedEntity().getName(), changeset);
                    }

                    @Override
                    public void onDelete(ActivemqDiscoveryGroup entity) {}
                });

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_add(),
                        clickEvent -> presenter.launchNewDiscoveryGroupWizard()));

        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_remove(), clickEvent -> Feedback.confirm(
                        Console.MESSAGES.deleteTitle("Discovery Group"),
                        Console.MESSAGES.deleteConfirm("Discovery Group " + getSelectedEntity().getName()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                presenter.onDeleteDiscoveryGroup(getSelectedEntity().getName());
                            }
                        })));

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadlineWidget(serverName)
                .setDescription(
                        Console.CONSTANTS.discoveryGroupDescription())
                .setMaster("DiscoveryGroups", factoryTable)
                .setMasterTools(tools)
                .setDetail("Details", defaultAttributes.asWidget());

        defaultAttributes.getForm().bind(factoryTable);
        defaultAttributes.getForm().setEnabled(false);

        return layout.build();
    }

    public void setDiscoveryGroups(List<ActivemqDiscoveryGroup> DiscoveryGroups) {
        factoryProvider.setList(DiscoveryGroups);
        serverName.setText("Discovery Groups: Provider " + presenter.getCurrentServer());

        factoryTable.selectDefaultEntity();
    }

    @SuppressWarnings("unchecked")
    public ActivemqDiscoveryGroup getSelectedEntity() {
        SingleSelectionModel<ActivemqDiscoveryGroup> selectionModel = (SingleSelectionModel<ActivemqDiscoveryGroup>) factoryTable
                .getSelectionModel();
        return selectionModel.getSelectedObject();
    }
}
