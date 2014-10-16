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
package org.jboss.as.console.client.shared.subsys.batch.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.subsys.batch.BatchPresenter;
import org.jboss.as.console.client.shared.subsys.batch.store.BatchStore;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.Property;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.List;
import java.util.Map;

/**
 * @author Harald Pehl
 */
class ThreadFactoriesPanel extends Composite {

    private final ProvidesKey<Property> providesKey;
    private final DefaultCellTable<Property> table;
    private final ListDataProvider<Property> dataProvider;
    private final SingleSelectionModel<Property> selectionModel;

    @SuppressWarnings("unchecked")
    ThreadFactoriesPanel(final StatementContext statementContext, final SecurityContext securityContext,
                         final BatchPresenter presenter) {

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.launchAddThreadFactory();
            }
        }));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final String name = selectionModel.getSelectedObject().getName();
                Feedback.confirm(Console.MESSAGES.deleteTitle("Thread Factory"),
                        Console.MESSAGES.deleteConfirm("Thread Factory '" + name + "'"),
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    presenter.removeThreadFactory(name);
                                }
                            }
                        });
            }
        }));

        providesKey = new ProvidesKey<Property>() {
            @Override
            public Object getKey(Property item) {
                return item.getName();
            }
        };
        table = new DefaultCellTable<>(5, providesKey);
        dataProvider = new ListDataProvider<Property>(providesKey);
        dataProvider.addDataDisplay(table);
        selectionModel = new SingleSelectionModel<Property>(providesKey);
        table.setSelectionModel(selectionModel);
        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };
        table.addColumn(nameColumn, "Name");

        final BatchResourceForm details = new BatchResourceForm(BatchStore.THREAD_FACTORIES_ADDRESS, statementContext, securityContext) {
            @Override
            void onSave(Map<String, Object> changedValues) {
                presenter.modifyThreadFactory(selectionModel.getSelectedObject().getName(), changedValues);
            }
        };
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if (details.getForm() != null) {
                    Property worker = selectionModel.getSelectedObject();
                    if (worker != null) {
                        details.getForm().edit(worker.getValue());
                    } else {
                        details.getForm().clearValues();
                    }
                }
            }
        });

        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline("Thread Factories")
                .setDescription(SafeHtmlUtils.fromString("Please chose a thread factory from below for specific settings."))
                .setMasterTools(tools)
                .setMaster(Console.MESSAGES.available("Thread Factory"), table)
                .addDetail("Attributes", details);
        initWidget(layoutBuilder.build());
    }

    void select(String key) {
        for (Property property : dataProvider.getList()) {
            if (property.getName().equals(key)) {
                selectionModel.setSelected(property, true);
                break;
            }
        }
    }

    void update(final List<Property> models) {
        dataProvider.setList(models);
        if (models.isEmpty()) {
            selectionModel.clear();
        } else {
            table.selectDefaultEntity();
        }
    }
}
