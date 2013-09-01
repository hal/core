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

import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.Principal;
import org.jboss.as.console.client.administration.role.model.PrincipalStore;
import org.jboss.as.console.client.administration.role.model.PrincipalType;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.administration.role.model.RoleStore;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

/**
 * @author Harald Pehl
 */
public class AddRoleAssignmentWizard implements IsWidget {

    private final PrincipalType type;
    private final PrincipalStore principals;
    private final RoleStore roles;
    private final RoleAssignmentPresenter presenter;
    private final BeanFactory beanFactory;

    public AddRoleAssignmentWizard(final PrincipalType type, final PrincipalStore principals, final RoleStore roles,
            final RoleAssignmentPresenter presenter, BeanFactory beanFactory) {
        this.type = type;
        this.principals = principals;
        this.roles = roles;
        this.presenter = presenter;
        this.beanFactory = beanFactory;
    }

    @Override
    public Widget asWidget() {
        MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
        List<Principal> byType = principals.get(type);
        if (byType != null) {
            for (Principal principal : byType) {
                oracle.add(principal.getName());
            }
        }

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");

        final Form<RoleAssignment> form = new Form<RoleAssignment>(RoleAssignment.class);
        String title = type == GROUP ? Console.CONSTANTS.common_label_group() : Console.CONSTANTS.common_label_user();
        PrincipalFormItem principalItem = new PrincipalFormItem(type, "principal", title, beanFactory);
        principalItem.setRequired(true);
        principalItem.update(principals);
        TextBoxItem realmItem = new TextBoxItem("realm", "Realm", false);
        // TODO The rolesItem is not part of the focus chain because it's not
        // TODO recognized by org.jboss.ballroom.client.widgets.window.Focus
        RolesFormItem rolesItem = new RolesFormItem("roles", Console.CONSTANTS.common_label_roles(), 5);
        rolesItem.setRequired(true);
        if (type == GROUP) {
            PrincipalsFormItem excludesItem = new PrincipalsFormItem(type, "excludes",
                    Console.CONSTANTS.common_label_exclude(), beanFactory);
            form.setFields(principalItem, realmItem, rolesItem, excludesItem);
        } else {
            form.setFields(principalItem, realmItem, rolesItem);
        }
        layout.add(form.asWidget());
        rolesItem.setRoles(roles.getRoles());

        DialogueOptions options = new DialogueOptions(
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        FormValidation validation = form.validate();
                        if (!validation.hasErrors()) {
                            RoleAssignment assignment = form.getUpdatedEntity();
                            presenter.addRoleAssignment(assignment);
                        }
                    }
                },
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        presenter.closeDialog();
                    }
                }
        );

        return new WindowContentBuilder(layout, options).build();
    }
}
