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

import static org.jboss.as.console.client.administration.role.model.Role.Type.HOST;
import static org.jboss.as.console.client.administration.role.model.Role.Type.SERVER_GROUP;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.administration.role.RoleAssignmentPresenter;
import org.jboss.as.console.client.administration.role.form.EnumFormItem;
import org.jboss.as.console.client.administration.role.form.MultiselectListBoxItem;
import org.jboss.as.console.client.administration.role.form.PojoForm;
import org.jboss.as.console.client.administration.role.form.StandardRoleFormItem;
import org.jboss.as.console.client.administration.role.model.Role;
import org.jboss.as.console.client.rbac.StandardRole;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

/**
 * @author Harald Pehl
 */
public class AddScopedRoleWizard implements IsWidget {

    private final List<String> hosts;
    private final List<String> serverGroups;
    private final RoleAssignmentPresenter presenter;

    public AddScopedRoleWizard(final List<String> hosts, final List<String> serverGroups,
            final RoleAssignmentPresenter presenter) {
        this.hosts = hosts;
        this.serverGroups = serverGroups;
        this.presenter = presenter;
    }

    @Override
    public Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");

        final PojoForm<Role> form = new PojoForm<Role>();
        final TextBoxItem nameItem = new TextBoxItem("name", "Name");
        final StandardRoleFormItem baseRoleItem = new StandardRoleFormItem("baseRole", "Base Role");
        baseRoleItem.setValues();
        final EnumFormItem<Role.Type> typeItem = new EnumFormItem<Role.Type>("type", "Type");
        typeItem.setDefaultToFirst(true);
        typeItem.setValues(UIHelper.enumFormItemsForScopedRoleTyp());
        final MultiselectListBoxItem scopeItem = new MultiselectListBoxItem("scope", "Scope", 3);
        final CheckBoxItem includeAllItem = new CheckBoxItem("includeAll", "Include All");
        form.setFields(nameItem, baseRoleItem, typeItem, scopeItem, includeAllItem);
        layout.add(new ScopedRoleHelpPanel().asWidget());
        layout.add(form.asWidget());

        typeItem.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(final ChangeEvent event) {
                Role.Type type = typeItem.getValue();
                updateScope(type, scopeItem, form);
            }
        });
        updateScope(typeItem.getValue(), scopeItem, form);

        DialogueOptions options = new DialogueOptions(
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        FormValidation validation = form.validate();
                        if (!validation.hasErrors()) {
                            Role scopedRole = new Role(nameItem.getValue(), nameItem.getValue(),
                                    baseRoleItem.getValue(), typeItem.getValue(), scopeItem.getValue());
                            scopedRole.setIncludeAll(includeAllItem.getValue());
                            presenter.addScopedRole(scopedRole);
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

    private void updateScope(final Role.Type type, final MultiselectListBoxItem scopeItem,
            final PojoForm<Role> form) {
        if (type == HOST) {
            scopeItem.setChoices(hosts);
        } else if (type == SERVER_GROUP) {
            scopeItem.setChoices(serverGroups);
        }
        // restore selection
        Role entity = form.getEditedEntity();
        if (entity != null) {
            scopeItem.setValue(new ArrayList<String>(entity.getScope()));
        }
    }
}
