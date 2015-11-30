/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.shared.runtime.logging.files;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.shared.runtime.logging.store.DownloadLogFile;
import org.jboss.as.console.client.shared.runtime.logging.store.LogStore;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeCellTable;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.dmr.client.ModelNode;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.gwt.dom.client.Style.Unit.PX;
import static com.google.gwt.user.client.ui.HasHorizontalAlignment.ALIGN_RIGHT;
import static org.jboss.as.console.client.shared.runtime.logging.store.LogStore.*;
import static org.jboss.as.console.client.shared.util.IdHelper.setId;

/**
 * @author Harald Pehl
 */
public class LogFilesTable extends Composite implements LogFilesId {

    private final static NumberFormat SIZE_FORMAT = NumberFormat.getFormat("0.00");

    private final ModelNodeCellTable table;
    private final TextColumn<ModelNode> nameColumn;
    private final ListDataProvider<ModelNode> dataProvider;
    private final SingleSelectionModel<ModelNode> selectionModel;
    private List<ModelNode> backup;

    @SuppressWarnings("unchecked")
    public LogFilesTable(final Dispatcher circuit, final LogFilesPresenter presenter) {

        VerticalPanel panel = new VerticalPanel();
        panel.addStyleName("rhs-content-panel");
        panel.getElement().getStyle().setMarginBottom(0, PX);

        // header
        panel.add(new ContentHeaderLabel("Log Viewer"));
        panel.add(new ContentDescription(Console.CONSTANTS.logFilesOfSelectedServer()));

        // toolbar
        ToolStrip tools = new ToolStrip();
        HTML label = new HTML(Console.CONSTANTS.commom_label_filter()+":&nbsp;");
        label.getElement().setAttribute("style", "padding-top:8px;");
        final TextBox filter = new TextBox();
        filter.setMaxLength(30);
        filter.setVisibleLength(20);
        filter.getElement().setAttribute("style", "float:right; width:120px;");
        filter.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent keyUpEvent) {
                String prefix = filter.getText();
                if (prefix != null && !prefix.equals("")) {
                    // filter by prefix
                    filterByPrefix(prefix);
                } else {
                    clearFilter();
                }
            }
        });
        setId(filter, BASE_ID, "filter");
        tools.addToolWidget(label);
        tools.addToolWidget(filter);

        final ToolButton download = new ToolButton("Download", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                ModelNode logFile = selectionModel.getSelectedObject();
                if (logFile != null) {
                    circuit.dispatch(new DownloadLogFile(logFile.get(FILE_NAME).asString()));
                }
            }
        });
        download.setEnabled(false);
        // actually the attribute 'stream' is relevant for download, however we need to pass an operation here
        download.setOperationAddress("/{implicit.host}/{selected.server}/subsystem=logging/log-file=*", "read-log-file");
        setId(download, BASE_ID, "download");
        tools.addToolButtonRight(download);

        final ToolButton view = new ToolButton(Console.CONSTANTS.common_label_view(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                ModelNode logFile = selectionModel.getSelectedObject();
                if (logFile != null) {
                    String name = logFile.get(FILE_NAME).asString();
                    int fileSize = logFile.get(FILE_SIZE).asInt();
                    presenter.onStreamLogFile(name, fileSize);
                }
            }
        });
        view.setEnabled(false);
        view.setOperationAddress("/{implicit.host}/{selected.server}/subsystem=logging/log-file=*", "read-log-file");
        setId(view, BASE_ID, "view");
        tools.addToolButtonRight(view);
        panel.add(tools);

        // table
        backup = new ArrayList<>();
        ProvidesKey<ModelNode> providesKey = new ProvidesKey<ModelNode>() {
            @Override
            public Object getKey(ModelNode item) {
                return item.get(FILE_NAME);
            }
        };
        selectionModel = new SingleSelectionModel<>(providesKey);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                download.setEnabled(selectionModel.getSelectedObject() != null);
                view.setEnabled(selectionModel.getSelectedObject() != null);
            }
        });
        table = new ModelNodeCellTable(10, providesKey);
        table.setSelectionModel(selectionModel);
        dataProvider = new ListDataProvider<>(providesKey);
        dataProvider.addDataDisplay(table);
        ColumnSortEvent.ListHandler<ModelNode> sortHandler = new ColumnSortEvent.ListHandler<ModelNode>(
                dataProvider.getList());
        table.addColumnSortHandler(sortHandler);
        // TODO Find a way to combine the double click handler with RBAC like
        // foo.setOperationAddress("/{implicit.host}/{selected.server}/subsystem=logging/log-file=*", "read-log-file");
//        table.addCellPreviewHandler(new CellPreviewEvent.Handler<ModelNode>() {
//            @Override
//            public void onCellPreview(CellPreviewEvent<ModelNode> event) {
//                NativeEvent nativeEvent = event.getNativeEvent();
//                if (BrowserEvents.DBLCLICK.equals(nativeEvent.getType())) {
//                    ModelNode selectedValue = event.getValue();
//
//                }
//            }
//        });
        panel.add(table);

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);
        panel.add(pager);

        // column: name
        nameColumn = new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode node) {
                return node.get(FILE_NAME).asString();
            }
        };
        nameColumn.setSortable(true);
        sortHandler.setComparator(nameColumn, new NameComparator());
        table.addColumn(nameColumn, "Log File Name");

        // column: last modified
        TextColumn<ModelNode> lastModifiedColumn = new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode node) {
                return node.get(LAST_MODIFIED_TIMESTAMP).asString();
            }
        };
        lastModifiedColumn.setSortable(true);
        sortHandler.setComparator(lastModifiedColumn, new Comparator<ModelNode>() {
            @Override
            public int compare(ModelNode node1, ModelNode node2) {
                return node1.get(LAST_MODIFIED_TIMESTAMP).asString().compareTo(node2.get(LAST_MODIFIED_TIMESTAMP).asString());
            }
        });
        table.addColumn(lastModifiedColumn, "Date - Time (UTC)");

        // column: size
        TextColumn<ModelNode> sizeColumn = new TextColumn<ModelNode>() {
            @Override
            public String getValue(ModelNode node) {
                double size = node.get(LogStore.FILE_SIZE).asLong() / 1048576.0;
                return SIZE_FORMAT.format(size);
            }
        };
        sizeColumn.setSortable(true);
        sizeColumn.setHorizontalAlignment(ALIGN_RIGHT);
        sortHandler.setComparator(sizeColumn, new Comparator<ModelNode>() {
            @Override
            public int compare(ModelNode node1, ModelNode node2) {
                return node1.get(FILE_SIZE).asInt() - node2.get(FILE_SIZE).asInt();
            }
        });
        table.addColumn(sizeColumn, "Size (MB)");

        ScrollPanel scroll = new ScrollPanel(panel);
        LayoutPanel layout = new LayoutPanel();
        layout.add(scroll);
        layout.setWidgetTopHeight(scroll, 0, PX, 100, Style.Unit.PCT);

        initWidget(layout);
    }

    public void list(List<ModelNode> files) {
        backup = files;
        table.setRowCount(files.size(), true);

        List<ModelNode> list = dataProvider.getList();
        list.clear(); // cannot call setList() as that breaks the sort handler
        Collections.sort(files, new NameComparator());
        list.addAll(files);

        // Make sure the new values are properly sorted
        table.getColumnSortList().push(nameColumn);
        ColumnSortEvent.fire(table, table.getColumnSortList());
    }

    private void filterByPrefix(String prefix) {
        final List<ModelNode> next = new ArrayList<>();
        for (ModelNode file : backup) {
            if (file.get(FILE_NAME).asString().toLowerCase().contains(prefix.toLowerCase()))
                next.add(file);
        }
        List<ModelNode> propList = dataProvider.getList();
        propList.clear(); // cannot call setList() as that breaks the sort handler
        propList.addAll(next);
    }

    private void clearFilter() {
        List<ModelNode> list = dataProvider.getList();
        list.clear(); // cannot call setList() as that breaks the sort handler
        list.addAll(backup);
    }


    private static class NameComparator implements Comparator<ModelNode> {
        @Override
        public int compare(ModelNode node1, ModelNode node2) {
            return node1.get(FILE_NAME).asString().compareTo(node2.get(FILE_NAME).asString());
        }
    }
}
