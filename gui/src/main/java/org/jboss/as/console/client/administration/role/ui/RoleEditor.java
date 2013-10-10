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

import java.util.List;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.administration.role.RoleAssignmentPresenter;
import org.jboss.as.console.client.administration.role.model.Roles;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;

/**
 * @author Harald Pehl
 */
public class RoleEditor implements IsWidget {

    private final RoleAssignmentPresenter presenter;
    private final StandardRoleEditor standardRoleEditor;
    private final ScopedRoleEditor scopedRoleEditor;

    public RoleEditor(final RoleAssignmentPresenter presenter) {
        this.presenter = presenter;
        this.standardRoleEditor = new StandardRoleEditor(presenter);
        this.scopedRoleEditor = new ScopedRoleEditor(presenter);
    }

    @Override
    public Widget asWidget() {
        if (!presenter.isStandalone()) {
            VerticalPanel panel = new VerticalPanel();
            panel.setStyleName("rhs-content-panel");

            panel.add(new ContentHeaderLabel("Role Mangement"));
            //panel.add(new ContentDescription(Console.CONSTANTS.subys_tx_desc()));

            TabPanel tabs = new TabPanel();
            tabs.setStyleName("default-tabpanel");
            tabs.getElement().setAttribute("style", "margin-top:15px;");

            tabs.add(standardRoleEditor.asWidget(), "Standard Roles");
            tabs.add(scopedRoleEditor.asWidget(), "Scoped Roles");
            tabs.selectTab(0);

            panel.add(tabs);
            return new ScrollPanel(panel);
        } else {
            return new ScrollPanel(standardRoleEditor.asWidget());
        }
    }

    public void update(Roles roles, final List<String> hosts, final List<String> serverGroups) {
        standardRoleEditor.update(roles);
        if (!presenter.isStandalone()) {
            scopedRoleEditor.update(roles, hosts, serverGroups);
        }
    }
}
