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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.resources.client.ExternalTextResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.accesscontrol.AccessControlFinder;
import org.jboss.as.console.client.administration.accesscontrol.store.AccessControlStore;
import org.jboss.as.console.client.administration.accesscontrol.store.RemoveScopedRole;
import org.jboss.as.console.client.administration.accesscontrol.store.Role;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.preview.PreviewContent;
import org.jboss.as.console.client.preview.PreviewContentFactory;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.gwt.circuit.Dispatcher;

/**
 * Column for standard and scoped roles.
 *
 * @author Harald Pehl
 */
public class RoleColumn extends FinderColumn<Role> {

    static final PreviewContent PREVIEW_CONTENT = PreviewContent.INSTANCE;

    private Widget widget;

    @SuppressWarnings({"unchecked", "Convert2MethodRef"})
    public RoleColumn(final BootstrapContext bootstrapContext,
            final AccessControlStore accessControlStore,
            final Dispatcher circuit,
            final AccessControlFinder presenter,
            final PreviewContentFactory contentFactory,
            final ColumnManager columnManager,
            final Scheduler.ScheduledCommand onSelect,
            final String token) {

        super(FinderId.ACCESS_CONTROL,
                "Role",
                new Display<Role>() {
                    @Override
                    public boolean isFolder(final Role data) {
                        return true;
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final Role data) {
                        return Templates.roleItem(baseCss, data);
                    }

                    @Override
                    public String rowCss(final Role data) {
                        return data.isStandard() ? "standard-role-item" : "scoped-role-item";
                    }
                },
                new ProvidesKey<Role>() {
                    @Override
                    public Object getKey(final Role item) {
                        return item.getId();
                    }
                },
                token);

        setShowSize(true);
        setPreviewFactory((data, callback) -> {
            if (data.isStandard()) {
                ExternalTextResource resource = (ExternalTextResource) PREVIEW_CONTENT
                        .getResource(data.getName().toLowerCase());

                contentFactory.createAndModifyContent(
                        resource,
                        input -> new SafeHtmlBuilder()
                                .appendHtmlConstant("<div class='preview-content'>")
                                .append(input)
                                .append(Templates.roleMembers(data,
                                        accessControlStore.getPrincipals(data, false),
                                        accessControlStore.getPrincipals(data, true)))
                                .appendHtmlConstant("</div>")
                                .toSafeHtml(),
                        callback);
            } else {
                callback.onSuccess(Templates.scopedRolePreview(data,
                        accessControlStore.getPrincipals(data, false),
                        accessControlStore.getPrincipals(data, true)));
            }
        });

        if (!bootstrapContext.isStandalone()) {
            setTopMenuItems(new MenuDelegate<>(Console.CONSTANTS.common_label_add(), item -> presenter.launchAddScopedRoleDialog(),
                    MenuDelegate.Role.Operation));
        }
        setMenuItems(
                new MenuDelegate<>(Console.CONSTANTS.common_label_edit(), item -> presenter.editRole(item), MenuDelegate.Role.Operation),
                new MenuDelegate<>(Console.CONSTANTS.common_label_delete(), item -> {
                    if (item.isScoped()) {
                        Feedback.confirm(Console.CONSTANTS.common_label_areYouSure(),
                                Console.MESSAGES.deleteTitle(item.getName()),
                                isConfirmed -> {
                                    if (isConfirmed) { circuit.dispatch(new RemoveScopedRole(item)); }
                                });
                    } else {
                        Console.warning(((UIConstants) GWT.create(UIConstants.class)).standardRolesCannotBeRemoved());
                    }
                }, MenuDelegate.Role.Operation)
        );

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
