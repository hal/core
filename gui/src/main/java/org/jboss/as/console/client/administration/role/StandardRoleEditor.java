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

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.Roles;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.ContentGroupLabel;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;

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
        LayoutPanel layout = new LayoutPanel();
        VerticalPanel content = new VerticalPanel();
        content.setStyleName("rhs-content-panel");
        ScrollPanel scroll = new ScrollPanel(content);
        layout.add(scroll);
        layout.setWidgetTopHeight(scroll, 0, Style.Unit.PX, 100, Style.Unit.PCT);

        // header and desc
        content.add(new ContentHeaderLabel(Console.CONSTANTS.administration_standard_roles()));
        content.add(new ContentDescription(Console.CONSTANTS.administration_standard_roles_desc()));

        // table
        content.add(table);

        // details
        details.bind(table.getCellTable());
        content.add(new ContentGroupLabel(Console.CONSTANTS.common_label_selection()));
        content.add(details);

        return layout;
    }

    public void update(final Roles roles) {
        table.update(roles.getStandardRoles());
    }
}
