package org.jboss.as.console.client.shared.general;

import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.general.model.SocketGroup;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.widgets.tables.ViewLinkCell;
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
 * @date 1/17/12
 */
public class SocketGroupList {

    private SocketBindingPresenter presenter;
    private CellTable<SocketGroup> table;
    private ListDataProvider<SocketGroup> dataProvider;
    private String token;
    private SingleSelectionModel<SocketGroup> selectionModel;

    public SocketGroupList(SocketBindingPresenter presenter, String token) {
        this.presenter = presenter;
        this.token = token;
    }

    public Widget asWidget() {

        ToolStrip toolstrip = new ToolStrip();
        ToolButton addBtn = new ToolButton(Console.CONSTANTS.common_label_add(), new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                presenter.launchNewGroupDialogue();
            }
        });
        addBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_add_socketBindingView());
        toolstrip.addToolButtonRight(addBtn);

        ToolButton removeBtn = new ToolButton(Console.CONSTANTS.common_label_delete(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final SocketGroup editedEntity = selectionModel.getSelectedObject();
                if(editedEntity!=null) {
                    Feedback.confirm(
                            Console.MESSAGES.deleteTitle("Socket Binding Group"),
                            Console.MESSAGES.deleteConfirm("Socket Binding Group " + editedEntity.getName()),
                            new Feedback.ConfirmationHandler() {
                                @Override
                                public void onConfirmation(boolean isConfirmed) {
                                    if (isConfirmed)
                                        presenter.onDeleteGroup(editedEntity.getName());
                                }
                            });
                }
            }
        });
        removeBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_remove_socketBindingView());
        toolstrip.addToolButtonRight(removeBtn);


        ToolButton cloneBtn = new ToolButton("Clone", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final SocketGroup editedEntity = selectionModel.getSelectedObject();
                if(editedEntity!=null) {
                    presenter.launchCloneDialoge(editedEntity.getName());
                }
            }
        });
        toolstrip.add(cloneBtn);

        table = new DefaultCellTable<SocketGroup>(5);
        dataProvider = new ListDataProvider<SocketGroup>();
        dataProvider.addDataDisplay(table);

        TextColumn<SocketGroup> nameColumn = new TextColumn<SocketGroup>() {
            @Override
            public String getValue(SocketGroup record) {
                return record.getName();
            }
        };

        Column<SocketGroup, SocketGroup> option = new Column<SocketGroup, SocketGroup>(
                new ViewLinkCell<SocketGroup>(Console.CONSTANTS.common_label_view(), new ActionCell.Delegate<SocketGroup>() {
                    @Override
                    public void execute(SocketGroup selection) {
                        presenter.getPlaceManager().revealPlace(
                                new PlaceRequest(token).with("name", selection.getName())
                        );
                    }
                })
        ) {
            @Override
            public SocketGroup getValue(SocketGroup manager) {
                return manager;
            }
        };

        table.addColumn(nameColumn, "Name");
        table.addColumn(option, "Option");

        selectionModel = new SingleSelectionModel<>();
        table.setSelectionModel(selectionModel);

        VerticalPanel formPanel = new VerticalPanel();
        formPanel.setStyleName("fill-layout-width");

        final Form<SocketGroup> form = new Form<>(SocketGroup.class);
        TextItem name = new TextItem("name", "Name");
        TextItem intf = new TextItem("defaultInterface", "Default Interface");

        form.setFields(name, intf);

        FormHelpPanel helpPanel = new FormHelpPanel(new FormHelpPanel.AddressCallback() {
            @Override
            public ModelNode getAddress() {
                ModelNode address = new ModelNode();
                address.add("socket-binding-group", "*");
                return address;
            }
        }, form);

        formPanel.add(helpPanel.asWidget());
        formPanel.add(form.asWidget());

        form.bind(table);
        form.setEnabled(false);

        // ----
        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline("Socket Binding Groups")
                .setDescription(Console.MESSAGES.pleaseChoseanItem())
                .setMaster("", table);

        if(!Console.MODULES.getBootstrapContext().isStandalone()) {
            layoutBuilder.setMasterTools(toolstrip.asWidget());
        }

        layoutBuilder.addDetail("Attributes", form.asWidget());

        return layoutBuilder.build();
    }

    public void setGroups(List<SocketGroup> adapters) {
        dataProvider.setList(adapters);

        if(!adapters.isEmpty())
            table.getSelectionModel().setSelected(adapters.get(0), true);

    }
}
