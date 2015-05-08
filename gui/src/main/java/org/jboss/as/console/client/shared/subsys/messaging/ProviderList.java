package org.jboss.as.console.client.shared.subsys.messaging;

import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.widgets.tables.ViewLinkCell;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.Property;

import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 * @date 1/17/12
 */
public class ProviderList  {

    private CommonMsgPresenter presenter;
    private DefaultCellTable table;
    private ListDataProvider<Property> dataProvider;
    private MessagingProviderEditor providerEditor;
    private String token;

    private final static AddressTemplate ADDRESS = AddressTemplate.of(
            "{selected.profile}/subsystem=messaging/hornetq-server=*"
    );

    private final static String[] COMMON = new String[] {
            "allow-failback",
            "backup",
            "backup-group-name",
            "check-for-live-server",
            "management-address",
            "management-notification-address",
            "statistics-enabled",
            "thread-pool-max-size",
            "scheduled-thread-pool-max-size",
            "transaction-timeout",
            "transaction-timeout-scan-period",
            "wild-card-routing-enabled",
            "persistence-enabled",
            "persist-id-cache",
            "failover-on-shutdown",

    };

    private final static String[] SECURITY = new String[] {
            "security-domain",
            "security-enabled",
            "security-invalidation-interval",
            "cluster-user",
            "cluster-password"
    };

    private final static String[] JOURNAL = new String[] {
            "journal-buffer-size",
            "journal-buffer-timeout",
            "journal-compact-min-files",
            "journal-compact-percentage",
            "journal-file-size",
            "journal-max-io",
            "journal-min-files",
            "journal-sync-non-transactional",
            "journal-sync-transactional",
            "journal-type",
            "create-journal-dir"
    };

    public ProviderList(CommonMsgPresenter presenter, String token) {

        this.presenter = presenter;
        this.token = token;
        this.table = new DefaultCellTable(5);
        this.dataProvider = new ListDataProvider<Property>();
        this.dataProvider.addDataDisplay(table);
        this.table.setSelectionModel(new SingleSelectionModel<Property>());
    }

    public Widget asWidget() {


        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };

        Column<Property, String> option = new Column<Property, String>(
                new ViewLinkCell<String>(Console.CONSTANTS.common_label_view(), new ActionCell.Delegate<String>() {
                    @Override
                    public void execute(String selection) {
                        presenter.getPlaceManager().revealPlace(
                                new PlaceRequest(token).with("name", selection)
                        );
                    }
                })
        ) {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };

        table.addColumn(nameColumn, "Name");
        table.addColumn(option, "Option");

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.launchAddProviderDialog();
            }
        }));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Feedback.confirm(Console.MESSAGES.deleteTitle("Messaging Provider"),
                        Console.MESSAGES.deleteConfirm("Messaging Provider '"+getCurrentSelection().getName()+"'"),
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    presenter.removeProvider(getCurrentSelection().getName());
                                }
                            }
                        });
            }
        }));


        final SecurityContext securityContext =
                presenter.getSecurityFramework().getSecurityContext(
                        presenter.getNameToken()
                );

        final ResourceDescription definition = presenter.getDescriptionRegistry().lookup(ADDRESS);

        final FormCallback callback = new FormCallback() {
            @Override
            public void onSave(Map changeset) {
                presenter.onSaveProvider(getCurrentSelection().getName(), changeset);
            }

            @Override
            public void onCancel(Object entity) {

            }
        };

        // common
        final ModelNodeFormBuilder.FormAssets commonForm = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .include(COMMON)
                .setSecurityContext(securityContext).build();

        commonForm.getForm().setToolsCallback(callback);

        // security
        final ModelNodeFormBuilder.FormAssets secForm = new ModelNodeFormBuilder()
                .setConfigOnly()
                .include(SECURITY)
                .setResourceDescription(definition)
                .setSecurityContext(securityContext).build();

        secForm.getForm().setToolsCallback(callback);

         // journal
        final ModelNodeFormBuilder.FormAssets journalForm = new ModelNodeFormBuilder()
                .setConfigOnly()
                .include(JOURNAL)
                .setResourceDescription(definition)
                .setSecurityContext(securityContext).build();

        journalForm.getForm().setToolsCallback(callback);


        // ----
        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline("JMS Messaging Provider")
                .setDescription("Please chose a provider from below for specific settings.")
                .setMasterTools(tools)
                .setMaster(Console.MESSAGES.available("Messaging Provider"), table)
                .addDetail("Attributes", commonForm.asWidget())
                .addDetail("Security", secForm.asWidget())
                .addDetail("Journal", journalForm.asWidget());


        final SingleSelectionModel<Property> selectionModel = new SingleSelectionModel<Property>();
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Property server = selectionModel.getSelectedObject();
                if(server!=null)
                {
                    commonForm.getForm().edit(server.getValue());
                    secForm.getForm().edit(server.getValue());
                    journalForm.getForm().edit(server.getValue());
                }
                else
                {
                    commonForm.getForm().clearValues();
                    secForm.getForm().clearValues();
                    journalForm.getForm().clearValues();
                }
            }
        });
        table.setSelectionModel(selectionModel);
        return layoutBuilder.build();
    }


    private Property getCurrentSelection() {
        Property selection = ((SingleSelectionModel<Property>) table.getSelectionModel()).getSelectedObject();
        return selection;
    }

    public void setProvider(List<Property> provider) {
        dataProvider.setList(provider);
        table.selectDefaultEntity();

    }
}
