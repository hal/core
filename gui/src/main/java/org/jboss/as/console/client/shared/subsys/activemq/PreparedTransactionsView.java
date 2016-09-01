/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.console.client.shared.subsys.activemq;

import java.util.List;

import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.shared.subsys.activemq.model.PreparedTransaction;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class PreparedTransactionsView extends SuspendableViewImpl implements PreparedTransactionsPresenter.MyView {

    private PreparedTransactionsPresenter presenter;
    private ListDataProvider<PreparedTransaction> dataProvider;
    private DefaultCellTable<PreparedTransaction> table;

    private ContentHeaderLabel serverName;
    private ToolButton commitButton;
    private ToolButton rollbackButton;

    @Override
    public void setPresenter(PreparedTransactionsPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        serverName = new ContentHeaderLabel();
        serverName.setHTML("Prepared transactions: Provider " + presenter.getCurrentServer());

        ToolStrip topLevelTools = new ToolStrip();
        commitButton = new ToolButton("Commit", new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (getSelectedTransaction() == null) return;
                presenter.onCommit(getSelectedTransaction());
            }
        });
        rollbackButton = new ToolButton("Rollback", new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (getSelectedTransaction() == null) return;
                presenter.onRollback(getSelectedTransaction());
            }
        });
        commitButton.setEnabled(false);
        rollbackButton.setEnabled(false);
        topLevelTools.addToolButtonRight(commitButton);
        topLevelTools.addToolButtonRight(rollbackButton);

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
                .setTitle("Prepared Transactions")
                .setHeadlineWidget(serverName)
                .setDescription("Prepared transactions management.")
                .setMaster("", table.asWidget())
                .setMasterTools(topLevelTools.asWidget());

        return builder.build();
    }

    @Override
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
