/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.jboss.as.console.client.administration.role.ui;

import java.util.Map;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.administration.role.RoleAssignmentPresenter;
import org.jboss.as.console.client.administration.role.form.PojoForm;
import org.jboss.as.console.client.administration.role.form.ReadOnlyItem;
import org.jboss.as.console.client.administration.role.model.Role;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.FormCallback;

/**
 * @author Harald Pehl
 */
public class StandardRoleDetails implements IsWidget {

    private final RoleAssignmentPresenter presenter;
    private final PojoForm<Role> form;
    private ReadOnlyItem<String> nameItem;
    private CheckBoxItem includeAllItem;

    public StandardRoleDetails(final RoleAssignmentPresenter presenter) {
        this.presenter = presenter;
        this.form = new PojoForm<Role>();
    }

    @Override
    public Widget asWidget() {
        nameItem = new ReadOnlyItem<String>("name", "Name");
        includeAllItem = new CheckBoxItem("includeAll", "Include All");
        form.setFields(nameItem, includeAllItem);
        form.setEnabled(false);
        form.setToolsCallback(new FormCallback() {
            @Override
            public void onSave(final Map changeset) {
                Role edited = form.getEditedEntity();
                if (edited != null) {
                    edited.setIncludeAll(includeAllItem.getValue());
                    presenter.modifyIncludeAll(edited);
                }
            }

            @Override
            public void onCancel(final Object entity) {
            }
        });

        VerticalPanel content = new VerticalPanel();
        content.setStyleName("fill-layout-width");
        content.add(new StandardRoleHelpPanel().asWidget());
        content.add(form.asWidget());

        return content;
    }

    @SuppressWarnings("unchecked")
    void bind(CellTable<Role> table) {
        final SingleSelectionModel<Role> selectionModel = (SingleSelectionModel<Role>) table
                .getSelectionModel();
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(final SelectionChangeEvent event) {
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        updateFormValues(selectionModel.getSelectedObject());
                    }
                });
            }
        });
    }

    private void updateFormValues(final Role role) {
        if (role != null) {
            nameItem.setValue(role.getName());
            includeAllItem.setValue(role.isIncludeAll());
            form.setUndefined(false);
            form.edit(role);
        } else {
            form.clearValues();
        }
    }

    public void update(final Role selectedRole) {
        updateFormValues(selectedRole);
    }
}
