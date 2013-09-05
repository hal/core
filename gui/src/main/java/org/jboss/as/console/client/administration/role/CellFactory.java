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
import java.util.List;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import org.jboss.as.console.client.administration.role.model.Principal;
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

    public static Cell<List<Principal>> newPrincipalsCell() {
        return new PrincipalsCell();
    }

    public static Cell<Role> newRoleCell() {
        return new RoleCell();
    }

    public static Cell<List<Role>> newRolesCell() {
        return new RolesCell();
    }

    private static void asSafeHtml(final Principal principal, final SafeHtmlBuilder builder) {
        if (principal.getRealm() != null) {
            builder.append(TEMPLATES.principalAtRealm(principal.getName(), principal.getRealm()));
        } else {
            builder.append(TEMPLATES.principal(principal.getName()));
        }
    }

    private static void asSafeHtml(final SafeHtmlBuilder builder, final Role role) {
        if (role instanceof StandardRole) {
            builder.append(TEMPLATES.role(role.getName()));
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
            builder.append(
                    TEMPLATES.scopedRole(role.getName(), scopedRole.getBaseRole().name(), scopes.toString()));
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

    }

    static public class PrincipalCell extends AbstractCell<Principal> {

        @Override
        public void render(final Context context, final Principal principal, final SafeHtmlBuilder builder) {
            asSafeHtml(principal, builder);
        }
    }

    static public class PrincipalsCell extends AbstractCell<List<Principal>> {

        @Override
        public void render(final Context context, final List<Principal> principals, final SafeHtmlBuilder builder) {
            for (Iterator<Principal> iterator = principals.iterator(); iterator.hasNext(); ) {
                Principal principal = iterator.next();
                asSafeHtml(principal, builder);
                if (iterator.hasNext()) {
                    builder.append(SafeHtmlUtils.fromString(", "));
                }
            }
        }
    }

    static public class RoleCell extends AbstractCell<Role> {

        @Override
        public void render(final Context context, final Role role, final SafeHtmlBuilder builder) {
            asSafeHtml(builder, role);
        }
    }

    static public class RolesCell extends AbstractCell<List<Role>> {

        @Override
        public void render(final Context context, final List<Role> roles, final SafeHtmlBuilder builder) {
            for (Iterator<Role> roleIter = roles.iterator(); roleIter.hasNext(); ) {
                Role role = roleIter.next();
                asSafeHtml(builder, role);
                if (roleIter.hasNext()) {
                    builder.append(SafeHtmlUtils.fromString(", "));
                }
            }
        }
    }
}