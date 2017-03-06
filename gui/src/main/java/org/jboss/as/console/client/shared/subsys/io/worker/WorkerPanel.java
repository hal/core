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
package org.jboss.as.console.client.shared.subsys.io.worker;

import java.util.Map;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.MultipleToOneLayout;
import org.jboss.as.console.client.shared.subsys.io.IOPanel;
import org.jboss.as.console.client.shared.subsys.io.IOPresenter;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.Property;

/**
 * @author Harald Pehl
 */
public class WorkerPanel extends IOPanel {

    public WorkerPanel(IOPresenter presenter) {
        super(AddressTemplate.of("{selected.profile}/subsystem=io/worker=*"), presenter);
    }

    @Override
    public Widget createWidget() {

        SecurityContext securityContext = presenter.getSecurityFramework().getSecurityContext(presenter.getProxy().getNameToken());
        ResourceDescription definition = presenter.getDescriptionRegistry().lookup(address);

        ToolStrip tools = buildTools();
        DefaultCellTable<Property> table = setupTable();
        Widget formPanel = buildFormPanel(definition, securityContext);

        // putting everything together
        MultipleToOneLayout layoutBuilder = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline("Workers")
                .setDescription(SafeHtmlUtils.fromString(
                        Console.CONSTANTS.pleaseChooseWorker()))
                .setMasterTools(tools)
                .setMaster(Console.MESSAGES.available("Worker"), table)
                .addDetail(Console.CONSTANTS.common_label_attributes(), formPanel);
        return layoutBuilder.build();
    }

    @Override
    protected void onAdd() {
        presenter.launchAddWorkerDialog();
    }

    @Override
    protected void onModify(final String name, final Map<String, Object> changedValues) {
        presenter.modifyWorker(name, changedValues);
    }

    @Override
    protected void onRemove(final String name) {
        Feedback.confirm(Console.MESSAGES.deleteTitle("Worker"),
                Console.MESSAGES.deleteConfirm("Worker '" + name + "'"),
                new Feedback.ConfirmationHandler() {
                    @Override
                    public void onConfirmation(boolean isConfirmed) {
                        if (isConfirmed) {
                            presenter.removeWorker(name);
                        }
                    }
                });
    }
}
