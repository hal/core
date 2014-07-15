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
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.help.StaticHelpPanel;

/**
 * @author Harald Pehl
 */
public class ScopedRoleHelpPanel extends StaticHelpPanel {

    final static Templates TEMPLATES = GWT.create(Templates.class);

    public ScopedRoleHelpPanel() {
        super(new SafeHtmlBuilder().append(TEMPLATES.help(
                Console.CONSTANTS.administration_scoped_role_base_role_desc(),
                Console.CONSTANTS.administration_scoped_role_scope_desc(),
                Console.CONSTANTS.administration_role_include_all_desc())).toSafeHtml());
    }

    interface Templates extends SafeHtmlTemplates {

        @Template("<table style=\"vertical-align:top\" cellpadding=\"3\">" +
                "<tr><td>Base Role</td><td>{0}</td></tr>" +
                "<tr><td>Scope</td><td>{1}</td></tr>" +
                "<tr><td>Include All</td><td>{2}</td></tr>" +
                "</table>")
        SafeHtml help(String baseRoleDesc, String scopeDesc, String includeAllDesc);
    }
}