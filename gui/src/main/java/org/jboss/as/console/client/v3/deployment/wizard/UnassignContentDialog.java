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
package org.jboss.as.console.client.v3.deployment.wizard;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.DefaultSelectionEventManager.SelectAction;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.deployment.Content;
import org.jboss.as.console.client.v3.deployment.DomainDeploymentFinder;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.TrappedFocusPanel;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import java.util.List;
import java.util.Set;

import static com.google.gwt.dom.client.Style.Unit.PX;

/**
 * @author Harald Pehl
 */
public class UnassignContentDialog implements IsWidget {

    private final DomainDeploymentFinder presenter;
    private Content content;

    private ListDataProvider<String> dataProvider;
    private MultiSelectionModel<String> selectionModel;
    private DefaultWindow window;
    private HTML errorMessages;
    private Label intro;
    private DefaultCellTable<String> table;

    public UnassignContentDialog(final DomainDeploymentFinder presenter) {
        this.presenter = presenter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        VerticalPanel root = new VerticalPanel();
        root.setStyleName("window-content");

        errorMessages = new HTML();
        errorMessages.setVisible(false);
        errorMessages.setStyleName("error-panel");
        intro = new Label();
        intro.getElement().getStyle().setMarginBottom(10, PX);

        root.add(new HTML("<h3>Unassign Content</h3>"));
        root.add(errorMessages);
        root.add(intro);

        // provider, selection model
        ProvidesKey<String> keyProvider = item -> item;
        dataProvider = new ListDataProvider<>(keyProvider);
        selectionModel = new MultiSelectionModel<>(keyProvider);

        // table
        table = new DefaultCellTable<>(5, keyProvider);
        table.setSelectionModel(selectionModel, DefaultSelectionEventManager.createCustomManager(
                new DefaultSelectionEventManager.CheckboxEventTranslator<String>() {
                    @Override
                    public SelectAction translateSelectionEvent(CellPreviewEvent<String> event) {
                        SelectAction action = super.translateSelectionEvent(event);
                        if (action.equals(SelectAction.IGNORE)) {
                            String serverGroup = event.getValue();
                            boolean selected = selectionModel.isSelected(serverGroup);
                            return selected ? SelectAction.DESELECT : SelectAction.SELECT;
                        }
                        return action;
                    }
                }));
        dataProvider.addDataDisplay(table);
        root.add(table);

        // columns
        Column<String, Boolean> checkColumn = new Column<String, Boolean>(new CheckboxCell(true, false)) {
            @Override
            public Boolean getValue(String serverGroup) {
                return selectionModel.isSelected(serverGroup);
            }
        };
        TextColumn<String> nameColumn = new TextColumn<String>() {
            @Override
            public String getValue(String serverGroup) {
                return serverGroup;
            }
        };
        table.addColumn(checkColumn, SafeHtmlUtils.fromSafeConstant("<br/>"));
        table.setColumnWidth(checkColumn, 40, PX);
        table.addColumn(nameColumn, "Server Group");

        // pager
        DefaultPager pager = new DefaultPager();
        pager.setWidth("auto");
        pager.setDisplay(table);
        root.add(pager);

        return new TrappedFocusPanel(new WindowContentBuilder(root,
                new DialogueOptions(Console.CONSTANTS.common_label_assign(),
                        event -> {
                            Set<String> selectedSet = selectionModel.getSelectedSet();
                            if (selectedSet.isEmpty()) {
                                errorMessages.setText(
                                        Console.CONSTANTS.pleaseSelectServerGroup());
                                errorMessages.setVisible(true);
                            } else {
                                close();
                                presenter.unassignContent(content, selectedSet);
                            }
                        },
                        Console.CONSTANTS.common_label_cancel(), event -> close()))
                .build());
    }

    public void open(final Content content, final List<String> serverGroups) {
        this.content = content;

        if (window == null) {
            window = new DefaultWindow("Unassign Content");
            window.setWidth(520);
            window.setHeight(400);
            window.trapWidget(asWidget());
            window.setGlassEnabled(true);
        }
        errorMessages.setText("");
        errorMessages.setVisible(false);
        intro.setText(Console.MESSAGES.unassignContent(content.getName()));
        dataProvider.setList(serverGroups);
        selectionModel.clear();
        table.selectDefaultEntity();
        window.center();
    }

    public void close() {
        if (window != null) {
            window.hide();
        }
    }
}
