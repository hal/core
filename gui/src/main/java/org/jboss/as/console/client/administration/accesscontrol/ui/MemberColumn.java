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

import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.accesscontrol.AccessControlFinder;
import org.jboss.as.console.client.administration.accesscontrol.store.AccessControlStore;
import org.jboss.as.console.client.administration.accesscontrol.store.Assignment;
import org.jboss.as.console.client.administration.accesscontrol.store.RemoveAssignment;
import org.jboss.as.console.client.administration.accesscontrol.store.Role;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.gwt.circuit.Dispatcher;

import static org.jboss.as.console.client.administration.accesscontrol.store.ModifiesAssignment.Relation.ROLE_TO_PRINCIPAL;

/**
 * Column for the assignment of a selected principal or role.
 *
 * @author Harald Pehl
 */
public class MemberColumn extends FinderColumn<Assignment> {

    private Widget widget;

    @SuppressWarnings({"unchecked", "Convert2MethodRef"})
    public MemberColumn(final AccessControlStore accessControlStore,
            final Dispatcher circuit,
            final AccessControlFinder presenter,
            final ColumnManager columnManager,
            final Supplier<Role> selectedRole,
            final Supplier<Boolean> include,
            final String token) {

        super(FinderId.ACCESS_CONTROL,
                Console.CONSTANTS.member(),
                new Display<Assignment>() {
                    @Override
                    public boolean isFolder(final Assignment data) {
                        return false;
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final Assignment data) {
                        return Templates.memberItem(baseCss, data.getPrincipal());
                    }

                    @Override
                    public String rowCss(final Assignment data) {
                        return "";
                    }
                },
                new ProvidesKey<Assignment>() {
                    @Override
                    public Object getKey(final Assignment item) {
                        return item.getId();
                    }
                },
                token
        );

        setShowSize(true);
        setPreviewFactory((data, callback) -> callback.onSuccess(Templates.memberPreview(data,
                Iterables.size(accessControlStore.getAssignments(data.getPrincipal(), include.get())))));

        setTopMenuItems(new MenuDelegate<>(Console.CONSTANTS.common_label_add(),
                item -> presenter.launchAddMemberDialog(selectedRole.get(), include.get()),
                MenuDelegate.Role.Operation));

        setMenuItems(new MenuDelegate<>(Console.CONSTANTS.common_label_delete(), item ->
                Feedback.confirm(Console.CONSTANTS.common_label_areYouSure(), Console.MESSAGES.deleteTitle(item.getPrincipal().getName()),
                        isConfirmed -> {
                            if (isConfirmed) {
                                circuit.dispatch(new RemoveAssignment(item, ROLE_TO_PRINCIPAL));
                            }
                        }),
                MenuDelegate.Role.Operation));

        addSelectionChangeHandler(event -> {
            columnManager.reduceColumnsTo(4);
            if (hasSelectedItem()) {
                columnManager.updateActiveSelection(asWidget());
            }
        });
    }

    @Override
    public Widget asWidget() {
        if (widget == null) {
            widget = super.asWidget();
        }
        return widget;
    }
}
