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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.runtime.DomainRuntimePresenter;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.forms.ComboBoxItem;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;
import org.jboss.dmr.client.ModelNode;

import java.util.Set;

/**
 * @author Heiko Braun
 * @date 3/28/11
 */
public class CopyServerWizard {

    private DomainRuntimePresenter presenter;
    private ComboBoxItem hostItem;
    private TextBoxItem nameItem;
    private Server origServer;

    public CopyServerWizard(final DomainRuntimePresenter presenter)
    {
        this.presenter = presenter;
    }

    public Widget asWidget() {

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");


        layout.add(new ContentDescription(Console.CONSTANTS.copyServerDescription()));

        final Form<Server> form = new Form<Server>(Server.class);
        form.setNumColumns(1);

        nameItem = new TextBoxItem("name", Console.CONSTANTS.common_label_name())
        {
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

            @Override
            public void setFiltered(boolean filtered) {
                // prevent filtering in add dialog
            }
        };



        NumberBoxItem portOffset = new NumberBoxItem("portOffset", Console.CONSTANTS.common_label_portOffset());

        // host names

        hostItem = new ComboBoxItem("hostName", Console.CONSTANTS.common_label_host());
/*
        hostItem.setValueMap(hostNames);
        hostItem.selectItem(preselection+1);
*/

        form.setFields(nameItem, portOffset, hostItem);

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
                if (!validation.hasErrors())
                {
                    presenter.onSaveCopy(hostItem.getValue(), origServer, newServer);
                }
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

    public void setCurrentServerSelection(Server server) {
        this.origServer = server;
        nameItem.setValue(server.getName()+"_copy");
    }

    public void setHosts(Set<String> hosts, String selectedHost) {

        int preselection = 0;
        int index = 1;
        for(String item : hosts)
        {

            if(item.equals(selectedHost))
                preselection = index;
            index++;

        }

        hostItem.setValueMap(hosts);
        hostItem.selectItem(preselection);
    }
}
