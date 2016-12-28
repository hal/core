/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.console.client.shared.runtime.activemq;

import java.util.List;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.shared.subsys.activemq.model.PreparedTransaction;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;

public class PreparedTransactionManagement {

    private ActivemqMetricPresenter presenter;

    private ListDataProvider<PreparedTransaction> dataProvider;
    private DefaultCellTable<PreparedTransaction> table;

    private ToolButton commitButton;
    private ToolButton rollbackButton;

    public PreparedTransactionManagement(ActivemqMetricPresenter presenter) {
        this.presenter = presenter;
    }

    Widget asWidget() {
        ToolStrip topLevelTools = new ToolStrip();
        commitButton = new ToolButton("Commit", event -> {
            if (getSelectedTransaction() == null) { return; }
            presenter.onCommit(getSelectedTransaction());
        });
        rollbackButton = new ToolButton("Rollback", event -> {
            if (getSelectedTransaction() == null) { return; }
            presenter.onRollback(getSelectedTransaction());
        });
        ToolButton refreshBtn = new ToolButton(Console.CONSTANTS.common_label_refresh(), event -> {
            presenter.loadTransactions();
        });
        commitButton.setEnabled(false);
        rollbackButton.setEnabled(false);
        topLevelTools.addToolButtonRight(commitButton);
        topLevelTools.addToolButtonRight(rollbackButton);
        topLevelTools.addToolButtonRight(refreshBtn);

        table = new DefaultCellTable<PreparedTransaction>(10, PreparedTransaction::getXid);

        dataProvider = new ListDataProvider<PreparedTransaction>();
        dataProvider.addDataDisplay(table);

        SingleSelectionModel<PreparedTransaction> selectionModel = new SingleSelectionModel<>();
        selectionModel.addSelectionChangeHandler(e -> {
            boolean isSelected = getSelectedTransaction() != null;
            commitButton.setEnabled(isSelected);
            rollbackButton.setEnabled(isSelected);
        });
        table.setSelectionModel(selectionModel);

        TextColumn<PreparedTransaction> xidColumn = new TextColumn<PreparedTransaction>() {
            @Override
            public String getValue(PreparedTransaction transaction) {
                return transaction.getXid();
            }
        };
        TextColumn<PreparedTransaction> dateColumn = new TextColumn<PreparedTransaction>() {
            @Override
            public String getValue(PreparedTransaction transaction) {
                return transaction.getDateString();
            }
        };

        table.addColumn(xidColumn, "Xid");
        table.addColumn(dateColumn, "Created");
        table.setColumnWidth(dateColumn, 120, Unit.PX);

        OneToOneLayout builder = new OneToOneLayout()
                .setPlain(true)
                .setTitle("Prepared Transactions")
                .setHeadlineWidget(new ContentHeaderLabel("Prepared Transactions"))
                .setDescription("Prepared transactions management.")
                .setMaster("", table.asWidget())
                .setMasterTools(topLevelTools.asWidget());

        return builder.build();
    }

    public void setTransactions(List<PreparedTransaction> transactions) {
        if (transactions.isEmpty()) {
            commitButton.setEnabled(false);
            rollbackButton.setEnabled(false);
        }
        dataProvider.setList(transactions);
        table.selectDefaultEntity();
    }

    private PreparedTransaction getSelectedTransaction() {
        return ((SingleSelectionModel<PreparedTransaction>) table.getSelectionModel()).getSelectedObject();
    }
}
