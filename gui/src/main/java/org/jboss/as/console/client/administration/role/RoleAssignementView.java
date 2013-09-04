/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
import static org.jboss.as.console.client.administration.role.model.PrincipalType.USER;

import java.util.List;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.Principals;
import org.jboss.as.console.client.administration.role.model.RoleAssignments;
import org.jboss.as.console.client.administration.role.model.Roles;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;

/**
 * @author Harald Pehl
 */
public class RoleAssignementView extends SuspendableViewImpl implements RoleAssignmentPresenter.MyView {

    private final BeanFactory beanFactory;
    private RoleAssignmentPresenter presenter;
    private RoleAssignmentEditor groupEditor;
    private RoleAssignmentEditor userEditor;
    private ScopedRoleEditor scopedRoleEditor;

    @Inject
    public RoleAssignementView(final BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public Widget createWidget() {
        groupEditor = new RoleAssignmentEditor(GROUP, presenter, beanFactory);
        userEditor = new RoleAssignmentEditor(USER, presenter, beanFactory);
        scopedRoleEditor = new ScopedRoleEditor(presenter);

        DefaultTabLayoutPanel tabLayoutpanel = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        tabLayoutpanel.addStyleName("default-tabpanel");
        tabLayoutpanel.add(groupEditor, Console.CONSTANTS.common_label_groups());
        tabLayoutpanel.add(userEditor, Console.CONSTANTS.common_label_users());
        tabLayoutpanel.add(scopedRoleEditor, Console.CONSTANTS.administration_scoped_roles());
        tabLayoutpanel.selectTab(0);

        return tabLayoutpanel;
    }

    @Override
    public void setPresenter(final RoleAssignmentPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void update(final Principals principals, RoleAssignments assignments, Roles roles, final List<String> hosts,
            final List<String> serverGroups) {
        groupEditor.update(principals, assignments, roles);
        userEditor.update(principals, assignments, roles);
        scopedRoleEditor.update(roles, hosts, serverGroups);
    }
}
