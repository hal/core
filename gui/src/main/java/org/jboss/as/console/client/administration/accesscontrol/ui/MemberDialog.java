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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.accesscontrol.AccessControlFinder;
import org.jboss.as.console.client.administration.accesscontrol.store.AddAssignment;
import org.jboss.as.console.client.administration.accesscontrol.store.Assignment;
import org.jboss.as.console.client.administration.accesscontrol.store.Principal;
import org.jboss.as.console.client.administration.accesscontrol.store.Principals;
import org.jboss.as.console.client.administration.accesscontrol.store.Role;
import org.jboss.as.console.client.widgets.lists.DefaultCellList;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.Set;

import static com.google.gwt.dom.client.Style.Unit.PX;
import static com.google.gwt.user.cellview.client.HasKeyboardPagingPolicy.KeyboardPagingPolicy.INCREASE_RANGE;
import static com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.BOUND_TO_SELECTION;
import static org.jboss.as.console.client.administration.accesscontrol.store.ModifiesAssignment.Relation.ROLE_TO_PRINCIPAL;

/**
 * @author Harald Pehl
 */
public class MemberDialog implements IsWidget {

    private final Role role;
    private final boolean include;
    private final Set<Principal> unassignedPrincipals;
    private final Dispatcher circuit;
    private final AccessControlFinder presenter;

    public MemberDialog(final Role role,
            final boolean include,
            final Set<Principal> unassignedPrincipals,
            final Dispatcher circuit,
            final AccessControlFinder presenter) {
        this.role = role;
        this.include = include;
        this.unassignedPrincipals = unassignedPrincipals;
        this.circuit = circuit;
        this.presenter = presenter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Widget asWidget() {
        ProvidesKey<Principal> keyProvider = Principal::getId;

        AbstractCell<Principal> roleCell = new AbstractCell<Principal>() {
            @Override
            public void render(final Context context, final Principal value, final SafeHtmlBuilder sb) {
                sb.append(Templates.memberItem("navigation-column-item", value));
            }
        };
        DefaultCellList<Principal> list = new DefaultCellList<>(roleCell, keyProvider);
        list.setPageSize(30);
        list.setKeyboardPagingPolicy(INCREASE_RANGE);
        list.setKeyboardSelectionPolicy(BOUND_TO_SELECTION);

        ListDataProvider<Principal> dataProvider = new ListDataProvider<>(keyProvider);
        dataProvider.setList(Principals.orderedByType()
                .compound(Principals.orderedByName())
                .immutableSortedCopy(unassignedPrincipals));
        dataProvider.addDataDisplay(list);

        SingleSelectionModel<Principal> selectionModel = new SingleSelectionModel<>(keyProvider);
        list.setSelectionModel(selectionModel);
        selectionModel.setSelected(dataProvider.getList().get(0), true);
        Scheduler.get().scheduleDeferred(() -> list.setFocus(true));

        Label errorMessage = new Label();
        errorMessage.setVisible(false);
        errorMessage.getElement().getStyle().setColor("#c82e2e");
        errorMessage.getElement().getStyle().setPadding(7, PX);

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");
        layout.add(errorMessage);
        layout.add(list);

        DialogueOptions options = new DialogueOptions(
                event -> {
                    if (selectionModel.getSelectedObject() == null) {
                        errorMessage.setText(Console.CONSTANTS.pleaseSelectPrincipal());
                        errorMessage.setVisible(true);
                    } else {
                        Assignment assignment = new Assignment(selectionModel.getSelectedObject(), role,
                                include);
                        circuit.dispatch(new AddAssignment(assignment, ROLE_TO_PRINCIPAL));
                        presenter.closeWindow();
                    }
                },
                event -> presenter.closeWindow()
        );

        return new WindowContentBuilder(layout, options).build();
    }
}
