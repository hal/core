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

package org.jboss.as.console.client.domain.hosts.general;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.DisposableViewImpl;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.jvm.JvmEditor;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 5/18/11
 */
public class HostJVMView extends DisposableViewImpl implements HostJVMPresenter.MyView {

    private HostJVMPresenter presenter;
    private JvmEditor jvmEditor;
    private DefaultCellTable<Property> table;
    private ListDataProvider<Property> dataProvider;
    private SingleSelectionModel<Property> selectionModel;

    private ResourceDescriptionRegistry resourceDescriptionRegistry;
    private SecurityFramework securityFramework;

    @Inject
    public HostJVMView(final ResourceDescriptionRegistry resourceDescriptionRegistry,
                       final SecurityFramework securityFramework) {
        this.resourceDescriptionRegistry = resourceDescriptionRegistry;
        this.securityFramework = securityFramework;
    }

    @Override
    public Widget createWidget() {

        ProvidesKey<Property> providesKey = Property::getName;
        selectionModel = new SingleSelectionModel<>(providesKey);
        selectionModel.addSelectionChangeHandler(event -> updateDetail(selectionModel.getSelectedObject()));
        table = new DefaultCellTable<>(8, providesKey);
        table.setSelectionModel(selectionModel);
        dataProvider = new ListDataProvider<>(providesKey);
        dataProvider.addDataDisplay(table);

        ToolStrip toolStrip = new ToolStrip();

        ToolButton addBtn= new ToolButton(Console.CONSTANTS.common_label_add(),
                event -> presenter.launchNewJVMDialogue());
        addBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_add_hostJVMView());
        toolStrip.addToolButtonRight(addBtn);

        ToolButton removeBtn = new ToolButton(Console.CONSTANTS.common_label_delete(), event -> {

            final Property entity = selectionModel.getSelectedObject();

            Feedback.confirm(
                    Console.MESSAGES.deleteTitle("JVM Configuration"),
                    Console.MESSAGES.deleteConfirm("JVM Configuration '" + entity.getName() + "'"),
                    isConfirmed -> {
                        if (isConfirmed)
                            presenter.onDeleteJvm("", entity.getName());
                    });

        });
        removeBtn.ensureDebugId(Console.DEBUG_CONSTANTS.debug_label_add_hostJVMView());
        toolStrip.addToolButtonRight(removeBtn);

        // ---

        TextColumn<Property> nameCol = new TextColumn<Property>() {
            @Override
            public String getValue(Property object) {
                return object.getName();
            }
        };


        table.addColumn(nameCol, "Name");
        //table.addColumn(debugCol, "IsDebugEnabled?");

        ResourceDescription resourceDescription = resourceDescriptionRegistry.lookup(HostJVMPresenter.ROOT_ADDRESS_TEMPLATE);
        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());

        jvmEditor = new JvmEditor(presenter, resourceDescription, securityContext, HostJVMPresenter.ROOT_ADDRESS);
        jvmEditor.setEnableClearButton(false);

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setTitle("JVM Configurations")
                .setPlain(true)
                .setDescription(SafeHtmlUtils.fromString(Console.CONSTANTS.hosts_jvm_desc()))
                .setHeadline(Console.CONSTANTS.hosts_jvm_title())
                .setMaster(Console.MESSAGES.available("JVM Configurations"), table)
                .setMasterTools(toolStrip)
                .setDetail(Console.CONSTANTS.common_label_selection(), jvmEditor.asWidget());

        return layout.build();
    }

    @Override
    public void setPresenter(HostJVMPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void updateModel(List<Property> jvms) {
        dataProvider.setList(jvms);
        table.selectDefaultEntity();
        SelectionChangeEvent.fire(selectionModel);
    }

    private void updateDetail(Property jvm) {
        jvmEditor.setSelectedRecord("", jvm);
    }
}
