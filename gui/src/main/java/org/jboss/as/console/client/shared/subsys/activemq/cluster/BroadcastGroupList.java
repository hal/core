package org.jboss.as.console.client.shared.subsys.activemq.cluster;

import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.subsys.activemq.forms.BroadcastGroupForm;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 4/2/12
 */
public class BroadcastGroupList {

    public static final AddressTemplate BASE_ADDRESS =
            AddressTemplate.of("{selected.profile}/subsystem=messaging-activemq/server={activemq.server}/broadcast-group=*");

    private ContentHeaderLabel serverName;
    private MsgClusteringPresenter presenter;
    private BroadcastGroupForm defaultAttributes;

    private final DefaultCellTable<Property> table;
    private final ListDataProvider<Property> dataProvider;
    private final SingleSelectionModel<Property> selectionModel;

    public BroadcastGroupList(MsgClusteringPresenter presenter) {
        this.presenter = presenter;
        this.table = new DefaultCellTable<Property>(8);
        this.dataProvider = new ListDataProvider<Property>();
        this.dataProvider.addDataDisplay(table);
        selectionModel = new SingleSelectionModel<>();
        this.table.setSelectionModel(selectionModel);
    }

    @SuppressWarnings("unchecked")
    Widget asWidget() {
        serverName = new ContentHeaderLabel();

        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };


        table.addColumn(nameColumn, "Name");

        // defaultAttributes
        defaultAttributes = new BroadcastGroupForm(presenter);

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_add(),
                        clickEvent -> presenter.onLaunchAddResourceDialog(BASE_ADDRESS)));

        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_remove(), clickEvent -> Feedback.confirm(
                        Console.MESSAGES.deleteTitle("BroadcastGroup"),
                        Console.MESSAGES.deleteConfirm("BroadcastGroup " + getSelectedEntity().getName()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                presenter.onDeleteBroadcastGroup(getSelectedEntity().getName());
                            }
                        })));

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadlineWidget(serverName)
                .setDescription(
                        Console.CONSTANTS.broadcastDescription())
                .setMaster("BroadcastGroups", table)
                .setMasterTools(tools)
                .setDetail(Console.CONSTANTS.common_label_details(), defaultAttributes.asWidget());

        table.getSelectionModel().addSelectionChangeHandler(selectionChangeEvent -> {
            Property selection = selectionModel.getSelectedObject();
            if(selection!=null)
            {
                defaultAttributes.setData(selection);
            }
            else
            {
                defaultAttributes.getForm().clearValues();
            }
        });
        defaultAttributes.getForm().setEnabled(false);

        return layout.build();
    }

    public void setBroadcastGroups(List<Property> BroadcastGroups) {
        dataProvider.setList(BroadcastGroups);
        serverName.setText("BroadcastGroups: Provider " + presenter.getCurrentServer());

        table.selectDefaultEntity();

        // populate oracle
        presenter.loadExistingSocketBindings(new AsyncCallback<List<String>>() {
            @Override
            public void onFailure(Throwable throwable) {}

            @Override
            public void onSuccess(List<String> names) {
                defaultAttributes.setSocketBindings(names);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public Property getSelectedEntity() {
        SingleSelectionModel<Property> selectionModel = (SingleSelectionModel<Property>) table.getSelectionModel();
        return selectionModel.getSelectedObject();
    }
}
