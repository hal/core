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

import static org.jboss.as.console.client.administration.role.model.Principal.Type.GROUP;

import java.util.Collection;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.Principal;
import org.jboss.as.console.client.administration.role.model.Principals;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.administration.role.model.Roles;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

/**
 * @author Harald Pehl
 */
public class AddRoleAssignmentWizard implements IsWidget {

    private final RoleAssignmentPresenter presenter;
    private final Principal.Type type;
    private final Principals principals;
    private final Roles roles;

    public AddRoleAssignmentWizard(final RoleAssignmentPresenter presenter, final Principal.Type type,
            final Principals principals, final Roles roles) {
        this.presenter = presenter;
        this.type = type;
        this.principals = principals;
        this.roles = roles;
    }

    @Override
    public Widget asWidget() {
        MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
        Collection<Principal> byType = principals.get(type);
        if (byType != null) {
            for (Principal principal : byType) {
                oracle.add(principal.getName());
            }
        }

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");

        final Form<RoleAssignment> form = new Form<RoleAssignment>(RoleAssignment.class);
        String title = type == GROUP ? Console.CONSTANTS.common_label_group() : Console.CONSTANTS.common_label_user();
        final PrincipalFormItem principalItem = new PrincipalFormItem(type, "principal", title);
        principalItem.setRequired(true);
        principalItem.update(principals);
        TextBoxItem realmItem = new TextBoxItem("realm", "Realm", false);
        // TODO The rolesItem is not part of the focus chain because it's not
        // TODO recognized by org.jboss.ballroom.client.widgets.window.Focus
        final RolesFormItem rolesItem = new RolesFormItem("roles", Console.CONSTANTS.common_label_roles());
        rolesItem.setRequired(true);
        form.setFields(principalItem, realmItem, rolesItem);
        layout.add(form.asWidget());
        rolesItem.update(roles);

        DialogueOptions options = new DialogueOptions(
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        FormValidation validation = form.validate();
                        if (!validation.hasErrors()) {
                            RoleAssignment roleAssignment = new RoleAssignment(principalItem.getValue());
                            roleAssignment.addRoles(rolesItem.getValue());
                            presenter.addRoleAssignment(roleAssignment);
                        }
                    }
                },
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        presenter.closeWindow();
                    }
                }
        );

        return new WindowContentBuilder(layout, options).build();
    }
}
