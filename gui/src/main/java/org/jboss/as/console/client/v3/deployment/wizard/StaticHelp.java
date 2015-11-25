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

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.UIConstants;

/**
 * @author Harald Pehl
 */
final class StaticHelp {

    private StaticHelp() {}

    static SafeHtml replace() {
        // TODO I18n or take from DMR
        SafeHtmlBuilder builder = new SafeHtmlBuilder();
        builder.appendHtmlConstant("<table class='help-attribute-descriptions'>");
        addHelpTextRow(builder, "Name:", ((UIConstants) GWT.create(UIConstants.class)).deploymentNameDescription());
        addHelpTextRow(builder, "Runtime Name:",
                ((UIConstants) GWT.create(UIConstants.class)).deploymentRuntimeNameDescription());
        return builder.toSafeHtml();
    }

    static SafeHtml deployment() {
        // TODO I18n or take from DMR
        SafeHtmlBuilder builder = new SafeHtmlBuilder();
        builder.appendHtmlConstant("<table class='help-attribute-descriptions'>");
        addHelpTextRow(builder, "Name:", Console.CONSTANTS.deploymentNameDescription());
        addHelpTextRow(builder, "Runtime Name:",
                Console.CONSTANTS.deploymentRuntimeNameDescription());
        addHelpTextRow(builder, "Enable:",
                ((UIConstants) GWT.create(UIConstants.class)).deploymentEnabledDescription());
        return builder.toSafeHtml();
    }

    static SafeHtml unmanaged() {
        // TODO I18n or take from DMR
        SafeHtmlBuilder builder = new SafeHtmlBuilder();
        builder.appendHtmlConstant("<table class='help-attribute-descriptions'>");
        addHelpTextRow(builder, "Path:",
                ((UIConstants) GWT.create(UIConstants.class)).unmanagedDeploymentPathDescription());
        addHelpTextRow(builder, "Relative To:",
                ((UIConstants) GWT.create(UIConstants.class)).unmanagedDeploymentRelativeToDescription());
        addHelpTextRow(builder, "Is Archive?:",
                ((UIConstants) GWT.create(UIConstants.class)).unmanagedDeploymentArchiveDescription());
        addHelpTextRow(builder, "Name:", Console.CONSTANTS.deploymentNameDescription());
        addHelpTextRow(builder, "Runtime Name:",
                Console.CONSTANTS.deploymentRuntimeNameDescription());
        if (Console.getBootstrapContext().isStandalone()) {
            addHelpTextRow(builder, "Enable:",
                    Console.CONSTANTS.deploymentEnabledDescription());
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
