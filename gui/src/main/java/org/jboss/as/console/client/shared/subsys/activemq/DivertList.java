package org.jboss.as.console.client.shared.subsys.activemq;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.subsys.activemq.forms.DivertForm;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqDivert;
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
public class DivertList {

    private ContentHeaderLabel serverName;
    private DefaultCellTable<ActivemqDivert> table;
    private ListDataProvider<ActivemqDivert> provider;
    private MsgDestinationsPresenter presenter;
    private DivertForm divertForm;

    public DivertList(MsgDestinationsPresenter presenter) {
        this.presenter = presenter;
    }

    @SuppressWarnings("unchecked")
    Widget asWidget() {
        serverName = new ContentHeaderLabel();

        table = new DefaultCellTable<>(10, ActivemqDivert::getRoutingName);
        provider = new ListDataProvider<>();
        provider.addDataDisplay(table);

        Column<ActivemqDivert, String> name = new Column<ActivemqDivert, String>(new TextCell()) {
            @Override
            public String getValue(ActivemqDivert object) {
                return object.getRoutingName();
            }
        };

        Column<ActivemqDivert, String> from = new Column<ActivemqDivert, String>(new TextCell()) {
            @Override
            public String getValue(ActivemqDivert object) {
                return object.getDivertAddress();
            }
        };

        Column<ActivemqDivert, String> to = new Column<ActivemqDivert, String>(new TextCell()) {
            @Override
            public String getValue(ActivemqDivert object) {
                return object.getForwardingAddress();
            }
        };

        table.addColumn(name, "Name");
        table.addColumn(from, "From");
        table.addColumn(to, "To");

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_add(), clickEvent -> presenter.launchNewDivertWizard()));

        tools.addToolButtonRight(
                new ToolButton(Console.CONSTANTS.common_label_remove(), clickEvent -> Feedback.confirm(
                        Console.MESSAGES.deleteTitle("Divert"),
                        Console.MESSAGES.deleteConfirm("Divert " + getSelectedEntity().getRoutingName()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                presenter.onDeleteDivert(getSelectedEntity().getRoutingName());
                            }
                        })));

        divertForm = new DivertForm(new FormToolStrip.FormCallback<ActivemqDivert>() {
            @Override
            public void onSave(Map<String, Object> changeset) {
                presenter.onSaveDivert(getSelectedEntity().getRoutingName(), changeset);
            }

            @Override
            public void onDelete(ActivemqDivert entity) {}
        });

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadlineWidget(serverName)
                .setDescription(
                        Console.CONSTANTS.divertDescription())
                .setMaster("Diverts", table)
                .setMasterTools(tools)
                .setDetail("Details", divertForm.asWidget());

        divertForm.getForm().bind(table);
        return layout.build();
    }

    public void setDiverts(List<ActivemqDivert> diverts) {
        provider.setList(diverts);
        serverName.setText("Diverts: Provider " + presenter.getCurrentServer());
        table.selectDefaultEntity();

        // populate oracle
        presenter.loadExistingQueueNames(new AsyncCallback<List<String>>() {
            @Override
            public void onFailure(Throwable throwable) {}

            @Override
            public void onSuccess(List<String> names) {
                divertForm.setQueueNames(names);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public ActivemqDivert getSelectedEntity() {
        SingleSelectionModel<ActivemqDivert> selectionModel = (SingleSelectionModel<ActivemqDivert>) table.getSelectionModel();
        return selectionModel.getSelectedObject();
    }
}
