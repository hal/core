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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.RoleAssignmentPresenter;
import org.jboss.as.console.client.administration.role.form.MultiselectListBoxItem;
import org.jboss.as.console.client.administration.role.form.PojoForm;
import org.jboss.as.console.client.administration.role.form.StandardRoleFormItem;
import org.jboss.as.console.client.administration.role.model.Role;
import org.jboss.as.console.client.core.EnumLabelLookup;
import org.jboss.ballroom.client.widgets.forms.CheckBoxItem;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.forms.TextItem;

/**
 * @author Harald Pehl
 */
public class ScopedRoleDetails implements IsWidget {

    private final RoleAssignmentPresenter presenter;
    private final PojoForm<Role> form;
    private List<String> hosts;
    private List<String> serverGroups;
    private TextItem nameItem;
    private StandardRoleFormItem baseRoleItem;
    private TextItem typeItem;
    private MultiselectListBoxItem scopeItem;
    private CheckBoxItem includeAllItem;

    public ScopedRoleDetails(final RoleAssignmentPresenter presenter) {
        this.presenter = presenter;
        this.form = new PojoForm<Role>();
    }

    @Override
    public Widget asWidget() {
        nameItem = new TextItem("name", Console.CONSTANTS.common_label_name());
        baseRoleItem = new StandardRoleFormItem("baseRole", "Base Role");
        typeItem = new TextItem("type", Console.CONSTANTS.common_label_type());
        scopeItem = new MultiselectListBoxItem("scope", "Scope", 3);
        includeAllItem = new CheckBoxItem("includeAll", "Include All");
        form.setFields(nameItem, baseRoleItem, typeItem, scopeItem, includeAllItem);
        form.setEnabled(false);
        form.setToolsCallback(new FormCallback() {
            @Override
            public void onSave(final Map changeSet) {
                Role edited = form.getEditedEntity();
                if (edited != null) {
                    edited.setBaseRole(baseRoleItem.getValue());
                    edited.setScope(scopeItem.getValue());
                    edited.setIncludeAll(includeAllItem.getValue());
                    presenter.saveScopedRole(edited);
                }
            }

            @Override
            public void onCancel(final Object entity) {
            }
        });

        VerticalPanel content = new VerticalPanel();
        content.setStyleName("fill-layout-width");
        content.add(new ScopedRoleHelpPanel().asWidget());
        content.add(form.asWidget());

        return content;
    }

    public void update(final List<Role> scopedRoles, final List<String> hosts, final List<String> serverGroups,
            final Role selectedRole) {
        this.hosts = hosts;
        this.serverGroups = serverGroups;
        baseRoleItem.setValues();
        if (scopedRoles.isEmpty()) {
            form.clearValues();
        } else {
            updateFormValues(selectedRole);
        }
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
            updateScope(role.getType());
            nameItem.setValue(role.getName());
            baseRoleItem.setValue(role.getBaseRole());
            typeItem.setValue(EnumLabelLookup.labelFor("ScopeType", role.getType()));
            scopeItem.setValue(new ArrayList<String>(role.getScope()));
            includeAllItem.setValue(role.isIncludeAll());
            form.setUndefined(false);
            form.edit(role);
        } else {
            form.clearValues();
        }
    }

    private void updateScope(final Role.Type type) {
        if (typeItem != null && scopeItem != null) {
            if (type == Role.Type.HOST) {
                scopeItem.setChoices(hosts);
            } else if (type == Role.Type.SERVER_GROUP) {
                scopeItem.setChoices(serverGroups);
            }
            // restore selection
            Role entity = form.getEditedEntity();
            if (entity != null) {
                scopeItem.setValue(new ArrayList<String>(entity.getScope()));
            }
        }
    }
}
