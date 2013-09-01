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

import java.util.Map;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.PrincipalStore;
import org.jboss.as.console.client.administration.role.model.RoleAssignmentStore;
import org.jboss.as.console.client.administration.role.model.RoleStore;
import org.jboss.as.console.client.administration.role.model.ScopedRole;
import org.jboss.as.console.client.widgets.forms.FormToolStrip;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.TextBoxItem;

/**
 * @author Harald Pehl
 */
public class ScopedRoleDetails implements IsWidget {

    private final RoleAssignmentPresenter presenter;
    private final Form<ScopedRole> form;

    public ScopedRoleDetails(final RoleAssignmentPresenter presenter) {
        this.presenter = presenter;
        this.form = new Form<ScopedRole>(ScopedRole.class);
    }

    @Override
    public Widget asWidget() {
        VerticalPanel content = new VerticalPanel();
        content.setStyleName("fill-layout-width");

        FormToolStrip<ScopedRole> toolStrip = new FormToolStrip<ScopedRole>(form,
                new FormToolStrip.FormCallback<ScopedRole>() {
                    @Override
                    public void onSave(Map<String, Object> changeset) {
                        presenter.saveScopedRole(form.getEditedEntity(), form.getChangedValues());
                    }

                    @Override
                    public void onDelete(ScopedRole scopedRole) {
                    }
                });
        toolStrip.providesDeleteOp(false);
        content.add(toolStrip.asWidget());

        TextBoxItem nameItem = new TextBoxItem("name", Console.CONSTANTS.common_label_name());
        form.setFields(nameItem);
        form.setEnabled(false);
        content.add(form.asWidget());

        return content;
    }

    public void update(final PrincipalStore principals, final RoleAssignmentStore assignments, final RoleStore roles) {
    }

    void bind(CellTable<ScopedRole> table) {
        form.bind(table);
    }
}
