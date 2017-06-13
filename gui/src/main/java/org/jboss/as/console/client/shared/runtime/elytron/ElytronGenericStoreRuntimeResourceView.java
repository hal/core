/*
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.console.client.shared.runtime.elytron;

import java.util.List;

import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import static org.jboss.dmr.client.ModelDescriptionConstants.DESCRIPTION;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class ElytronGenericStoreRuntimeResourceView {

    protected ResourceDescription resourceDescription;
    protected String title;
    protected AddressTemplate addressTemplate;
    protected final SecurityContext securityContext;
    private ElytronRuntimePresenter presenter;

    private DefaultCellTable<Property> table;
    private ListDataProvider<Property> dataProvider;
    private SingleSelectionModel<Property> selectionModel;
    private GenericAliasEditor aliasEditor;

    public ElytronGenericStoreRuntimeResourceView(final ResourceDescription resourceDescription,
            final SecurityContext securityContext, String title, AddressTemplate addressTemplate) {

        this.securityContext = securityContext;
        this.resourceDescription = resourceDescription;
        this.title = title;
        this.addressTemplate = addressTemplate;

        aliasEditor = new GenericAliasEditor(resourceDescription, securityContext);
    }

    public Widget asWidget() {

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_refresh(),
                event -> presenter.loadCredentialReferences()));

        ProvidesKey<Property> providesKey = Property::getName;
        table = new DefaultCellTable<>(10, providesKey);
        dataProvider = new ListDataProvider<>(providesKey);
        dataProvider.addDataDisplay(table);
        selectionModel = new SingleSelectionModel<>(providesKey);
        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };
        TextColumn<Property> stateColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getValue().get("state").asString();
            }
        };
        table.addColumn(nameColumn, "Name");
        table.addColumn(stateColumn, "State");
        stateColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        table.setColumnWidth(stateColumn, 20, Style.Unit.PCT);

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);

        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline(title)
                .setDescription(SafeHtmlUtils.fromString(resourceDescription.get(DESCRIPTION).asString()))
                .setMasterTools(tools)
                .setMaster(Console.MESSAGES.available(title), table)
                .addDetail("Aliases", aliasEditor.asWidget());

        selectionModel.addSelectionChangeHandler(event -> {
            Property selectedProperty = selectionModel.getSelectedObject();
            if (selectedProperty != null) {
                aliasEditor.setCredentialReferenceName(selectedProperty.getName());
                presenter.loadAliases(ElytronRuntimePresenter.CREDENTIAL_STORE_TEMPLATE, selectedProperty.getName());
            } else {
                aliasEditor.setCredentialReferenceName(null);
                aliasEditor.clearValues();
            }
        });
        table.setSelectionModel(selectionModel);

        return layoutBuilder.build();
    }

    public void update(final List<Property> models) {
        dataProvider.setList(models);
        table.selectDefaultEntity();
        if (models.isEmpty()) {
            selectionModel.clear();
        }
    }

    public void setPresenter(final ElytronRuntimePresenter presenter) {
        this.presenter = presenter;
        aliasEditor.setPresenter(presenter);
    }

    public void updateCredentialReferenceAliases(final List<ModelNode> models) {
        aliasEditor.update(models);
    }
}
