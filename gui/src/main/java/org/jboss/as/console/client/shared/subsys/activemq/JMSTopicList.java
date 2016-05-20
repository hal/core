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

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqJMSEndpoint;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqJMSTopic;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.as.console.client.widgets.forms.items.JndiNamesItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.ListItem;
import org.jboss.ballroom.client.widgets.forms.TextItem;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @date 5/10/11
 */
public class JMSTopicList {

    private EndpointTable table;
    private ListDataProvider<ActivemqJMSEndpoint> endpointProvider;

    private MsgDestinationsPresenter presenter;
    private Form<ActivemqJMSTopic> form;

    public JMSTopicList(MsgDestinationsPresenter presenter) {
        this.presenter = presenter;
    }

    @SuppressWarnings("unchecked")
    Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();

        form = new Form<>(ActivemqJMSTopic.class);
        form.setEnabled(false);
        form.setNumColumns(2);

        FormToolStrip<ActivemqJMSTopic> formTools = new FormToolStrip<>(
                form,
                new FormToolStrip.FormCallback<ActivemqJMSTopic>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                        presenter.onSaveTopic(form.getEditedEntity().getName(), form.getChangedValues());
                    }

                    @Override
                    public void onDelete(ActivemqJMSTopic entity) {}
                }
        );

        ToolStrip tableTools = new ToolStrip();

        ToolButton addBtn = new ToolButton(Console.CONSTANTS.common_label_add(),
                event -> presenter.launchNewTopicDialogue());
        addBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_add_topicList());
        tableTools.addToolButtonRight(addBtn);

        ToolButton removeBtn = new ToolButton(Console.CONSTANTS.common_label_delete(), event -> Feedback.confirm(
                Console.MESSAGES.deleteTitle("Topic"),
                Console.MESSAGES.deleteConfirm("Topic"),
                isConfirmed -> {
                    if (isConfirmed)
                        presenter.onDeleteTopic(form.getEditedEntity());
                }));

        tableTools.addToolButtonRight(removeBtn);

        layout.add(tableTools.asWidget());

        table = new EndpointTable();
        endpointProvider = new ListDataProvider<>();
        endpointProvider.addDataDisplay(table);
        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);

        layout.add(table);
        layout.add(pager);

        pager.getElement().setAttribute("style", "margin-bottom:15px;");

        TextItem name = new TextItem("name", "Name");
        ListItem jndi = new JndiNamesItem("entries", "JNDI Names");

        form.setFields(name, jndi);

        Widget formToolsWidget = formTools.asWidget();
        formToolsWidget.getElement().setAttribute("style", "padding-top:15px;");

        form.bind(table);

        final FormHelpPanel helpPanel = new FormHelpPanel(() -> {
            ModelNode address = Baseadress.get();
            address.add("subsystem", "messaging-activemq");
            address.add("server", presenter.getCurrentServer());
            address.add("jms-topic", "*");
            return address;
        }, form);

        layout.add(formToolsWidget);
        layout.add(helpPanel.asWidget());
        layout.add(form.asWidget());
        return layout;
    }

    public void setTopics(List<ActivemqJMSEndpoint> topics) {
        endpointProvider.setList(topics);
        table.selectDefaultEntity();
    }

    public void setEnabled(boolean b) {
        form.setEnabled(b);
    }
}
