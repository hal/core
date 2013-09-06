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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.ScopedRole;
import org.jboss.as.console.client.rbac.StandardRole;
import org.jboss.ballroom.client.widgets.forms.Form;
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

        final Form<ScopedRole> form = new Form<ScopedRole>(ScopedRole.class);
        TextBoxItem nameItem = new TextBoxItem("name", Console.CONSTANTS.common_label_name());
        final EnumFormItem<StandardRole> baseRoleItem = new EnumFormItem<StandardRole>("baseRole",
                Console.CONSTANTS.administration_base_role());
        baseRoleItem.setValues(UIHelper.enumFormItemsForStandardRole());
        final EnumFormItem<ScopedRole.Type> typeItem = new EnumFormItem<ScopedRole.Type>("type", Console.CONSTANTS.common_label_type());
        typeItem.setDefaultToFirst(true);
        typeItem.setValues(UIHelper.enumFormItemsForScopedRoleTyp());
        final MultiselectListBoxItem scopeItem = new MultiselectListBoxItem("scope", Console.CONSTANTS.administration_scope(), 3);
        form.setFields(nameItem, baseRoleItem, typeItem, scopeItem);
        layout.add(form.asWidget());

        typeItem.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(final ChangeEvent event) {
                ScopedRole.Type type = typeItem.getValue();
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
                            ScopedRole scopedRole = form.getUpdatedEntity();
                            // The form cannot handle enums...
                            scopedRole.setBaseRole(baseRoleItem.getValue());
                            scopedRole.setType(typeItem.getValue());
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

    private void updateScope(final ScopedRole.Type type, final MultiselectListBoxItem scopeItem,
            final Form<ScopedRole> form) {
        if (type == ScopedRole.Type.HOST) {
            scopeItem.setChoices(hosts);
        } else if (type == ScopedRole.Type.SERVER_GROUP) {
            scopeItem.setChoices(serverGroups);
        }
        // restore selection
        ScopedRole entity = form.getEditedEntity();
        if (entity != null) {
            scopeItem.setValue(new ArrayList<String>(entity.getScope()));
        }
    }
}
