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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.RoleAssignmentPresenter;
import org.jboss.as.console.client.administration.accesscontrol.store.Roles;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.ContentGroupLabel;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;

/**
 * @author Harald Pehl
 */
public class StandardRoleEditor implements IsWidget {

    private final RoleAssignmentPresenter presenter;
    private final RoleTable table;
    private final StandardRoleDetails details;

    public StandardRoleEditor(final RoleAssignmentPresenter presenter) {
        this.presenter = presenter;
        this.table = new StandardRoleTable();
        this.details = new StandardRoleDetails(presenter);
    }

    @Override
    public Widget asWidget() {
        // container and panels
        VerticalPanel layout = new VerticalPanel();

        if(Console.MODULES.getBootstrapContext().isStandalone())
        {
            layout.setStyleName("rhs-content-panel");
            layout.add(new ContentHeaderLabel("Role Management"));
        }
        else
        {
            layout.setStyleName("fill-layout-width");
        }

        layout.add(new ContentDescription(Console.CONSTANTS.administration_standard_roles_desc()));

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton(Console.CONSTANTS.administration_members(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.showMembers(table.getSelectedRole());
            }
        }));
        layout.add(tools.asWidget());

        // table
        layout.add(table);

        // details
        details.bind(table.getCellTable());
        layout.add(new ContentGroupLabel(Console.CONSTANTS.common_label_selection()));
        layout.add(details);

        return layout;
    }

    public void update(final Roles roles) {
//        table.update(roles.getStandardRoles());
        details.update(table.getSelectedRole());
    }
}
