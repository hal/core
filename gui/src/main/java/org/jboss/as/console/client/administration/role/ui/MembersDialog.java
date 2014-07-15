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
package org.jboss.as.console.client.administration.role.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.RoleAssignmentPresenter;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import java.util.List;

import static org.jboss.as.console.client.administration.role.model.Principal.Type.USER;

/**
 * @author Harald Pehl
 */
public class MembersDialog implements IsWidget {

    static final Templates TEMPLATES = GWT.create(Templates.class);
    private final RoleAssignmentPresenter presenter;
    private final RoleAssignment.Internal internal;

    public MembersDialog(final RoleAssignmentPresenter presenter, final RoleAssignment.Internal internal) {
        this.presenter = presenter;
        this.internal = internal;
    }

    @Override
    public Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("window-content");

        SafeHtmlBuilder builder = new SafeHtmlBuilder();
        if (internal.isEmpty()) {
            builder.append(TEMPLATES.noMembers(internal.getRole().getName()));
        } else {
            builder.append(TEMPLATES.title(internal.getRole().getName()));
            builder.appendHtmlConstant("<ol class=\"outer-list\">");
            boolean includeAll = internal.getRole().isIncludeAll();
            List<RoleAssignment.PrincipalRealmTupel> includes = internal.getIncludes();
            if (includeAll || !includes.isEmpty()) {
                builder.appendHtmlConstant(
                        "<li class=\"header\">Included principals</li><ul class=\"inner-list icons-ul\">");
                if (includeAll) {
                    builder.append(TEMPLATES.includeAll("User"));
                }
                if (!includes.isEmpty()) {
                    for (RoleAssignment.PrincipalRealmTupel include : includes) {
                        if (include.principal.getType() == USER) {
                            builder.append(TEMPLATES.principal("user", "User",
                                    UIHelper.principalAsSafeHtml(include.principal, include.realm)));
                        } else {
                            builder.append(TEMPLATES.principal("group", "Group",
                                    UIHelper.principalAsSafeHtml(include.principal, include.realm)));
                        }
                    }
                }
                builder.appendHtmlConstant("</ul>");
            } else {
                builder.appendHtmlConstant("<li class=\"header\">No principals are included</li>");
            }
            List<RoleAssignment.PrincipalRealmTupel> excludes = internal.getExcludes();
            if (!excludes.isEmpty()) {
                builder.appendHtmlConstant(
                        "<li class=\"header\">Excluded principals</li><ul class=\"inner-list icons-ul\">");
                for (RoleAssignment.PrincipalRealmTupel exclude : excludes) {
                    if (exclude.principal.getType() == USER) {
                        builder.append(TEMPLATES.principal("user", "User",
                                UIHelper.principalAsSafeHtml(exclude.principal, exclude.realm)));
                    } else {
                        builder.append(TEMPLATES.principal("group", "Group",
                                UIHelper.principalAsSafeHtml(exclude.principal, exclude.realm)));
                    }
                }
                builder.appendHtmlConstant("</ul>");
            } else {
                builder.appendHtmlConstant("<li class=\"header\">No principals are excluded</li>");
            }
            builder.appendHtmlConstant("</ol>");
        }
        HTML html = new HTML(builder.toSafeHtml());
        html.addStyleName("members-dialog");
        layout.add(html);

        DialogueOptions options = new DialogueOptions(
                Console.CONSTANTS.common_label_done(),
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        presenter.closeWindow();
                    }
                },
                Console.CONSTANTS.common_label_cancel(),
                new ClickHandler() {
                    @Override
                    public void onClick(final ClickEvent event) {
                        presenter.closeWindow();
                    }
                }
        );
        options.showCancel(false);
        return new WindowContentBuilder(new ScrollPanel(layout), options).build();
    }

    interface Templates extends SafeHtmlTemplates {

        @Template("<p>No members found for role {0}</p>")
        SafeHtml noMembers(String role);

        @Template(
                "<p>The role {0} consists of these principals. Please note that any exclude definitions takes priority over any include definitions.</p>")
        SafeHtml title(String role);

        @Template("<li title=\"{0}\"><i class=\"icon-li icon-user\"></i>Any authenticated user</li>")
        SafeHtml includeAll(String label);

        @Template("<li title=\"{1}\"><i class=\"icon-li icon-{0}\"></i>{2}</li>")
        SafeHtml principal(String type, String label, SafeHtml principal);
    }
}
