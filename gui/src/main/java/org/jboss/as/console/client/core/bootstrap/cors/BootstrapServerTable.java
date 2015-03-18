package org.jboss.as.console.client.core.bootstrap.cors;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;

import java.util.List;

/**
 * Widget which contains a cell table with the stored servers. There are buttons to add and remove server.
 *
 * @author Harald Pehl
 */
class BootstrapServerTable implements IsWidget {

    private static final int PAGE_SIZE = 10;

    private final BootstrapServerDialog serverDialog;
    private DefaultCellTable<BootstrapServer> cellTable;
    private ListDataProvider<BootstrapServer> dataProvider;
    private SingleSelectionModel<BootstrapServer> selectionModel;
    private BootstrapServerStore bootstrapServerStore;
    private BootstrapServer selectedServer;

    BootstrapServerTable(final BootstrapServerDialog serverDialog) {
        this.serverDialog = serverDialog;
        this.bootstrapServerStore = new BootstrapServerStore();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width");

        // toolbar
        ToolStrip topLevelTools = new ToolStrip();
        topLevelTools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                serverDialog.onConfigure();
            }
        }));
        topLevelTools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(), new ClickHandler() {
            @Override
            public void onClick(final ClickEvent event) {
                if (selectedServer != null) {
                    List<BootstrapServer> servers = bootstrapServerStore.remove(selectedServer);
                    dataProvider.setList(servers);
                    cellTable.selectDefaultEntity();
                }
            }
        }));
        layout.add(topLevelTools.asWidget());

        // table
        ProvidesKey<BootstrapServer> providesKey = new ProvidesKey<BootstrapServer>() {
            @Override
            public Object getKey(BootstrapServer item) {
                return item.getName();
            }
        };
        cellTable = new DefaultCellTable<>(PAGE_SIZE, providesKey);
        dataProvider = new ListDataProvider<>(providesKey);
        dataProvider.setList(bootstrapServerStore.load());
        dataProvider.addDataDisplay(cellTable);
        selectionModel = new SingleSelectionModel<>(dataProvider);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(final SelectionChangeEvent event) {
                selectedServer = ((SingleSelectionModel<BootstrapServer>) cellTable.getSelectionModel())
                        .getSelectedObject();
            }
        });
        cellTable.setSelectionModel(selectionModel);
        TextColumn<BootstrapServer> nameColumn = new TextColumn<BootstrapServer>() {
            @Override
            public String getValue(BootstrapServer record) {
                return record.getName();
            }
        };
        TextColumn<BootstrapServer> urlColumn = new TextColumn<BootstrapServer>() {
            @Override
            public String getValue(BootstrapServer record) {
                return BootstrapServerSetup.getServerUrl(record);
            }
        };
        cellTable.addColumn(nameColumn, Console.CONSTANTS.common_label_name());
        cellTable.addColumn(urlColumn, "URL");
        layout.add(cellTable);

        // pager
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(cellTable);
        layout.add(pager);

        return layout;
    }

    BootstrapServer getSelectedServer() {
        return selectedServer;
    }

    void select(BootstrapServer server) {
        selectionModel.setSelected(server, true);
    }

    DefaultCellTable<BootstrapServer> getCellTable() {
        return cellTable;
    }

    ListDataProvider<BootstrapServer> getDataProvider() {
        return dataProvider;
    }
}
