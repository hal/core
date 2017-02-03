package org.jboss.as.console.client.shared.subsys.activemq.connections;

import java.util.List;
import java.util.Map;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.subsys.activemq.forms.BridgeConnectionsForm;
import org.jboss.as.console.client.shared.subsys.activemq.forms.DefaultBridgeForm;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqBridge;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.as.console.mbui.widgets.ComplexAttributeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;

import static org.jboss.as.console.client.shared.subsys.activemq.connections.MsgConnectionsPresenter.MESSAGING_SERVER;

/**
 * @author Heiko Braun
 * @date 4/2/12
 */
public class BridgesList {

    protected SingleSelectionModel<ActivemqBridge> selectionModel;
    private ContentHeaderLabel serverName;
    private DefaultCellTable<ActivemqBridge> table;
    private ListDataProvider<ActivemqBridge> dataProvider;
    private MsgConnectionsPresenter presenter;
    private DefaultBridgeForm defaultAttributes;
    private BridgeConnectionsForm connectionAttributes;
    private ModelNodeFormBuilder.FormAssets credentialRefFormAsset;

    public BridgesList(MsgConnectionsPresenter presenter) {
        this.presenter = presenter;
    }

    @SuppressWarnings("unchecked")
    Widget asWidget() {
        serverName = new ContentHeaderLabel();

        SecurityContext securityContext = presenter.getSecurityFramework()
                .getSecurityContext(presenter.getProxy().getNameToken());
        // r-r-d the parent server, because mgmt op doesn't work to read resource with 2 wildcards
        // as blabla/server=*/pooled-connection-factory=*, see WFCORE-2022
        ResourceDescription bridgeResourceDescription = presenter.getDescriptionRegistry().lookup(MESSAGING_SERVER);
        bridgeResourceDescription = bridgeResourceDescription.getChildDescription("bridge");


        ProvidesKey<ActivemqBridge> providesKey = ActivemqBridge::getName;
        table = new DefaultCellTable<>(10, providesKey);
        dataProvider = new ListDataProvider<>(providesKey);
        selectionModel = new SingleSelectionModel<>(providesKey);
        dataProvider.addDataDisplay(table);

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

        table.addColumn(nameColumn, "Name");
        table.addColumn(queueColumn, "Queue");
        table.addColumn(toColumn, "Forward");

        // defaultAttributes
        defaultAttributes = new DefaultBridgeForm(presenter,
                new FormToolStrip.FormCallback<ActivemqBridge>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                        presenter.onSaveBridge(selectionModel.getSelectedObject().getName(), changeset);
                    }

                    @Override
                    public void onDelete(ActivemqBridge entity) {
                    }
                });

        connectionAttributes = new BridgeConnectionsForm(presenter,
                new FormToolStrip.FormCallback<ActivemqBridge>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                        presenter.onSaveBridge(selectionModel.getSelectedObject().getName(), changeset);
                    }

                    @Override
                    public void onDelete(ActivemqBridge entity) {
                    }
                });

        credentialRefFormAsset = new ComplexAttributeForm("credential-reference", securityContext,
                bridgeResourceDescription).build();

        credentialRefFormAsset.getForm().setToolsCallback(new FormCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onSave(final Map changeset) {
                ModelNode updatedEntity = credentialRefFormAsset.getForm().getUpdatedEntity();
                presenter.saveAttribute(selectionModel.getSelectedObject().getName(), updatedEntity);
            }

            @Override
            public void onCancel(final Object entity) {
                credentialRefFormAsset.getForm().cancel();
            }
        });

        table.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(event -> {
            ActivemqBridge activemqBridge = selectionModel.getSelectedObject();
            if (activemqBridge != null && activemqBridge.getCredentialReference() != null) {
                ModelNode credentialBean = presenter.getCredentialReferenceAdapter()
                        .fromEntity(activemqBridge.getCredentialReference());
                defaultAttributes.getForm().edit(activemqBridge);
                connectionAttributes.getForm().edit(activemqBridge);
                credentialRefFormAsset.getForm().edit(credentialBean);
            } else {
                defaultAttributes.getForm().clearValues();
                connectionAttributes.getForm().clearValues();
                credentialRefFormAsset.getForm().edit(new ModelNode());
            }
        });


        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_add(), clickEvent -> presenter.launchNewBridgeWizard()));

        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_remove(), clickEvent -> Feedback.confirm(
                        Console.MESSAGES.deleteTitle("Bridge"),
                        Console.MESSAGES.deleteConfirm("Bridge " + selectionModel.getSelectedObject().getName()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                presenter.onDeleteBridge(selectionModel.getSelectedObject().getName());
                            }
                        })));

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadlineWidget(serverName)
                .setDescription(Console.CONSTANTS.bridgeDescription())
                .setMaster("Bridges", table)
                .setMasterTools(tools)
                .addDetail("Common", defaultAttributes.asWidget())
                .addDetail("Connection Management", connectionAttributes.asWidget())
                .addDetail("Credential Reference", credentialRefFormAsset.asWidget());

        defaultAttributes.getForm().bind(table);
        defaultAttributes.getForm().setEnabled(false);

        connectionAttributes.getForm().bind(table);
        connectionAttributes.getForm().setEnabled(false);

        return layout.build();
    }

    public void setBridges(List<ActivemqBridge> bridges) {
        dataProvider.setList(bridges);
        serverName.setText("Bridges: Provider " + presenter.getCurrentServer());

        table.selectDefaultEntity();

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
        credentialRefFormAsset.getForm().clearValues();
        defaultAttributes.getForm().clearValues();
        connectionAttributes.getForm().clearValues();
        SelectionChangeEvent.fire(selectionModel);
    }

}
