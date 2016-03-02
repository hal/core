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
package org.jboss.as.console.client.standalone.deploymentscanner;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.Property;

import java.util.List;
import java.util.Map;

import static org.jboss.as.console.client.standalone.deploymentscanner.DeploymentScannerPresenter.SCANNER_TEMPLATE;

/**
 * @author Harald Pehl
 */
public class DeploymentScannerView extends SuspendableViewImpl implements DeploymentScannerPresenter.MyView {

    private final SecurityFramework securityFramework;
    private final ResourceDescriptionRegistry resourceDescriptionRegistry;

    private DefaultCellTable<Property> table;
    private ListDataProvider<Property> dataProvider;
    private SingleSelectionModel<Property> selectionModel;

    private DeploymentScannerPresenter presenter;

    @Inject
    public DeploymentScannerView(final SecurityFramework securityFramework,
            final ResourceDescriptionRegistry resourceDescriptionRegistry) {
        this.securityFramework = securityFramework;
        this.resourceDescriptionRegistry = resourceDescriptionRegistry;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget createWidget() {

        // tools
        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(),
                event -> presenter.launchAddDialog()));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(),
                event -> {
                    String name = selectionModel.getSelectedObject().getName();
                    Feedback.confirm(Console.MESSAGES.deleteTitle("Deployment Scanner"),
                            Console.MESSAGES.deleteConfirm("Deployment Scanner '" + name + "'"),
                            isConfirmed -> {
                                if (isConfirmed) {
                                    presenter.remove(name);
                                }
                            });
                }));

        // table
        ProvidesKey<Property> providesKey = Property::getName;
        table = new DefaultCellTable<>(5, providesKey);
        dataProvider = new ListDataProvider<>(providesKey);
        selectionModel = new SingleSelectionModel<>(providesKey);
        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };
        table.addColumn(nameColumn, "Name");
        TextColumn<Property> pathColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getValue().get("path").asString();
            }
        };
        table.addColumn(pathColumn, "Path");
        TextColumn<Property> relativeToColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getValue().get("relative-to").asString();
            }
        };
        table.addColumn(relativeToColumn, "Relative to");
        dataProvider.addDataDisplay(table);
        table.setSelectionModel(selectionModel);

        // form
        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());
        ResourceDescription resourceDescription = resourceDescriptionRegistry.lookup(SCANNER_TEMPLATE);
        ModelNodeFormBuilder.FormAssets formAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext).build();
        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeSet) {
                presenter.save(selectionModel.getSelectedObject().getName(), formAssets.getForm().getChangedValues());
            }

            @Override
            public void onCancel(Object entity) {
                formAssets.getForm().cancel();
            }
        });
        VerticalPanel formPanel = new VerticalPanel();
        formPanel.setStyleName("fill-layout-width");
        formPanel.add(formAssets.getHelp().asWidget());
        formPanel.add(formAssets.getForm().asWidget());
        selectionModel.addSelectionChangeHandler(event -> {
            Property scanner = selectionModel.getSelectedObject();
            if (scanner != null) {
                formAssets.getForm().edit(scanner.getValue());
            } else {
                formAssets.getForm().clearValues();
            }
        });

        // layout
        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline("Deployment Scanners")
                .setDescription(SafeHtmlUtils
                        .fromString(Console.CONSTANTS.chooseDeploymentScanner()))
                .setMasterTools(tools)
                .setMaster(Console.MESSAGES.available("Deployment Scanners"), table)
                .addDetail(Console.CONSTANTS.common_label_attributes(), formPanel);
        return layoutBuilder.build();
    }

    @Override
    public void setPresenter(final DeploymentScannerPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void update(final List<Property> scanners) {
        dataProvider.setList(scanners);
        if (scanners.isEmpty()) {
            selectionModel.clear();
        } else {
            table.selectDefaultEntity();
            SelectionChangeEvent.fire(selectionModel); // updates ModelNodeForm's editedEntity with current value
        }
    }
}
