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

import java.util.Iterator;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import org.jboss.as.console.client.administration.role.model.Principal;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.administration.role.model.ScopedRole;
import org.jboss.as.console.client.rbac.Role;
import org.jboss.as.console.client.rbac.StandardRole;

/**
 * @author Harald Pehl
 */
public final class CellFactory {

    final static Templates TEMPLATES = GWT.create(Templates.class);

    public static Cell<Principal> newPrincipalCell() {
        return new PrincipalCell();
    }

    public static Cell<RoleAssignment> newRolesCell() {
        return new RolesCell();
    }

    private static void asSafeHtml(final Principal principal, final SafeHtmlBuilder builder) {
        if (principal.getRealm() != null) {
            builder.append(TEMPLATES.principalAtRealm(principal.getName(), principal.getRealm()));
        } else {
            builder.append(TEMPLATES.principal(principal.getName()));
        }
    }

    private static void asSafeHtml(final SafeHtmlBuilder builder, final Role role, boolean include) {
        if (role instanceof StandardRole) {
            builder.append(include ? TEMPLATES.role(role.getName()) : TEMPLATES.exclude(role.getName()));
        } else if (role instanceof ScopedRole) {
            ScopedRole scopedRole = (ScopedRole) role;
            StringBuilder scopes = new StringBuilder();
            for (Iterator<String> scopeIter = scopedRole.getScope().iterator(); scopeIter.hasNext(); ) {
                String scope = scopeIter.next();
                scopes.append(scope);
                if (scopeIter.hasNext()) {
                    scopes.append(", ");
                }
            }
            builder.append(include ?
                    TEMPLATES.scopedRole(role.getName(), scopedRole.getBaseRole().getName(), scopes.toString()) :
                    TEMPLATES.scopedExclude(role.getName(), scopedRole.getBaseRole().getName(), scopes.toString()));
        }
    }

    interface Templates extends SafeHtmlTemplates {

        @Template("<span>{0}</span>")
        SafeHtml principal(String principal);

        @Template("<span title=\"'{0}' (at) realm '{1}'\">{0}<span class=\"admin-principal-realm\">@{1}</span></span>")
        SafeHtml principalAtRealm(String principal, String realm);

        @Template("<span>{0}</span>")
        SafeHtml role(String role);

        @Template(
                "<span title=\"based on '{1}' scoped to '{2}'\">{0} <span class=\"admin-role-scope\">[{2}]</span></span>")
        SafeHtml scopedRole(String role, String baseRole, String scope);

        @Template("<span class=\"admin-role-exclude\">&dash;{0}</span>")
        SafeHtml exclude(String role);

        @Template(
                "<span class=\"admin-role-exclude\" title=\"based on '{1}' scoped to '{2}'\">&dash;{0} <span class=\"admin-role-scope\">[{2}]</span></span>")
        SafeHtml scopedExclude(String role, String baseRole, String scope);

    }

    static public class PrincipalCell extends AbstractCell<Principal> {

        @Override
        public void render(final Context context, final Principal principal, final SafeHtmlBuilder builder) {
            asSafeHtml(principal, builder);
        }
    }

    static public class RolesCell extends AbstractCell<RoleAssignment> {

        @Override
        public void render(final Context context, final RoleAssignment roleAssignment, final SafeHtmlBuilder builder) {
            boolean excludes = !roleAssignment.getExcludes().isEmpty();
            for (Iterator<Role> iterator = roleAssignment.getRoles().iterator(); iterator.hasNext(); ) {
                Role role = iterator.next();
                asSafeHtml(builder, role, true);
                if (iterator.hasNext() || excludes) {
                    builder.append(SafeHtmlUtils.fromString(", "));
                }
            }
            for (Iterator<Role> iterator = roleAssignment.getExcludes().iterator(); iterator.hasNext(); ) {
                Role exclude = iterator.next();
                asSafeHtml(builder, exclude, false);
                if (iterator.hasNext()) {
                    builder.append(SafeHtmlUtils.fromString(", "));
                }
            }
        }
    }
}