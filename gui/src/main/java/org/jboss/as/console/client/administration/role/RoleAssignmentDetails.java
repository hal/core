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

import java.util.Map;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.PrincipalStore;
import org.jboss.as.console.client.administration.role.model.PrincipalType;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.administration.role.model.RoleAssignmentStore;
import org.jboss.as.console.client.administration.role.model.RoleStore;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.Form;

/**
 * @author Harald Pehl
 */
public class RoleAssignmentDetails implements IsWidget {

    private final PrincipalType type;
    private final RoleAssignmentPresenter presenter;
    private final BeanFactory beanFactory;
    private final Form<RoleAssignment> form;
    private RolesFormItem rolesItem;
    private PrincipalsFormItem excludesItem;

    public RoleAssignmentDetails(final PrincipalType type, final RoleAssignmentPresenter presenter,
            final BeanFactory beanFactory) {
        this.presenter = presenter;
        this.type = type;
        this.beanFactory = beanFactory;
        this.form = new Form<RoleAssignment>(RoleAssignment.class);
    }

    @Override
    public Widget asWidget() {
        VerticalPanel content = new VerticalPanel();
        content.setStyleName("fill-layout-width");

        FormToolStrip<RoleAssignment> toolStrip = new FormToolStrip<RoleAssignment>(form,
                new FormToolStrip.FormCallback<RoleAssignment>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                    }

                    @Override
                    public void onDelete(RoleAssignment assignment) {
                    }
                });
        toolStrip.providesDeleteOp(false);
        content.add(toolStrip.asWidget());

        rolesItem = new RolesFormItem("roles", Console.CONSTANTS.common_label_roles());
        if (type == GROUP) {
            excludesItem = new PrincipalsFormItem(type, "excludes", Console.CONSTANTS.common_label_exclude(),
                    beanFactory
            );
            form.setFields(rolesItem, excludesItem);
        } else {
            form.setFields(rolesItem);
        }
        form.setEnabled(false);
        content.add(form.asWidget());

        return content;
    }

    public void update(final PrincipalStore principals, final RoleAssignmentStore assignments, final RoleStore roles) {
        if (rolesItem != null) {
            rolesItem.setRoles(roles.getRoles());
        }
        if (type == GROUP && excludesItem != null) {
            excludesItem.update(principals);
        }
    }

    void bind(CellTable<RoleAssignment> table) {
        form.bind(table);
    }
}
