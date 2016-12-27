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
package org.jboss.as.console.client.shared.subsys.undertow;

import java.util.List;
import java.util.Map;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelDescriptionConstants;
import org.jboss.dmr.client.Property;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 * @since 04/06/2016
 */
public class FilterEditor {

    private FilterPresenter presenter;
    private final boolean showDeprecated;
    private DefaultCellTable table;
    private ListDataProvider<Property> dataProvider;
    private final SingleSelectionModel<Property> selectionModel;
    private SecurityContext securityContext;
    private ResourceDescription definition;
    private final AddressTemplate addressTemplate;
    private final String title;
    private ModelNodeFormBuilder.FormAssets formAssets;

    public FilterEditor(FilterPresenter presenter, AddressTemplate addressTemplate, String title, boolean showDeprecated) {
        this.presenter = presenter;
        this.showDeprecated = showDeprecated;
        this.table = new DefaultCellTable(5);
        this.dataProvider = new ListDataProvider<>();
        this.dataProvider.addDataDisplay(table);
        ProvidesKey<Property> providesKey = Property::getName;
        this.selectionModel = new SingleSelectionModel<>(providesKey);
        this.table.setSelectionModel(new SingleSelectionModel<Property>());
        securityContext = presenter.getSecurityFramework().getSecurityContext(presenter.getProxy().getNameToken());
        definition = presenter.getDescriptionRegistry().lookup(addressTemplate);
        this.addressTemplate = addressTemplate;
        this.title = title;
    }

    public Widget asWidget() {

        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };

        table.addColumn(nameColumn, "Name");

        formAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(definition)
                .setSecurityContext(securityContext)
                .includeDeprecated(showDeprecated)
                .build();


        VerticalPanel formPanel =  null;
        boolean hasAttributes = definition.hasAttributes();
        if (hasAttributes) {
            // edit attributes callback
            formAssets.getForm().setToolsCallback(new FormCallback() {
                @Override
                public void onSave(Map changeset) {
                    presenter.onSaveFilter(addressTemplate, getCurrentSelection().getName(), changeset);
                }

                @Override
                public void onCancel(Object entity) {
                    formAssets.getForm().cancel();
                }
            });

            formPanel = new VerticalPanel();
            formPanel.setStyleName("fill-layout-width");
            formPanel.add(formAssets.getHelp().asWidget());
            formPanel.add(formAssets.getForm().asWidget());

            selectionModel.addSelectionChangeHandler(event -> {
                Property selectedItem = selectionModel.getSelectedObject();
                if (selectedItem != null) {
                    formAssets.getForm().edit(selectedItem.getValue());
                } else {
                    formAssets.getForm().clearValues();
                }
            });
        }

        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline(title)
                .setDescription(SafeHtmlUtils.fromString(definition.get(ModelDescriptionConstants.DESCRIPTION).asString()))
                .setMasterTools(tableToolsButtons())
                .setMaster(Console.MESSAGES.available(title), table);


        if (hasAttributes)
            layoutBuilder.addDetail(Console.CONSTANTS.common_label_attributes(), formPanel);

        table.setSelectionModel(selectionModel);
        return layoutBuilder.build();
    }

    private ToolStrip tableToolsButtons() {
        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), event -> {
            presenter.onLaunchAddResourceDialog(addressTemplate, title);
        }));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(), event -> Feedback.confirm(Console.MESSAGES.deleteTitle("Container"),
            Console.MESSAGES.deleteConfirm(title + " '" + getCurrentSelection().getName() + "'"),
            isConfirmed -> {
                if (isConfirmed) {
                    presenter.onRemoveResource(addressTemplate, getCurrentSelection().getName());
                }
            })));
        return tools;
    }

    private Property getCurrentSelection() {
        Property selection = ((SingleSelectionModel<Property>) table.getSelectionModel()).getSelectedObject();
        return selection;
    }

    public void updateValuesFromModel(List<Property> filters) {
        dataProvider.setList(filters);
        table.selectDefaultEntity();
        if (filters.isEmpty()) {
            selectionModel.clear();
            formAssets.getForm().clearValues();
        }
        SelectionChangeEvent.fire(selectionModel);

    }
}
