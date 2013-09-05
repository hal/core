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

import static org.jboss.as.console.client.administration.role.model.PrincipalType.GROUP;

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
import org.jboss.as.console.client.administration.role.model.Principals;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.administration.role.model.Roles;
import org.jboss.as.console.client.rbac.Role;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormCallback;

/**
 * @author Harald Pehl
 */
public class RoleAssignmentDetails implements IsWidget {

    private final PrincipalType type;
    private final RoleAssignmentPresenter presenter;
    private final BeanFactory beanFactory;
    private final Form<RoleAssignment> form;
    private final Map<String, Role> rolesBefore;
    private RolesFormItem rolesItem;
//    private PrincipalsFormItem excludesItem;


    public RoleAssignmentDetails(final PrincipalType type, final RoleAssignmentPresenter presenter,
            final BeanFactory beanFactory) {
        this.presenter = presenter;
        this.type = type;
        this.beanFactory = beanFactory;
        this.form = new Form<RoleAssignment>(RoleAssignment.class);
        this.rolesBefore = new HashMap<String, Role>();
    }

    @Override
    public Widget asWidget() {
        rolesItem = new RolesFormItem("roles", Console.CONSTANTS.common_label_roles(), 7, type == GROUP);
        rolesItem.setRequired(true);
        form.setFields(rolesItem);
        form.setEnabled(false);
        form.setToolsCallback(new FormCallback() {
            @Override
            public void onSave(final Map changeset) {
                RoleAssignment assignment = form.getUpdatedEntity();
                // 'principal' was not part of the form
                assignment.setPrincipal(form.getEditedEntity().getPrincipal());
                // The form cannot handle enums
                assignment.setRoles(rolesItem.getValue());
                presenter.saveRoleAssignment(assignment, removedRoles(assignment));
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

    public void update(final Principals principals, final Roles roles) {
        if (rolesItem != null) {
            rolesItem.update(principals, roles);
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
                        RoleAssignment assignment = selectionModel.getSelectedObject();
                        if (assignment != null) {
                            rolesItem.setRoleAssignment(assignment);
                            for (Role role : assignment.getRoles()) {
                                rolesBefore.put(role.getName(), role);
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
}
