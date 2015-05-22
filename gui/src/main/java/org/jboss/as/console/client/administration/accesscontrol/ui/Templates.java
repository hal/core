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

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import org.jboss.as.console.client.administration.accesscontrol.store.Assignment;
import org.jboss.as.console.client.administration.accesscontrol.store.Principal;
import org.jboss.as.console.client.administration.accesscontrol.store.Role;
import org.jboss.as.console.client.administration.accesscontrol.store.Roles;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.Math.max;

/**
 * @author Harald Pehl
 */
final class Templates {

    static final Items ITEMS = GWT.create(Items.class);
    static final Previews PREVIEWS = GWT.create(Previews.class);


    interface Items extends SafeHtmlTemplates {

        @Template("<div class=\"{0}\" title=\"{2}\">{1}</div>")
        SafeHtml item(String css, String name, String title);

        @Template("<div class=\"{0}\" title=\"{2}\">{1}<span class=\"member-realm\">@{3}</span></div>")
        SafeHtml principalWithRealm(String css, String name, String title, String realm);

        @Template("<div class=\"{0}\" title=\"{2}\">{1}<div style=\"font-size:8px\">{3}</div></div>")
        SafeHtml scopedRole(String css, String name, String title, String baseAndScope);

        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml aggregationItem(String css, String name);

        @Template("<div class=\"{0}\" title=\"{2}\"><i class=\"{3}\"></i> {1}</div>")
        SafeHtml member(String css, String name, String title, String principalTypeCss);

        @Template("<div class=\"{0}\" title=\"{2}\"><i class=\"{3}\"></i> {1}<span class=\"member-realm\">@{4}</span></div>")
        SafeHtml memberWithRealm(String css, String name, String title, String principalTypeCss, String realm);

        @Template("<div class=\"{0}\" title=\"{2}\">{1}<div style=\"font-size:8px\">{3}</div></div>")
        SafeHtml assignmentWithScopedRole(String css, String name, String title, String baseAndScope);
    }


    interface Previews extends SafeHtmlTemplates {

        @Template("<div class='preview-content'><h2>{0}</h2>{1}</div>")
        SafeHtml user(String name, SafeHtml details);

        @Template("<div class='preview-content'><h2>{0}</h2>{1}</div>")
        SafeHtml group(String name, SafeHtml details);

        @Template("<div class='preview-content'><h2>{0}</h2><p>{1}</p>{2}</div>")
        SafeHtml scopedRole(String name, String baseAndScope, SafeHtml members);

        @Template("<div class='preview-content'><h2>{0}</h2><p>This is a principal assignment...</p></div>")
        SafeHtml assignment(String role);

        @Template("<div class='preview-content'><h2>{0}</h2>{1}</div>")
        SafeHtml member(String principal, SafeHtml details);
    }


    // ------------------------------------------------------ principal

    static SafeHtml principalItem(final String css, final Principal principal) {
        return principal.getRealm() == null ?
                ITEMS.item(css, principal.getName(), principal.getName()) :
                ITEMS.principalWithRealm(css, principal.getName(),
                        principal.getName() + " (at) " + principal.getRealm(), principal.getRealm());
    }

    static SafeHtml principalPreview(final Principal principal, Iterable<Assignment> includes,
            Iterable<Assignment> excludes) {
        SafeHtmlBuilder details = new SafeHtmlBuilder();
        details.appendHtmlConstant("<p>");
        if (!Iterables.isEmpty(excludes)) {
            List<Role> excludedRoles = Roles.orderedByName().immutableSortedCopy(distinctRoles(excludes));
            details.appendEscaped("Excluded from ");
            details.appendEscaped(Joiner.on(", ").join(Lists.transform(excludedRoles, Role::getName)));
            details.appendEscaped(".");
            details.appendHtmlConstant("<br/>");
        }
        if (!Iterables.isEmpty(includes)) {
            List<Role> assignedRoles = Roles.orderedByName().immutableSortedCopy(distinctRoles(includes));
            details.appendEscaped("Assigned to ");
            details.appendEscaped(Joiner.on(", ").join(Lists.transform(assignedRoles, Role::getName)));
            details.appendEscaped(".");
        }
        if (Iterables.isEmpty(excludes) && Iterables.isEmpty(includes)) {
            details.appendEscaped("No roles are assigned to this ");
            details.appendEscaped(principal.getType() == Principal.Type.USER ? "user" : "group");
            details.append('.');
        }
        details.appendHtmlConstant("</p>");
        return principal.getType() == Principal.Type.USER ?
                PREVIEWS.user(principal.getName(), details.toSafeHtml()) :
                PREVIEWS.group(principal.getName(), details.toSafeHtml());
    }


    // ------------------------------------------------------ role

    static SafeHtml roleItem(final String css, final Role role) {
        if (role.isStandard()) {
            return ITEMS.item(css, role.getName(), role.getName());
        } else {
            return ITEMS.scopedRole(css, role.getName(), baseAndScope(role), shortBaseAndScope(role));
        }
    }

    static SafeHtml scopedRolePreview(final Role role) {
        return PREVIEWS.scopedRole(role.getName(), baseAndScope(role), SafeHtmlUtils.EMPTY_SAFE_HTML);
    }

    static SafeHtml scopedRolePreview(final Role role, final Iterable<Principal> excludes,
            final Iterable<Principal> includes) {
        return PREVIEWS.scopedRole(role.getName(), baseAndScope(role), roleMembers(role, excludes, includes));
    }

    static SafeHtml roleMembers(final Role role, final Iterable<Principal> excludes,
            final Iterable<Principal> includes) {
        SafeHtmlBuilder members = new SafeHtmlBuilder();
        if (role.isIncludeAll()) {
            members.appendHtmlConstant("<p>")
                    .appendEscaped("All authenticated users are automatically assigned to this role.")
                    .appendHtmlConstant("</p>");

        } else if (Iterables.isEmpty(excludes) && Iterables.isEmpty(includes)) {
            members.appendHtmlConstant("<p>")
                    .appendEscaped("No users or groups are assigned to this role.")
                    .appendHtmlConstant("</p>");

        } else {
            if (!Iterables.isEmpty(excludes)) {
                String names = Joiner.on(", ").join(Iterables.transform(excludes, Principal::getNameAndRealm));
                members.appendHtmlConstant("<p><b>")
                        .appendEscaped("Excludes")
                        .appendHtmlConstant("</b><br/>")
                        .appendEscaped(names)
                        .appendHtmlConstant("</p>");
            }

            if (!Iterables.isEmpty(includes)) {
                String names = Joiner.on(", ").join(Iterables.transform(includes, Principal::getNameAndRealm));
                members.appendHtmlConstant("<p><b>")
                        .appendEscaped("Includes")
                        .appendHtmlConstant("</b><br/>")
                        .appendEscaped(names)
                        .appendHtmlConstant("</p>");
            }
        }
        return members.toSafeHtml();
    }


    // ------------------------------------------------------ aggregation items

    static SafeHtml aggregationItem(final String css, final AggregationItem item) {
        return ITEMS.aggregationItem(css, item.getTitle());
    }


    // ------------------------------------------------------ assignment

    static SafeHtml assignmentItem(final String css, final Assignment assignment) {
        Role role = assignment.getRole();
        String title = (assignment.isInclude() ? "" : "Exclude ") + role.getName();
        if (role.isStandard()) {
            return ITEMS.item(css, role.getName(), title);
        } else {
            return ITEMS.assignmentWithScopedRole(css, role.getName(), title, shortBaseAndScope(role));
        }
    }


    // ------------------------------------------------------ member

    static SafeHtml memberItem(final String css, final Assignment assignment) {
        Principal principal = assignment.getPrincipal();
        String principalTypeCss = principal.getType() == Principal.Type.USER ? "icon-user" : "icon-group";
        StringBuilder title = new StringBuilder();
        if (!assignment.isInclude()) {
            title.append("Exclude ");
        }
        title.append(principal.getName());
        if (principal.getRealm() != null) {
            title.append(" (at) ").append(principal.getRealm());
        }

        if (principal.getRealm() == null) {
            return ITEMS.member(css, principal.getName(), title.toString(), principalTypeCss);
        } else {
            return ITEMS.memberWithRealm(css, principal.getName(), title.toString(), principalTypeCss,
                    principal.getRealm());
        }
    }

    static SafeHtml memberPreview(final Assignment assignment, int memberAssignments) {
        int otherAssignments = max(0, memberAssignments - 1);
        Principal member = assignment.getPrincipal();
        SafeHtmlBuilder builder = new SafeHtmlBuilder();
        builder.appendHtmlConstant("<p>");
        if (!assignment.isInclude()) {
            builder.appendEscaped("Excluded from role ").appendEscaped(assignment.getRole().getName())
                    .appendEscaped(". ");
        }
        if (otherAssignments == 0) {
            builder.appendEscaped("Not used in other assignments. ");
        } else if (otherAssignments == 1) {
            builder.appendEscaped("Used in one other assignment. ");
        } else {
            builder.appendEscaped("Used in ").append(otherAssignments).appendEscaped(" other assignments. ");
        }
        if (member.getRealm() != null) {
            builder.appendEscaped("Bound to realm '").appendEscaped(member.getRealm()).appendEscaped("'.");
        }
        builder.appendHtmlConstant("</p>");
        return PREVIEWS.member(member.getName(), builder.toSafeHtml());
    }


    // ------------------------------------------------------ helper methods

    private static Set<Role> distinctRoles(final Iterable<Assignment> assignments) {
        Set<Role> roles = new HashSet<>();
        for (Assignment assignment : assignments) {
            roles.add(assignment.getRole());
        }
        return roles;
    }

    private static String baseAndScope(Role role) {
        StringBuilder builder = new StringBuilder();
        builder.append("Based on role '").append(role.getBaseRole().getId());
        if (role.getType() == Role.Type.HOST) {
            builder.append("' scoped to host(s) '");
        } else if (role.getType() == Role.Type.SERVER_GROUP) {
            builder.append("' scoped to server group(s) '");
        }
        Joiner.on(", ").appendTo(builder, role.getScope());
        builder.append("'.");
        return builder.toString();
    }

    private static String shortBaseAndScope(final Role role) {
        return Joiner.on(" / ").join(role.getBaseRole().getId(), Joiner.on(", ").join(role.getScope()));
    }
}