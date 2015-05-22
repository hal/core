/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.administration.accesscontrol.ui;

import com.google.common.collect.Sets;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.accesscontrol.AccessControlFinder;
import org.jboss.as.console.client.administration.accesscontrol.store.AccessControlStore;
import org.jboss.as.console.client.administration.accesscontrol.store.Assignment;
import org.jboss.as.console.client.administration.accesscontrol.store.Principal;
import org.jboss.as.console.client.administration.accesscontrol.store.Role;
import org.jboss.as.console.client.administration.accesscontrol.store.Roles;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.List;
import java.util.Set;

/**
 * @author Harald Pehl
 */
public class AssignmentDialog implements IsWidget {

    private final Principal principal;
    private final boolean include;
    private final AccessControlStore accessControlStore;
    private final Dispatcher circuit;
    private final AccessControlFinder presenter;

    public AssignmentDialog(final Principal principal,
            final boolean include,
            final AccessControlStore accessControlStore,
            final Dispatcher circuit,
            final AccessControlFinder presenter) {
        this.principal = principal;
        this.include = include;
        this.accessControlStore = accessControlStore;
        this.circuit = circuit;
        this.presenter = presenter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        ProvidesKey<Role> keyProvider = Role::getId;
        DefaultCellTable<Role> table = new DefaultCellTable<>(7, keyProvider);

        ListDataProvider<Role> dataProvider = new ListDataProvider<>(keyProvider);
        dataProvider.setList(unassignedRoles());
        dataProvider.addDataDisplay(table);

        SingleSelectionModel<Role> selectionModel = new SingleSelectionModel<>(keyProvider);
        table.setSelectionModel(selectionModel);

        TextColumn<Role> nameColumn = new TextColumn<Role>() {
            @Override
            public String getValue(Role role) {
                return role.getName();
            }
        };
        table.addColumn(nameColumn, Console.CONSTANTS.common_label_name());

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);

        Label errorMessage = new Label();
        errorMessage.setVisible(false);
        errorMessage.getElement().getStyle().setColor("#c82e2e");

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");
        layout.add(table);
        layout.add(pager);
        layout.add(errorMessage);

        DialogueOptions options = new DialogueOptions(
                event -> {
                    if (selectionModel.getSelectedObject() == null) {
                        errorMessage.setText("Please select a role");
                        errorMessage.setVisible(true);
                    } else {
                        Console.warning("Not yet implemented");
//                        circuit.dispatch(new AddAssignment(
//                                new Assignment(principal, selectionModel.getSelectedObject(), include)));
                        presenter.closeWindow();
                    }
                },
                event -> presenter.closeWindow()
        );

        return new WindowContentBuilder(layout, options).build();
    }

    private List<Role> unassignedRoles() {
        Set<Role> unassignedRoles = Sets.newHashSet(accessControlStore.getRoles());
        Iterable<Assignment> assignments = accessControlStore.getAssignments(principal, include);
        for (Assignment assignment : assignments) {
            unassignedRoles.remove(assignment.getRole());
        }
        return Roles.orderedByType().compound(Roles.orderedByName()).immutableSortedCopy(unassignedRoles);
    }
}
