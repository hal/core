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

import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.as.console.client.rbac.RolesHelpPanel;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.dmr.client.dispatch.DispatchAsync;

/**
 * @author Heiko Braun
 * @date 5/17/11
 */
public class RoleAssignementView extends SuspendableViewImpl implements RoleAssignmentPresenter.MyView {

    private final BeanFactory beanFactory;
    private final DispatchAsync dispatcher;
    private RoleAssignmentPresenter presenter;
    private TabPanel editors;
    private RoleAssignmentEditor groupEditor;
    private RoleAssignmentEditor userEditor;

    @Inject
    public RoleAssignementView(final BeanFactory beanFactory, final DispatchAsync dispatcher) {
        this.beanFactory = beanFactory;
        this.dispatcher = dispatcher;
    }

    @Override
    public Widget createWidget() {
        groupEditor = new RoleAssignmentEditor(Principal.Type.GROUP, beanFactory, dispatcher);
        groupEditor.setPresenter(presenter);
        userEditor = new RoleAssignmentEditor(Principal.Type.USER, beanFactory, dispatcher);
        userEditor.setPresenter(presenter);

        editors = new TabPanel();
        editors.setStyleName("default-tabpanel");
        editors.add(groupEditor, Console.CONSTANTS.common_label_groups());
        editors.add(userEditor, Console.CONSTANTS.common_label_users());
        editors.selectTab(0);

        SimpleLayout layout = new SimpleLayout()
                .setTitle(Console.CONSTANTS.common_label_roles())
                .setHeadline(Console.CONSTANTS.role_assignment())
                .setDescription(Console.CONSTANTS.role_assignment_desc())
                .addContent("help", new RolesHelpPanel().asWidget())
                .addContent("editors", editors);
        return layout.build();
    }

    @Override
    public void setPresenter(final RoleAssignmentPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void refresh() {
        if (editors.getTabBar().getSelectedTab() == 0) {
            groupEditor.refresh();
        } else {
            userEditor.refresh();
        }
    }
}
