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
import com.google.gwt.resources.client.ExternalTextResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.accesscontrol.AccessControlFinder;
import org.jboss.as.console.client.administration.accesscontrol.store.Assignment;
import org.jboss.as.console.client.administration.accesscontrol.store.Principal;
import org.jboss.as.console.client.administration.accesscontrol.store.RemoveAssignment;
import org.jboss.as.console.client.preview.PreviewContent;
import org.jboss.as.console.client.preview.PreviewContentFactory;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.gwt.circuit.Dispatcher;

import static org.jboss.as.console.client.administration.accesscontrol.store.ModifiesAssignment.Relation.PRINCIPAL_TO_ROLE;

/**
 * Column for the assignment of a selected principal or role.
 *
 * @author Harald Pehl
 */
public class AssignmentColumn extends FinderColumn<Assignment> {

    static final PreviewContent PREVIEW_CONTENT = PreviewContent.INSTANCE;

    private Widget widget;

    @SuppressWarnings({"unchecked", "Convert2MethodRef"})
    public AssignmentColumn(final Dispatcher circuit,
            final AccessControlFinder presenter,
            final PreviewContentFactory contentFactory,
            final ColumnManager columnManager,
            final Supplier<Principal> selectedPrincipal,
            final Supplier<Boolean> include) {

        super(FinderId.ACCESS_CONTROL,
                "Role",
                new Display<Assignment>() {
                    @Override
                    public boolean isFolder(final Assignment data) {
                        return false;
                    }

                    @Override
                    public SafeHtml render(final String baseCss, final Assignment data) {
                        return Templates.assignmentItem(baseCss, data);
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
                }
        );

        setShowSize(true);
        setPreviewFactory((data, callback) -> {
            if (data.getRole().isStandard()) {
                ExternalTextResource resource = (ExternalTextResource) PREVIEW_CONTENT
                        .getResource(data.getRole().getName().toLowerCase());

                contentFactory.createAndModifyContent(
                        resource,
                        input -> new SafeHtmlBuilder()
                                .appendHtmlConstant("<div class='preview-content'>")
                                .append(input)
                                .appendHtmlConstant("</div>")
                                .toSafeHtml(),
                        callback);
            } else {
                callback.onSuccess(Templates.scopedRolePreview(data.getRole()));
            }
        });

        setTopMenuItems(new MenuDelegate<>("Add",
                item -> presenter.launchAddAssignmentDialog(selectedPrincipal.get(), include.get())));
        setMenuItems(new MenuDelegate<>("Remove", item ->
                Feedback.confirm(Console.CONSTANTS.common_label_areYouSure(), "Remove " + item.getRole().getName(),
                        isConfirmed -> {
                            if (isConfirmed) {
                                circuit.dispatch(new RemoveAssignment(item, PRINCIPAL_TO_ROLE));
                            }
                        })));

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
