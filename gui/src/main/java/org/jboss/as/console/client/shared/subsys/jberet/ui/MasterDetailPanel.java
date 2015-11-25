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
package org.jboss.as.console.client.shared.subsys.jberet.ui;

import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.mbui.widgets.ComplexAttributeForm;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.dmr.client.ModelDescriptionConstants.NAME;

/**
 * @author Harald Pehl
 */
public abstract class MasterDetailPanel implements IsWidget {

    private final String title;
    private final Dispatcher circuit;
    private final ResourceDescription resourceDescription;
    private final SecurityContext securityContext;
    private final String[] excludes;

    private DefaultCellTable<Property> table;
    private ListDataProvider<Property> dataProvider;
    private SingleSelectionModel<Property> selectionModel;
    private ModelNodeFormBuilder.FormAssets formAssets;
    private ModelNodeFormBuilder.FormAssets timeFormAssets;

    public MasterDetailPanel(final String title, final Dispatcher circuit,
                             final ResourceDescription resourceDescription, final SecurityContext securityContext,
                             final String... excludes) {
        this.title = title;
        this.circuit = circuit;
        this.resourceDescription = resourceDescription;
        this.securityContext = securityContext;
        this.excludes = excludes;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_add(), event -> onAdd()));
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.common_label_delete(),
                event -> {
                    if (selectionModel.getSelectedObject() != null) {
                        String name = selectionModel.getSelectedObject().getName();
                        Feedback.confirm(Console.MESSAGES.deleteTitle(title),
                                Console.MESSAGES.deleteConfirm(title + " '" + name + "'"),
                                isConfirmed -> {
                                    if (isConfirmed) {
                                        dispatchRemove(circuit, name);
                                    }
                                });
                    }
                }));

        ProvidesKey<Property> providesKey = Property::getName;
        table = new DefaultCellTable<>(5, providesKey);
        dataProvider = new ListDataProvider<>(providesKey);
        dataProvider.addDataDisplay(table);
        selectionModel = new SingleSelectionModel<>(providesKey);
        table.setSelectionModel(selectionModel);
        TextColumn<Property> nameColumn = new TextColumn<Property>() {
            @Override
            public String getValue(Property node) {
                return node.getName();
            }
        };
        table.addColumn(nameColumn, "Name");

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);

        VerticalPanel tableAndPager = new VerticalPanel();
        tableAndPager.setStyleName("fill-layout-width");
        tableAndPager.add(table);
        tableAndPager.add(pager);

        if (hasAttributes()) {
            ModelNodeFormBuilder formBuilder = new ModelNodeFormBuilder()
                    .setResourceDescription(resourceDescription)
                    .setSecurityContext(securityContext)
                    .setConfigOnly();
            if (excludes != null && excludes.length != 0) {
                formBuilder.exclude(excludes);
            }
            formAssets = formBuilder.build();

            formAssets.getForm().setToolsCallback(new FormCallback() {
                @Override
                @SuppressWarnings("unchecked")
                public void onSave(final Map changeset) {
                    dispatchModify(circuit, selectionModel.getSelectedObject().getName(), changeset);
                }

                @Override
                public void onCancel(final Object entity) {
                    formAssets.getForm().cancel();
                }
            });

            selectionModel.addSelectionChangeHandler(event -> {
                Property property = selectionModel.getSelectedObject();
                if (property != null) {
                    formAssets.getForm().edit(property.getValue());
                    if(timeFormAssets!=null)
                        timeFormAssets.getForm().edit(property.getValue().get("keepalive-time"));
                } else {
                    formAssets.getForm().clearValues();
                    if(timeFormAssets!=null)
                        timeFormAssets.getForm().clearValues();
                }
            });
        }

        OneToOneLayout layoutBuilder = new OneToOneLayout()
                .setPlain(true)
                .setHeadline(title)
                .setDescription(resourceDescription.get("description").asString())
                .setMasterTools(tools)
                .setMaster(null, tableAndPager);


        if (hasAttributes()) {
            layoutBuilder.addDetail(Console.CONSTANTS.common_label_attributes(), formAssets.asWidget());
        }

        if(resourceDescription.get("attributes").hasDefined("keepalive-time"))
        {
            // complex attribute 'file'
            ComplexAttributeForm timeForm = new ComplexAttributeForm("keepalive-time", securityContext, resourceDescription);
            timeFormAssets = timeForm.build();

            // order matters
            timeFormAssets.getForm().setToolsCallback(new FormCallback() {
                @Override
                public void onSave(Map changeset) {

                    // ignore the changeset: complex attributes are written atomically, including all attributes

                    dispatchWriteAttribute(
                            circuit,
                            selectionModel.getSelectedObject().getName(),
                            "keepalive-time",
                            timeFormAssets.getForm().getUpdatedEntity()
                    );

                }

                @Override
                public void onCancel(Object o) {
                    timeFormAssets.getForm().cancel();
                }
            });

            layoutBuilder.addDetail("Keepalive", timeFormAssets.asWidget());
        }


        return layoutBuilder.build();
    }

    private void onAdd() {
        DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle("In Memory Job Repository"));
        AddResourceDialog addDialog = new AddResourceDialog(securityContext, resourceDescription,
                new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        // The instance name must be part of the model node!
                        String name = payload.get(NAME).asString();
                        dispatchAdd(circuit, new Property(name, payload));
                        dialog.hide();
                    }

                    @Override
                    public void onCancel() {
                        dialog.hide();
                    }
                });
        dialog.setWidth(480);
        dialog.setHeight(360);
        dialog.setWidget(addDialog);
        dialog.setGlassEnabled(true);
        dialog.center();
    }

    private boolean hasAttributes() {
        return resourceDescription.hasDefined(ATTRIBUTES) && !resourceDescription.get(ATTRIBUTES).asList().isEmpty();
    }

    protected void update(final List<Property> models) {
        dataProvider.setList(models);
        table.selectDefaultEntity();
        SelectionChangeEvent.fire(selectionModel); // updates ModelNodeForm's editedEntity with current value
    }

    protected abstract void dispatchAdd(final Dispatcher circuit, final Property property);

    protected abstract void dispatchModify(final Dispatcher circuit, final String name, final Map<String, Object> changedValues);

    protected abstract void dispatchRemove(final Dispatcher circuit, final String name);

    protected void dispatchWriteAttribute(final Dispatcher circuit, String parentName, final String attributeName, ModelNode payload) {};
}
