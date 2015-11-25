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
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqSecurityPattern;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.DisclosureGroupRenderer;
import org.jboss.ballroom.client.widgets.forms.Form;
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
public class SecurityDetails {

    private MsgDestinationsPresenter presenter;
    private Form<ActivemqSecurityPattern> form;

    private DefaultCellTable<ActivemqSecurityPattern> secTable;
    private ListDataProvider<ActivemqSecurityPattern> secProvider;
    private ContentHeaderLabel serverName;

    public SecurityDetails(MsgDestinationsPresenter presenter) {
        this.presenter = presenter;
    }

    @SuppressWarnings("unchecked")
    Widget asWidget() {
        secTable = new DefaultCellTable<>(8, item -> item.getPattern() + "_" + item.getRole());
        secProvider = new ListDataProvider<>();
        secProvider.addDataDisplay(secTable);

        Column<ActivemqSecurityPattern, String> roleColumn = new Column<ActivemqSecurityPattern, String>(new TextCell()) {
            @Override
            public String getValue(ActivemqSecurityPattern object) {
                return object.getRole();
            }
        };

        Column<ActivemqSecurityPattern, String> patternColumn = new Column<ActivemqSecurityPattern, String>(new TextCell()) {
            @Override
            public String getValue(ActivemqSecurityPattern object) {
                return object.getPattern();
            }
        };

        secTable.addColumn(patternColumn, "Pattern");
        secTable.addColumn(roleColumn, "Role");

        form = new Form<>(ActivemqSecurityPattern.class);
        form.setNumColumns(2);
        form.bind(secTable);

        CheckBoxItem send = new CheckBoxItem("send", "Send?");
        CheckBoxItem consume = new CheckBoxItem("consume", "Consume?");
        CheckBoxItem manage = new CheckBoxItem("manage", "Manage?");

        CheckBoxItem createDQ = new CheckBoxItem("createDurableQueue", "CreateDurable?");
        CheckBoxItem deleteDQ = new CheckBoxItem("deleteDurableQueue", "DeleteDurable?");

        CheckBoxItem createNDQ = new CheckBoxItem("createNonDurableQueue", "CreateNonDurable?");
        CheckBoxItem deleteNDQ = new CheckBoxItem("deleteNonDurableQueue", "DeleteNonDurable?");

        form.setFields(send, consume, manage);
        form.setFieldsInGroup(Console.CONSTANTS.common_label_advanced(), new DisclosureGroupRenderer(), createDQ,
                deleteDQ, createNDQ, deleteNDQ);

        FormHelpPanel helpPanel = new FormHelpPanel(() -> {
            ModelNode address = Baseadress.get();
            address.add("subsystem", "messaging-activemq");
            address.add("server", "*");
            address.add("security-setting", "*");
            address.add("role", "*");
            return address;
        }, form);

        FormToolStrip<ActivemqSecurityPattern> formTools = new FormToolStrip<>(
                form,
                new FormToolStrip.FormCallback<ActivemqSecurityPattern>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                        presenter.onSaveSecDetails(form.getEditedEntity(), changeset);
                    }

                    @Override
                    public void onDelete(ActivemqSecurityPattern entity) {}
                }
        );

        ToolStrip tableTools = new ToolStrip();

        ToolButton addBtn = new ToolButton(Console.CONSTANTS.common_label_add(),
                event -> presenter.launchNewSecDialogue());
        addBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_add_securityDetails());
        tableTools.addToolButtonRight(addBtn);

        ToolButton removeBtn = new ToolButton(Console.CONSTANTS.common_label_delete(), event -> Feedback.confirm(
                Console.MESSAGES.deleteTitle("Security Config"),
                Console.MESSAGES.deleteConfirm("Security Config"),
                isConfirmed -> {
                    if (isConfirmed) { presenter.onDeleteSecDetails(form.getEditedEntity()); }
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
                        ((UIConstants) GWT.create(UIConstants.class)).securitySettingsDescription())
                .setMaster(Console.MESSAGES.available("security settings"), secTable)
                .setMasterTools(tableTools.asWidget())
                .setDetail(Console.CONSTANTS.common_label_details(), formPanel);

        return layout.build();
    }

    public void setSecurityConfig(List<ActivemqSecurityPattern> patterns) {
        secProvider.setList(patterns);
        secTable.selectDefaultEntity();
        form.setEnabled(false);
        serverName.setText("Security Settings: Provider " + presenter.getCurrentServer());
    }
}
