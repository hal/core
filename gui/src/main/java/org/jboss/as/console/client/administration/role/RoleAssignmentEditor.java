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

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.Principal;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.rbac.StandardRole;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.widgets.browser.DefaultCellBrowser;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.dispatch.DispatchAsync;

/**
 * @author Harald Pehl
 */
public class RoleAssignmentEditor implements IsWidget {

    private final Principal.Type principalType;
    private final BeanFactory beanFactory;
    private final DispatchAsync dispatcher;
    private RoleAssignmentPresenter presenter;
    private RoleAssignmentTreeModel treeModel;
    private ToolButton addButton;
    private ToolButton deleteButton;


    public RoleAssignmentEditor(final Principal.Type principalType, final BeanFactory beanFactory,
            final DispatchAsync dispatcher) {
        this.principalType = principalType;
        this.beanFactory = beanFactory;
        this.dispatcher = dispatcher;
    }

    @Override
    public Widget asWidget() {

        treeModel = new RoleAssignmentTreeModel(principalType, beanFactory, dispatcher);
        treeModel.getRoleSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(final SelectionChangeEvent event) {
                treeModel.getPrincipalSelectionModel().clear();
                updateToolButtons();
            }
        });
        treeModel.getRoleAssignmentSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(final SelectionChangeEvent event) {
                updateToolButtons();
            }
        });
        treeModel.getPrincipalSelectionModel().addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(final SelectionChangeEvent event) {
                updateToolButtons();
            }
        });
        DefaultCellBrowser cellBrowser = new DefaultCellBrowser.Builder(treeModel, null).build();
        cellBrowser.setHeight("250px");

        String add = principalType == Principal.Type.USER ? Console.CONSTANTS.role_assignment_add_user() : Console
                .CONSTANTS.role_assignment_add_group();
        String delete = principalType == Principal.Type.USER ? Console.CONSTANTS.role_assignment_delete_user() : Console
                .CONSTANTS.role_assignment_delete_group();

        ToolStrip tools = new ToolStrip();
        addButton = new ToolButton(add, new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                StandardRole role = treeModel.getRoleSelectionModel().getSelectedObject();
                RoleAssignment roleAssignment = treeModel.getRoleAssignmentSelectionModel().getSelectedObject();
                if (role != null && roleAssignment != null) {
                    presenter.launchAddDialg(role, roleAssignment, principalType);
                } else {
                    // TODO Change this to a console message?
                    Log.warn("No role or no includes/excludes selected");
                }
            }
        });
        addButton.setEnabled(false);
        tools.addToolButtonRight(addButton);
        deleteButton = new ToolButton(delete, new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final StandardRole role = treeModel.getRoleSelectionModel().getSelectedObject();
                final RoleAssignment roleAssignment = treeModel.getRoleAssignmentSelectionModel().getSelectedObject();
                final Principal principal = treeModel.getPrincipalSelectionModel().getSelectedObject();
                if (role != null && roleAssignment != null && principal != null) {
                    String principalTypeName = principalType == Principal.Type.USER ? Console.CONSTANTS
                            .common_label_user() : Console.CONSTANTS.common_label_group();
                    Feedback.confirm(
                            Console.MESSAGES.deleteTitle(principal.getName()),
                            Console.MESSAGES.deleteConfirm(principalTypeName + " " + principal.getName()),
                            new Feedback.ConfirmationHandler() {
                                @Override
                                public void onConfirmation(boolean isConfirmed) {
                                    if (isConfirmed) {
                                        presenter.onDelete(role, roleAssignment, principal);
                                    }
                                }
                            });
                } else {
                    // TODO Change this to a console message?
                    Log.warn("No role or no includes/excludes or no principal selected");
                }
            }
        });
        deleteButton.setEnabled(false);
        tools.addToolButtonRight(deleteButton);

        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setStyleName("rhs-content-panel");
        vpanel.add(tools);
        vpanel.add(cellBrowser);
        return vpanel;
    }

    private void updateToolButtons() {
        StandardRole role = treeModel.getRoleSelectionModel().getSelectedObject();
        RoleAssignment roleAssignment = treeModel.getRoleAssignmentSelectionModel().getSelectedObject();
        Principal principal = treeModel.getPrincipalSelectionModel().getSelectedObject();

        addButton.setEnabled(role != null && roleAssignment != null);
        deleteButton.setEnabled(role != null && roleAssignment != null && principal != null);
    }

    public void setPresenter(final RoleAssignmentPresenter presenter) {
        this.presenter = presenter;
    }

    public void refresh() {
        treeModel.refreshRoleAssignments();
    }
}
