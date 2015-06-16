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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.accesscontrol.AccessControlFinder;
import org.jboss.as.console.client.administration.accesscontrol.store.AccessControlStore;
import org.jboss.as.console.client.administration.accesscontrol.store.Principal;
import org.jboss.as.console.client.administration.accesscontrol.store.RemovePrincipal;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.gwt.circuit.Dispatcher;

/**
 * Column for a principal which is either a user or a group.
 *
 * @author Harald Pehl
 */
public class PrincipalColumn extends FinderColumn<Principal> {

    private Widget widget;

    @SuppressWarnings({"Convert2MethodRef", "unchecked"})
    public PrincipalColumn(final String title,
            final Principal.Type type,
            final AccessControlStore accessControlStore,
            final Dispatcher circuit,
            final AccessControlFinder presenter,
            final ColumnManager columnManager,
            final Scheduler.ScheduledCommand onSelect) {

        super(FinderId.ACCESS_CONTROL,
                title,
                new Display<Principal>() {
                    @Override
                    public boolean isFolder(final Principal data) {
                        return true;
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final Principal data) {
                        return Templates.principalItem(baseCss, data);
                    }

                    @Override
                    public String rowCss(final Principal data) {
                        return "";
                    }
                },
                new ProvidesKey<Principal>() {
                    @Override
                    public Object getKey(final Principal item) {
                        return item.getId();
                    }
                }
        );

        setShowSize(true);
        setPreviewFactory((data, callback) -> callback.onSuccess(
                Templates.principalPreview(data,
                        accessControlStore.getAssignments(data, true),
                        accessControlStore.getAssignments(data, false))
        ));

        setTopMenuItems(new MenuDelegate<>("Add", item -> presenter.launchAddPrincipalDialog(type)));
        setMenuItems(new MenuDelegate<>("Remove", item ->
                Feedback.confirm(Console.CONSTANTS.common_label_areYouSure(),
                        "Remove " + item.getName() +
                                "? This will also remove all assignments for this " +
                                (item.getType() == Principal.Type.USER ? "user." : "group."),
                        isConfirmed -> {
                            if (isConfirmed) { circuit.dispatch(new RemovePrincipal(item)); }
                        })));

        addSelectionChangeHandler(event -> {
            columnManager.reduceColumnsTo(2);
            if (hasSelectedItem()) {
                columnManager.updateActiveSelection(asWidget());
                onSelect.execute();
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
