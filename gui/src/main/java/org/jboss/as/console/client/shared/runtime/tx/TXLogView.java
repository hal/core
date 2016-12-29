package org.jboss.as.console.client.shared.runtime.tx;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.layout.FormLayout;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 2/27/13
 */
public class TXLogView extends SuspendableViewImpl implements TXLogPresenter.MyView {

    private TXLogPresenter presenter;

    private DefaultCellTable<TXRecord> table;
    private ListDataProvider<TXRecord> dataProvider;
    private final SingleSelectionModel<TXRecord> selectionModel;

    private ParticipantsPanel participantsPanel;

    public TXLogView() {

        ProvidesKey<TXRecord> providesKey = TXRecord::getId;
        this.selectionModel = new SingleSelectionModel<>(providesKey);
        table = new DefaultCellTable<>(8, providesKey);
        this.table.setSelectionModel(selectionModel);
        dataProvider = new ListDataProvider<>(providesKey);
        dataProvider.addDataDisplay(table);

        TextColumn<TXRecord> id = new TextColumn<TXRecord>() {
            @Override
            public String getValue(TXRecord record) {
                return record.getId();
            }
        };

        TextColumn<TXRecord> age = new TextColumn<TXRecord>() {
            @Override
            public String getValue(TXRecord record) {
                return record.getAge();
            }
        };


        table.addColumn(id, "ID");
        table.addColumn(age, "Age");

        participantsPanel = new ParticipantsPanel();
    }

    @Override
    public Widget createWidget() {


        // record details
        final Form<TXRecord> recordForm = new Form<TXRecord>(TXRecord.class);
        TextItem idItem = new TextItem("id", "ID");
        TextItem ageItem = new TextItem("age", "Age");
        TextItem type = new TextItem("type", "Type");

        recordForm.setFields(idItem, ageItem, type);

        FormHelpPanel helpText = new FormHelpPanel(new FormHelpPanel.AddressCallback() {
            @Override
            public ModelNode getAddress() {
                ModelNode address = RuntimeBaseAddress.get();
                address.add("subsystem", "transactions");
                address.add("log-store", "log-store");

                return address;
            }
        }, recordForm);

        FormLayout formPanel = new FormLayout()
                .setForm(recordForm)
                .setHelp(helpText);

        recordForm.bind(table);


        // top level tools

        ToolStrip tools = new ToolStrip();
        final ToolButton removeButton = new ToolButton(Console.CONSTANTS.common_label_delete(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                final TXRecord record = selectionModel.getSelectedObject();

                if (record != null) {
                    Feedback.confirm(
                            Console.MESSAGES.deleteTitle("TX Record"),
                            Console.MESSAGES.deleteConfirm(record.getId()),
                            new Feedback.ConfirmationHandler() {
                                @Override
                                public void onConfirmation(boolean confirmed) {
                                    if (confirmed) { presenter.onDeleteRecord(record); }
                                }
                            }
                    );
                }
            }
        });
        tools.addToolButtonRight(removeButton);

        // lazy load the participant details
        selectionModel.addSelectionChangeHandler(selectionChangeEvent -> {
            TXRecord selection = selectionModel.getSelectedObject();
            if (selection != null) {
                presenter.onLoadParticipants(selection);
            } else {
                participantsPanel.clear();
                recordForm.clearValues();
            }
        });

        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_refresh(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                presenter.onProbe(true);
            }
        }));

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setTitle("Transaction Manager")
                .setHeadline("Transaction Recovery Logs")
                .setDescription(Console.MESSAGES.transaction_log_description())
                .setMaster("Transactions", table)
                .setMasterTools(tools)
                .addDetail("Log Entry", formPanel.build())
                .addDetail("Participants", participantsPanel.asWidget());

        return layout.build();
    }

    @Override
    public void setPresenter(TXLogPresenter presenter) {
        this.presenter = presenter;
        participantsPanel.setPresenter(presenter);
    }

    @Override
    public void clear() {
        dataProvider.getList().clear();
        dataProvider.flush();
        participantsPanel.clear();
    }

    @Override
    public void updateFrom(List<TXRecord> records) {
        dataProvider.setList(records);
        table.selectDefaultEntity();
        if (records.isEmpty()) {
            selectionModel.clear();
            participantsPanel.clear();
        }
    }

    @Override
    public void updateParticpantsFrom(List<TXParticipant> records) {
        participantsPanel.updateParticpantsFrom(records);
    }
}
