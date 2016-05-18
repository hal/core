package org.jboss.as.console.client.shared.subsys.activemq.cluster;

import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.subsys.activemq.forms.DiscoveryGroupForm;
import org.jboss.as.console.client.shared.subsys.activemq.forms.DiscoveryGroupFormValidator;
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
public class DiscoveryGroupList {

    public static final AddressTemplate BASE_ADDRESS =
                        AddressTemplate.of("{selected.profile}/subsystem=messaging-activemq/server={activemq.server}/discovery-group=*");
    private final SingleSelectionModel<Property> selectionModel;
    private final DefaultCellTable<Property> table;
    private final ListDataProvider<Property> dataProvider;

    private ContentHeaderLabel serverName;
    private MsgClusteringPresenter presenter;
    private DiscoveryGroupForm defaultAttributes;

    public DiscoveryGroupList(MsgClusteringPresenter presenter) {
        this.presenter = presenter;

        this.table = new DefaultCellTable<>(8);
        this.dataProvider = new ListDataProvider<>();
        this.dataProvider.addDataDisplay(table);
        this.selectionModel = new SingleSelectionModel<>();
        this.table.setSelectionModel(this.selectionModel);
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
        defaultAttributes = new DiscoveryGroupForm(presenter);

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_add(),
                        clickEvent -> presenter.onLaunchAddResourceDialog(BASE_ADDRESS, new DiscoveryGroupFormValidator())));

        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_remove(), clickEvent -> Feedback.confirm(
                        Console.MESSAGES.deleteTitle("DiscoveryGroup"),
                        Console.MESSAGES.deleteConfirm("DiscoveryGroup " + getSelectedEntity().getName()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                presenter.onDeleteDiscoveryGroup(getSelectedEntity().getName());
                            }
                        })));

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadlineWidget(serverName)
                .setDescription(
                        Console.CONSTANTS.clusterConnectionDescription())
                .setMaster("DiscoveryGroups", table)
                .setMasterTools(tools)
                .setDetail("Details", defaultAttributes.asWidget());

        defaultAttributes.getForm().bind(table);

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

    public void setDiscoveryGroups(List<Property> DiscoveryGroups) {
        dataProvider.setList(DiscoveryGroups);
        serverName.setText("DiscoveryGroups: Provider " + presenter.getCurrentServer());

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
