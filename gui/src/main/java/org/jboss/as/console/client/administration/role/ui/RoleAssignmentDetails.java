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

import static org.jboss.as.console.client.administration.role.form.IncludeExcludeFormItem.Type.EXCLUDE;
import static org.jboss.as.console.client.administration.role.form.IncludeExcludeFormItem.Type.INCLUDE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.RoleAssignmentPresenter;
import org.jboss.as.console.client.administration.role.form.IncludeExcludeFormItem;
import org.jboss.as.console.client.administration.role.form.PojoForm;
import org.jboss.as.console.client.administration.role.form.ReadOnlyItem;
import org.jboss.as.console.client.administration.role.model.Principal;
import org.jboss.as.console.client.administration.role.model.Role;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.administration.role.model.RoleAssignments;
import org.jboss.as.console.client.administration.role.model.Roles;
import org.jboss.ballroom.client.widgets.forms.FormCallback;

/**
 * @author Harald Pehl
 */
public class RoleAssignmentDetails implements IsWidget {

    private final Principal.Type type;
    private final RoleAssignmentPresenter presenter;
    private final PojoForm<RoleAssignment> form;
    private ReadOnlyItem<RoleAssignment> principalItem;
    private IncludeExcludeFormItem includeExcludeFormItem;

    public RoleAssignmentDetails(final RoleAssignmentPresenter presenter, final Principal.Type type) {
        this.presenter = presenter;
        this.type = type;
        this.form = new PojoForm<RoleAssignment>();
    }

    @Override
    public Widget asWidget() {
        principalItem = new ReadOnlyItem<RoleAssignment>("principal",
                type == Principal.Type.GROUP ? Console.CONSTANTS.common_label_group() : Console.CONSTANTS
                        .common_label_user()) {
            @Override
            public String asString() {
                StringBuilder builder = new StringBuilder();
                RoleAssignment assignment = getValue();
                if (assignment != null) {
                    builder.append(assignment.getPrincipal().getName());
                    if (assignment.getRealm() != null) {
                        builder.append("@").append(assignment.getRealm());
                    }
                }
                return builder.toString();
            }
        };
        principalItem.setEnabled(false);
        includeExcludeFormItem = new IncludeExcludeFormItem("n/a", Console.CONSTANTS.common_label_roles());

        form.setFields(principalItem, includeExcludeFormItem);
        //        form.setFieldsInGroup(Console.CONSTANTS.common_label_advanced(), new DisclosureGroupRenderer(), excludesItem);
        form.setEnabled(false);
        form.setToolsCallback(new FormCallback() {
            @Override
            public void onSave(final Map changeset) {
                RoleAssignment edited = form.getEditedEntity();
                if (edited != null) {
                    // create a new role assignment to calculate the include / exclude diffs
                    Map<IncludeExcludeFormItem.Type, Set<Role>> includesExcludes = includeExcludeFormItem.getValue();
                    RoleAssignment newRoleAssignment = new RoleAssignment(edited.getPrincipal());
                    newRoleAssignment.setRealm(edited.getRealm());
                    newRoleAssignment.addRoles(includesExcludes.get(INCLUDE));
                    newRoleAssignment.addExcludes(includesExcludes.get(EXCLUDE));
                    presenter.saveRoleAssignment(newRoleAssignment, newRoleAssignment.changedRoles(edited),
                            newRoleAssignment.changedExcludes(edited));
                }
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

    public void update(final RoleAssignments assignments, final Roles roles, final RoleAssignment selectedAssignment) {
        if (includeExcludeFormItem != null) {
            includeExcludeFormItem.update(roles);
        }
        if (assignments.get(type).isEmpty()) {
            form.clearValues();
        } else {
            updateFormFields(selectedAssignment);
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
                        RoleAssignment selected = selectionModel.getSelectedObject();
                        updateFormFields(selected);
                    }
                });
            }
        });
    }

    private void updateFormFields(final RoleAssignment assignment) {
        if (assignment != null) {
            principalItem.setValue(assignment);
            Map<IncludeExcludeFormItem.Type, Set<Role>> includeExclude = new HashMap<IncludeExcludeFormItem.Type, Set<Role>>();
            includeExclude.put(INCLUDE, new HashSet<Role>(assignment.getRoles()));
            includeExclude.put(EXCLUDE, new HashSet<Role>(assignment.getExcludes()));
            includeExcludeFormItem.setValue(includeExclude);
            form.setUndefined(false);
            form.edit(assignment);
        } else {
            form.clearValues();
        }
    }
}
