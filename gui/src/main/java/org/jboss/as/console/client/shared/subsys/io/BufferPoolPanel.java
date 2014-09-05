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
package org.jboss.as.console.client.shared.subsys.io;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.dmr.ResourceDefinition;
import org.jboss.as.console.mbui.widgets.ModelDrivenWidget;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.Property;

import java.util.Map;

/**
 * @author Harald Pehl
 */
public class BufferPoolPanel extends ModelDrivenWidget {

    private final IOPresenter presenter;
    private final DefaultCellTable<Property> table;
    private final ListDataProvider<Property> dataProvider;
    private final SingleSelectionModel<Property> selectionModel;
    private final SecurityContext securityContext;

    @SuppressWarnings("unchecked")
    public BufferPoolPanel(IOPresenter presenter, SecurityFramework securityFramework) {
        super("{selected.profile}/subsystem=io/buffer-pool=*");

        this.presenter = presenter;
        this.table = new DefaultCellTable<>(5);
        this.dataProvider = new ListDataProvider<Property>();
        this.selectionModel = new SingleSelectionModel<Property>();
        this.securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());

        dataProvider.addDataDisplay(table);
        table.setSelectionModel(selectionModel);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget buildWidget(ResourceAddress address, ResourceDefinition definition) {

        // tool strip
        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.launchAddWorkerDialog();
            }
        }));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Feedback.confirm(Console.MESSAGES.deleteTitle("Buffer Pool"),
                        Console.MESSAGES.deleteConfirm("Buffer Pool '" + selectionModel.getSelectedObject().getName() + "'"),
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    presenter.removeWorker(selectionModel.getSelectedObject().getName());
                                }
                            }
                        });
            }
        }));

        // master (table)
        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };
        table.addColumn(nameColumn, "Name");

        // details (form)
        final ModelNodeFormBuilder.FormAssets formAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .setSecurityContext(securityContext).build();
        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeSet) {
                presenter.saveWorker(selectionModel.getSelectedObject().getName(), formAssets.getForm().getChangedValues());
            }

            @Override
            public void onCancel(Object entity) {
                formAssets.getForm().cancel();
            }
        });
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                Property worker = selectionModel.getSelectedObject();
                if (worker != null) {
                    formAssets.getForm().edit(worker.getValue());
                } else {
                    formAssets.getForm().clearValues();
                }
            }
        });

        // putting everything together
        VerticalPanel formPanel = new VerticalPanel();
        formPanel.setStyleName("fill-layout-width");
        formPanel.add(formAssets.getHelp().asWidget());
        formPanel.add(formAssets.getForm().asWidget());

        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline("Buffer Pools")
                .setDescription("Please chose a buffer pool from below for specific settings.")
                .setMasterTools(tools)
                .setMaster(Console.MESSAGES.available("Buffer Pool"), table)
                .addDetail("Attributes", formPanel);
        return layoutBuilder.build();
    }
}
