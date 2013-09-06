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
package org.jboss.as.console.client.administration.role;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.PrincipalType;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.administration.role.model.Roles;
import org.jboss.as.console.client.rbac.Role;
import org.jboss.ballroom.client.widgets.forms.DisclosureGroupRenderer;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormCallback;

/**
 * @author Harald Pehl
 */
public class RoleAssignmentDetails implements IsWidget {

    private final PrincipalType type;
    private final RoleAssignmentPresenter presenter;
    private final Form<RoleAssignment> form;
    private final Map<String, Role> rolesBefore;
    private final Map<String, Role> excludesBefore;
    private RolesFormItem rolesItem;
    private RolesFormItem excludesItem;

    public RoleAssignmentDetails(final RoleAssignmentPresenter presenter, final PrincipalType type) {
        this.presenter = presenter;
        this.type = type;
        this.form = new Form<RoleAssignment>(RoleAssignment.class);
        this.rolesBefore = new HashMap<String, Role>();
        this.excludesBefore = new HashMap<String, Role>();
    }

    @Override
    public Widget asWidget() {
        rolesItem = new RolesFormItem("roles", Console.CONSTANTS.common_label_roles());
        excludesItem = new RolesFormItem("excludes", Console.CONSTANTS.common_label_exclude());
        excludesItem.setRequired(false);

        form.setFields(rolesItem);
        form.setFieldsInGroup(Console.CONSTANTS.common_label_advanced(), new DisclosureGroupRenderer(), excludesItem);
        form.setEnabled(false);
        form.setToolsCallback(new FormCallback() {
            @Override
            public void onSave(final Map changeset) {
                RoleAssignment assignment = form.getUpdatedEntity();
                assignment.setPrincipal(form.getEditedEntity().getPrincipal());
                assignment.setRoles(rolesItem.getValue());
                assignment.setExcludes(excludesItem.getValue());
                presenter.saveRoleAssignment(assignment, removedRoles(assignment), removedExcludes(assignment));
            }

            @Override
            public void onCancel(final Object entity) {
            }
        });

        VerticalPanel content = new VerticalPanel();
        content.setStyleName("fill-layout-width");
        content.add(form.asWidget());
        return content;
    }

    public void update(final Roles roles) {
        if (rolesItem != null) {
            rolesItem.update(roles);
        }
        if (excludesItem != null) {
            excludesItem.update(roles);
        }
    }

    @SuppressWarnings("unchecked")
    void bind(CellTable<RoleAssignment> table) {
        final SingleSelectionModel<RoleAssignment> selectionModel = (SingleSelectionModel<RoleAssignment>) table
                .getSelectionModel();
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(final SelectionChangeEvent event) {
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        rolesBefore.clear();
                        excludesBefore.clear();
                        RoleAssignment assignment = selectionModel.getSelectedObject();
                        if (assignment != null) {
                            for (Role role : assignment.getRoles()) {
                                rolesBefore.put(role.getName(), role);
                            }
                            for (Role role : assignment.getExcludes()) {
                                excludesBefore.put(role.getName(), role);
                            }
                            form.edit(assignment);
                        } else {
                            form.clearValues();
                        }
                    }
                });
            }
        });
    }

    private Collection<Role> removedRoles(final RoleAssignment assignment) {
        Map<String, Role> copy = new HashMap<String, Role>(rolesBefore);
        for (Role role : assignment.getRoles()) {
            copy.remove(role.getName());
        }
        return copy.values();
    }

    private Collection<Role> removedExcludes(final RoleAssignment assignment) {
        Map<String, Role> copy = new HashMap<String, Role>(excludesBefore);
        for (Role role : assignment.getExcludes()) {
            copy.remove(role.getName());
        }
        return copy.values();
    }
}
