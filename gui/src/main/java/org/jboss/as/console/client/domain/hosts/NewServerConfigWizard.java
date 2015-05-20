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

package org.jboss.as.console.client.domain.hosts;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.runtime.DomainRuntimePresenter;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.v3.stores.domain.actions.FilterType;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.ComboBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;
import org.jboss.dmr.client.ModelNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Heiko Braun
 * @date 3/28/11
 */
public class NewServerConfigWizard {

    private DomainRuntimePresenter presenter;
    private ComboBoxItem groupItem;

    private ComboBoxItem hostItem;
    private ArrayList<String> groups;
    private List<ServerGroupRecord> serverGroups;

    public NewServerConfigWizard(final DomainRuntimePresenter presenter) {
        this.presenter = presenter;
    }

    public Widget asWidget() {

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");

        layout.add(new ContentHeaderLabel("Create new server"));

        final Form<Server> form = new Form<Server>(Server.class);
        form.setNumColumns(1);

        TextBoxItem nameItem = new TextBoxItem("name", Console.CONSTANTS.common_label_name())
        {

            @Override
            public void setFiltered(boolean filtered) {
                // prevent filtering in add dialog
            }

            @Override
            public boolean validate(String value) {
                boolean hasValue = super.validate(value);
                boolean hasWhitespace = value.contains(" ");
                return hasValue && !hasWhitespace;
            }

            @Override
            public String getErrMessage() {
                return Console.MESSAGES.common_validation_notEmptyNoSpace();
            }
        };

        CheckBoxItem startedItem = new CheckBoxItem("autoStart", "Auto Start?");

        // 'socket-binding-group' inherited from group
        // 'jvm' inherited from group

        NumberBoxItem portOffset = new NumberBoxItem("portOffset", "Port Offset");


        groupItem = new ComboBoxItem("group", "Server Group");
        groupItem.setDefaultToFirstOption(true);

        hostItem = new ComboBoxItem("hostName", "Host");
        hostItem.setDefaultToFirstOption(true);

        if(presenter.getFilter().equals(FilterType.GROUP)) {
            groupItem.setEnabled(false);

        }
        else
        {
            hostItem.setEnabled(false);

            /*int i=1;
            for (String host : hostNames) {
                if(presenter.getSelectedHost().equals(host))
                {
                    hostItem.selectItem(i);
                    break;
                }
                i++;
            }*/
        }

        form.setFields(nameItem, hostItem, groupItem, portOffset, startedItem);


        final FormHelpPanel helpPanel = new FormHelpPanel(
                new FormHelpPanel.AddressCallback() {
                    @Override
                    public ModelNode getAddress() {
                        ModelNode address = new ModelNode();
                        address.add("host", Console.MODULES.getHostStore().getSelectedHost());
                        address.add("server-config", "*");
                        return address;
                    }
                }, form
        );
        layout.add(helpPanel.asWidget());

        layout.add(form.asWidget());


        // ---

        ClickHandler saveHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final Server newServer = form.getUpdatedEntity();

                FormValidation validation = form.validate();
                if (validation.hasErrors())
                    return;

                // merge inherited values
                ServerGroupRecord selectedGroup =
                        getSelectedServerGroup(serverGroups, newServer.getGroup());

                newServer.setSocketBinding(selectedGroup.getSocketBinding());
                newServer.setJvm(null);//newServer.setJvm(selectedGroup.getJvm());
                newServer.setProperties(Collections.EMPTY_LIST);
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        presenter.onCreateServerConfig(newServer);
                    }
                });


            }
        };


        ClickHandler cancelHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.closeWindow();
            }
        };

        DialogueOptions options = new DialogueOptions(saveHandler, cancelHandler);

        return new WindowContentBuilder(layout, options).build();

    }

    public void updateGroups(final List<ServerGroupRecord> serverGroups) {
        this.serverGroups = serverGroups;
        List<String> groups = new ArrayList<String>(serverGroups.size());
        for(ServerGroupRecord rec : serverGroups)
            groups.add(rec.getName());
        groupItem.setValueMap(groups);

        int i=1;
        for (String group : groups) {
            if(group.equals(presenter.getSelectedGroup()))
            {
                groupItem.selectItem(i);
                break;
            }
            i++;
        }
    }

    public void updateHosts(final Set<String> hostNames) {
        hostItem.setValueMap(hostNames);

        int i=1;
        for (String host: hostNames) {
            if(presenter.getSelectedHost().equals(host))
            {
                hostItem.selectItem(i);
                break;
            }
            i++;
        }
    }

    private ServerGroupRecord getSelectedServerGroup(List<ServerGroupRecord> available, String selectedName)
    {
        ServerGroupRecord match = null;
        for(ServerGroupRecord rec : available)
        {
            if(rec.getName().equals(selectedName))
            {
                match = rec;
                break;
            }
        }

        return match;
    }
}
