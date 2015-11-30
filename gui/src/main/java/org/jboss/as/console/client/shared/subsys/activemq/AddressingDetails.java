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

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.help.FormHelpPanel;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqAddressingPattern;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;

import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 * @date 5/10/11
 */
public class AddressingDetails {

    private MsgDestinationsPresenter presenter;
    private Form<ActivemqAddressingPattern> form;

    private DefaultCellTable<ActivemqAddressingPattern> addrTable;
    private ListDataProvider<ActivemqAddressingPattern> addrProvider;
    private ContentHeaderLabel serverName;

    public AddressingDetails(MsgDestinationsPresenter presenter) {
        this.presenter = presenter;
    }

    @SuppressWarnings("unchecked")
    Widget asWidget() {
        addrTable = new DefaultCellTable<>(8, ActivemqAddressingPattern::getPattern);
        addrProvider = new ListDataProvider<>();
        addrProvider.addDataDisplay(addrTable);

        Column<ActivemqAddressingPattern, String> patternColumn = new Column<ActivemqAddressingPattern, String>(new TextCell()) {
            @Override
            public String getValue(ActivemqAddressingPattern object) {
                return object.getPattern();
            }
        };

        addrTable.addColumn(patternColumn, "Pattern");

        form = new Form<>(ActivemqAddressingPattern.class,
                "{selected.profile}/subsystem=messaging-activemq/server=*/address-setting=*");
        form.setNumColumns(2);
        form.bind(addrTable);

        TextBoxItem dlQ = new TextBoxItem("deadLetterQueue", "Dead Letter Address");
        TextBoxItem expQ = new TextBoxItem("expiryQueue", "Expiry Address");
        NumberBoxItem redelivery = new NumberBoxItem("redeliveryDelay", "Redelivery Delay");
        NumberBoxItem maxDelivery = new NumberBoxItem("maxDelivery", "Max Delivery Attempts", -1, Integer.MAX_VALUE);

        form.setFields(dlQ, expQ, redelivery, maxDelivery);

        FormHelpPanel helpPanel = new FormHelpPanel(() -> {
            ModelNode address = Baseadress.get();
            address.add("subsystem", "messaging-activemq");
            address.add("server", "*");
            address.add("address-setting", "*");
            return address;
        }, form);

        FormToolStrip<ActivemqAddressingPattern> formTools = new FormToolStrip<>(
                form,
                new FormToolStrip.FormCallback<ActivemqAddressingPattern>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                        presenter.onSaveAddressDetails(form.getEditedEntity(), changeset);
                    }

                    @Override
                    public void onDelete(ActivemqAddressingPattern entity) {}
                }
        );
        ToolStrip tableTools = new ToolStrip();
        ToolButton addBtn = new ToolButton(Console.CONSTANTS.common_label_add(),
                event -> presenter.launchNewAddrDialogue());
        addBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_add_addressingDetails());
        tableTools.addToolButtonRight(addBtn);

        ToolButton removeBtn = new ToolButton(Console.CONSTANTS.common_label_delete(), event -> Feedback.confirm(
                Console.MESSAGES.deleteTitle("Addressing Config"),
                Console.MESSAGES.deleteConfirm("Addressing Config"),
                isConfirmed -> {
                    if (isConfirmed) { presenter.onDeleteAddressDetails(form.getEditedEntity()); }
                }));

        tableTools.addToolButtonRight(removeBtn);

        VerticalPanel formPanel = new VerticalPanel();
        formPanel.addStyleName("fill-layout-width");

        formPanel.add(helpPanel.asWidget());
        formPanel.add(formTools.asWidget());
        formPanel.add(form.asWidget());

        serverName = new ContentHeaderLabel();

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadlineWidget(serverName)
                .setDescription(
                        Console.CONSTANTS.addressingDescription())
                .setMaster(Console.MESSAGES.available("Address Settings"), addrTable)
                .setMasterTools(tableTools.asWidget())
                .setDetail(Console.CONSTANTS.common_label_details(), formPanel);

        return layout.build();

    }

    public void setAddressingConfig(List<ActivemqAddressingPattern> addrPatterns) {
        addrProvider.setList(addrPatterns);
        addrTable.selectDefaultEntity();
        form.setEnabled(false);
        serverName.setText("Address Settings: Provider " + presenter.getCurrentServer());
    }
}
