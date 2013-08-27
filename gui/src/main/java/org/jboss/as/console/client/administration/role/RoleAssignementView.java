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

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.dmr.client.dispatch.DispatchAsync;

/**
 * @author Harald Pehl
 */
public class RoleAssignementView extends SuspendableViewImpl implements RoleAssignmentPresenter.MyView {

    private final BeanFactory beanFactory;
    private final DispatchAsync dispatcher;
    private RoleAssignmentPresenter presenter;
    private GroupEditor groupEditor;
    private UserEditor userEditor;
    private ScopedRoleEditor scopedRoleEditor;

    @Inject
    public RoleAssignementView(final BeanFactory beanFactory, final DispatchAsync dispatcher) {
        this.beanFactory = beanFactory;
        this.dispatcher = dispatcher;
    }

    @Override
    public Widget createWidget() {
        groupEditor = new GroupEditor(presenter, beanFactory, dispatcher);
        userEditor = new UserEditor(presenter, beanFactory, dispatcher);
        scopedRoleEditor = new ScopedRoleEditor(presenter, beanFactory, dispatcher);

        DefaultTabLayoutPanel tabLayoutpanel = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        tabLayoutpanel.addStyleName("default-tabpanel");
        tabLayoutpanel.add(groupEditor.asWidget(), Console.CONSTANTS.common_label_groups(), true);
        tabLayoutpanel.add(userEditor.asWidget(), Console.CONSTANTS.common_label_users(), true);
        tabLayoutpanel.add(scopedRoleEditor.asWidget(), Console.CONSTANTS.administration_scoped_roles(), true);
        tabLayoutpanel.selectTab(0);

        return tabLayoutpanel;
    }

    @Override
    public void setPresenter(final RoleAssignmentPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void reset() {
        groupEditor.reset();
        userEditor.reset();
        scopedRoleEditor.reset();
    }
}
