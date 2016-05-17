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
import java.util.Map;

import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqJMSEndpoint;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqJMSQueue;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.as.console.client.widgets.forms.items.JndiNamesItem;
import org.jboss.ballroom.client.widgets.ContentGroupLabel;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.ListItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 */
public class JMSQueueList {

    private DefaultCellTable<ActivemqJMSQueue> queueTable;
    private ListDataProvider<ActivemqJMSQueue> queueProvider;

    private MsgDestinationsPresenter presenter;
    private Form<ActivemqJMSQueue> form;

    public JMSQueueList(MsgDestinationsPresenter presenter) {
        this.presenter = presenter;
    }

    @SuppressWarnings("unchecked")
    Widget asWidget() {

        VerticalPanel layout = new VerticalPanel();

        form = new Form<>(ActivemqJMSQueue.class);

        ToolStrip tableTools = new ToolStrip();
        ToolButton addBtn = new ToolButton(Console.CONSTANTS.common_label_add(),
                event -> presenter.launchNewJMSQueueDialogue());
        addBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_add_queueList());
        tableTools.addToolButtonRight(addBtn);

        ToolButton removeBtn = new ToolButton(Console.CONSTANTS.common_label_delete(), event -> Feedback.confirm(
                Console.MESSAGES.deleteTitle("Queue"),
                Console.MESSAGES.deleteConfirm("Queue"),
                isConfirmed -> {
                    if (isConfirmed) { presenter.onDeleteJMSQueue(form.getEditedEntity()); }
                }));

        tableTools.addToolButtonRight(removeBtn);

        layout.add(tableTools.asWidget());

        queueTable = new DefaultCellTable<>(8, ActivemqJMSEndpoint::getEntries);
        queueProvider = new ListDataProvider<>();
        queueProvider.addDataDisplay(queueTable);

        TextColumn<ActivemqJMSQueue> nameColumn = new TextColumn<ActivemqJMSQueue>() {
            @Override
            public String getValue(ActivemqJMSQueue record) {
                return record.getName();
            }
        };

        JMSEndpointJndiColumn<ActivemqJMSQueue> jndiColumn = new JMSEndpointJndiColumn<>();

        queueTable.addColumn(nameColumn, "Name");
        queueTable.addColumn(jndiColumn, "JNDI");

        layout.add(queueTable);

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(queueTable);
        layout.add(pager);

        pager.getElement().setAttribute("style", "margin-bottom:15px;");

        TextItem name = new TextItem("name", "Name");
        ListItem jndi = new JndiNamesItem("entries", "JNDI Names");

        CheckBoxItem durable = new CheckBoxItem("durable", "Durable?");
        TextBoxItem selector = new TextBoxItem("selector", "Selector") {
            @Override
            public boolean isUndefined() {
                return getValue().equals("");
            }

            @Override
            public boolean isRequired() {
                return false;
            }
        };
        durable.setEnabled(false);
        selector.setEnabled(false);

        form.setFields(name, jndi, durable, selector);
        form.bind(queueTable);

        final FormHelpPanel helpPanel = new FormHelpPanel(() -> {
            ModelNode address = Baseadress.get();
            address.add("subsystem", "messaging-activemq");
            address.add("server", presenter.getCurrentServer());
            address.add("jms-queue", "*");
            return address;
        }, form);

        // this is enough to AbstractForm to add the Edit button, as the form adds the callback 
        FormToolStrip<ActivemqJMSQueue> formTools = new FormToolStrip<>(
                form,
                new FormToolStrip.FormCallback<ActivemqJMSQueue>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                        presenter.onSaveJMSQueue(form.getEditedEntity().getName(), changeset);
                    }

                    @Override
                    public void onDelete(ActivemqJMSQueue entity) {}
                }
        );

        layout.add(new ContentGroupLabel("Queues are read-only after creation"));
        layout.add(helpPanel.asWidget());
        layout.add(form.asWidget());

        return layout;
    }

    void setQueues(List<ActivemqJMSQueue> queues) {
        queueProvider.setList(queues);
        queueTable.selectDefaultEntity();
        form.setEnabled(false);
    }

    public void setEnabled(boolean b) {
        form.setEnabled(b);
    }
}
