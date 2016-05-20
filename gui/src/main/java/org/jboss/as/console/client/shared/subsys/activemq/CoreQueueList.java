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

import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqCoreQueue;
import org.jboss.ballroom.client.widgets.ContentGroupLabel;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Claudio Miranda
 */
public class CoreQueueList {

    private DefaultCellTable<ActivemqCoreQueue> queueTable;
    private ListDataProvider<ActivemqCoreQueue> queueProvider;

    private MsgDestinationsPresenter presenter;

    public CoreQueueList(MsgDestinationsPresenter presenter) {
        this.presenter = presenter;
    }

    @SuppressWarnings("unchecked")
    Widget asWidget() {

        VerticalPanel layout = new VerticalPanel();

        Form<ActivemqCoreQueue> form = new Form<>(ActivemqCoreQueue.class);

        ToolStrip tableTools = new ToolStrip();
        ToolButton addBtn = new ToolButton(Console.CONSTANTS.common_label_add(),
                event -> presenter.launchNewCoreQueueDialogue());
        addBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_add_queueList());
        tableTools.addToolButtonRight(addBtn);

        ToolButton removeBtn = new ToolButton(Console.CONSTANTS.common_label_delete(), event -> Feedback.confirm(
                Console.MESSAGES.deleteTitle("Queue"),
                Console.MESSAGES.deleteConfirm("queue: " + form.getEditedEntity().getName()),
                isConfirmed -> {
                    if (isConfirmed) { presenter.onDeleteCoreQueue(form.getEditedEntity()); }
                }));

        tableTools.addToolButtonRight(removeBtn);

        layout.add(tableTools.asWidget());

        queueTable = new DefaultCellTable<>(8, ActivemqCoreQueue::getQueueAddress);
        queueProvider = new ListDataProvider<>();
        queueProvider.addDataDisplay(queueTable);

        TextColumn<ActivemqCoreQueue> nameColumn = new TextColumn<ActivemqCoreQueue>() {
            @Override
            public String getValue(ActivemqCoreQueue record) {
                return record.getName();
            }
        };
        TextColumn<ActivemqCoreQueue> addressColumn = new TextColumn<ActivemqCoreQueue>() {
            @Override
            public String getValue(ActivemqCoreQueue record) {
                return record.getQueueAddress();
            }
        };

        queueTable.addColumn(nameColumn, "Name");
        queueTable.addColumn(addressColumn, "Address");

        layout.add(queueTable);

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(queueTable);
        layout.add(pager);

        pager.getElement().setAttribute("style", "margin-bottom:15px;");

        TextItem name = new TextItem("name", "Name");
        TextItem queueAddress = new TextItem("queueAddress", "Address");
        TextItem filter = new TextItem("filter", "Filter");
        CheckBoxItem durable = new CheckBoxItem("durable", "Durable");

        // Queues are read-only after creation
        form.setFields(name, queueAddress, filter, durable);
        form.bind(queueTable);
        form.setEnabled(false);

        final FormHelpPanel helpPanel = new FormHelpPanel(() -> {
            ModelNode address = Baseadress.get();
            address.add("subsystem", "messaging-activemq");
            address.add("server", presenter.getCurrentServer());
            address.add("queue", "*");
            return address;
        }, form);

        layout.add(new ContentGroupLabel("Queues are read-only after creation"));
        layout.add(helpPanel.asWidget());
        layout.add(form.asWidget());

        return layout;
    }

    void setQueues(List<ActivemqCoreQueue> queues) {
        queueProvider.setList(queues);
        queueTable.selectDefaultEntity();
    }
}
