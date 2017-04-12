package org.jboss.as.console.client.shared.subsys.activemq.connections;

import java.util.List;
import java.util.Map;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqBridge;
import org.jboss.as.console.client.shared.subsys.elytron.CredentialReferenceAlternativesFormValidation;
import org.jboss.as.console.client.shared.subsys.elytron.CredentialReferenceFormValidation;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.as.console.mbui.widgets.ComplexAttributeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.forms.PasswordBoxItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.MESSAGING_QUEUES;
import static org.jboss.as.console.client.shared.subsys.activemq.connections.MsgConnectionsPresenter.MESSAGING_SERVER;
import static org.jboss.dmr.client.ModelDescriptionConstants.CREDENTIAL_REFERENCE;

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
    private ModelNodeFormBuilder.FormAssets defaultAttributes;
    private ModelNodeFormBuilder.FormAssets connectionAttributes;
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
        defaultAttributes = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setSecurityContext(securityContext)
                .setResourceDescription(bridgeResourceDescription)
                .include("queue-name", "forwarding-address", "discovery-group", "static-connectors", "filter", "transformer-class-name")
                .createValidators(true)
                .requiresAtLeastOne("discovery-group", "static-connectors")
                .addFactory("queue-name", attributeDescription ->  {
                    SuggestionResource suggestionResource = new SuggestionResource("queue-name", "Queue Name", true,
                            Console.MODULES.getCapabilities().lookup(MESSAGING_QUEUES));
                    return suggestionResource.buildFormItem();
                })
                .build();

        defaultAttributes.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(final Map changeset) {
                presenter.onSaveBridge(selectionModel.getSelectedObject().getName(), changeset);
            }

            @Override
            public void onCancel(final Object entity) {
                defaultAttributes.getForm().cancel();
            }
        });

        connectionAttributes = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setSecurityContext(securityContext)
                .setResourceDescription(bridgeResourceDescription)
                .include("user", "password", "retry-interval", "reconnect-attempts")
                .createValidators(true)
                .addFactory("password", attributeDescription ->  new PasswordBoxItem("password", "Password", false))
                .build();

        connectionAttributes.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(final Map changeset) {
                presenter.onSaveBridge(selectionModel.getSelectedObject().getName(), changeset);
            }

            @Override
            public void onCancel(final Object entity) {
                connectionAttributes.getForm().cancel();
            }
        });

        credentialRefFormAsset = new ComplexAttributeForm(CREDENTIAL_REFERENCE, securityContext,
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
        credentialRefFormAsset.getForm().addFormValidator(new CredentialReferenceFormValidation());

        connectionAttributes.getForm().addFormValidator(new CredentialReferenceAlternativesFormValidation("password", credentialRefFormAsset.getForm(), "Credential Reference", true));
        credentialRefFormAsset.getForm().addFormValidator(new CredentialReferenceAlternativesFormValidation("password", connectionAttributes.getForm(), "Connection Management", false));


        table.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(event -> {
            ActivemqBridge activemqBridge = selectionModel.getSelectedObject();
            if (activemqBridge != null && activemqBridge.getName() != null) {

                ModelNode bridgeModel = presenter.getBridgeAdapter().fromEntity(activemqBridge);
                for (String connector: activemqBridge.getStaticConnectors()) {
                    bridgeModel.get("static-connectors").add(connector);
                }
                defaultAttributes.getForm().edit(bridgeModel);
                connectionAttributes.getForm().edit(bridgeModel);

                if (activemqBridge.getCredentialReference() != null) {
                    ModelNode credentialBean = presenter.getCredentialReferenceAdapter()
                            .fromEntity(activemqBridge.getCredentialReference());
                    credentialRefFormAsset.getForm().edit(credentialBean);
                } else {
                    credentialRefFormAsset.getForm().editTransient(new ModelNode());
                }

            } else {
                defaultAttributes.getForm().clearValues();
                connectionAttributes.getForm().clearValues();
                credentialRefFormAsset.getForm().clearValues();
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

        return layout.build();
    }

    public void setBridges(List<ActivemqBridge> bridges) {
        dataProvider.setList(bridges);
        serverName.setText("Bridges: Provider " + presenter.getCurrentServer());
        table.selectDefaultEntity();
        credentialRefFormAsset.getForm().clearValues();
        defaultAttributes.getForm().clearValues();
        connectionAttributes.getForm().clearValues();
        if (bridges.size() == 0)
            selectionModel.clear();
        SelectionChangeEvent.fire(selectionModel);
    }

}
