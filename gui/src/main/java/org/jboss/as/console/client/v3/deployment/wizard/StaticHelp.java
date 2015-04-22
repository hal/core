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
package org.jboss.as.console.client.v3.deployment.wizard;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import org.jboss.as.console.client.Console;

/**
 * @author Harald Pehl
 */
final class StaticHelp {

    private StaticHelp() {}

    static SafeHtml deployment() {
        // TODO I18n or take from DMR
        SafeHtmlBuilder builder = new SafeHtmlBuilder();
        builder.appendHtmlConstant("<table class='help-attribute-descriptions'>");
        addHelpTextRow(builder, "Name:", "Unique identifier of the deployment. Must be unique across all deployments.");
        addHelpTextRow(builder, "Runtime Name:",
                "Name by which the deployment should be known within a server's runtime. This would be equivalent to the file name of a deployment file, and would form the basis for such things as default Java Enterprise Edition application and module names. This would typically be the same as 'name', but in some cases users may wish to have two deployments with the same 'runtime-name' (e.g. two versions of \\\"foo.war\\\") both available in the deployment content repository, in which case the deployments would need to have distinct 'name' values but would have the same 'runtime-name'.");
        addHelpTextRow(builder, "Enable:",
                "Boolean indicating whether the deployment should be enabled after deployment.");
        return builder.toSafeHtml();
    }

    static SafeHtml unmanaged() {
        // TODO I18n or take from DMR
        SafeHtmlBuilder builder = new SafeHtmlBuilder();
        builder.appendHtmlConstant("<table class='help-attribute-descriptions'>");
        addHelpTextRow(builder, "Path:",
                "Path (relative or absolute) to unmanaged content that is part of the deployment.");
        addHelpTextRow(builder, "Relative To:",
                "Name of a system path to which the value of the 'path' is relative. If not set, the 'path' is considered to be absolute.");
        addHelpTextRow(builder, "Is Archive?:",
                "Flag indicating whether unmanaged content is a zip archive (true) or exploded (false).");
        addHelpTextRow(builder, "Name:", "Unique identifier of the deployment. Must be unique across all deployments.");
        addHelpTextRow(builder, "Runtime Name:",
                "Name by which the deployment should be known within a server's runtime. This would be equivalent to the file name of a deployment file, and would form the basis for such things as default Java Enterprise Edition application and module names. This would typically be the same as 'name', but in some cases users may wish to have two deployments with the same 'runtime-name' (e.g. two versions of \\\"foo.war\\\") both available in the deployment content repository, in which case the deployments would need to have distinct 'name' values but would have the same 'runtime-name'.");
        if (Console.getBootstrapContext().isStandalone()) {
            addHelpTextRow(builder, "Enable:",
                    "Boolean indicating whether the deployment should be enabled after deployment.");
        }
        return builder.toSafeHtml();
    }

    private static void addHelpTextRow(SafeHtmlBuilder builder, String name, String desc) {
        builder.appendHtmlConstant("<tr class='help-field-row'>");
        builder.appendHtmlConstant("<td class='help-field-name'>");
        builder.appendEscaped(name);
        builder.appendHtmlConstant("</td>");
        builder.appendHtmlConstant("<td class='help-field-desc'>");
        builder.appendEscaped(desc);
        builder.appendHtmlConstant("</td>");
        builder.appendHtmlConstant("</tr>");
    }
}
