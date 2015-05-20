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
package org.jboss.as.console.client.v3.deployment;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

import java.util.List;

/**
 * HTML templates used for the deployment finder.
 *
 * @author Harald Pehl
 */
final class Templates {

    static final Items ITEMS = GWT.create(Items.class);
    static final Previews PREVIEWS = GWT.create(Previews.class);


    interface Items extends SafeHtmlTemplates {

        @Template("<div class=\"{0}\" title=\"{2}\">{1}</div>")
        SafeHtml item(String cssClass, String name, String title);

        @Template("<div class=\"{0}\">Host: {1}<br/>Server: {2}</div>")
        SafeHtml referenceServer(String cssClass, String host, String server);
    }


    interface Previews extends SafeHtmlTemplates {

        @Template("<div class='preview-content'><h2>{0}</h2>" +
                "<ul>" +
                "<li>{1}</li>" +
                "</ul>" +
                "</div>")
        SafeHtml assignment(String name, String enabledDisabled);

        @Template("<div class='preview-content'><h2>{0}</h2>" +
                "<ul>" +
                "<li>Runtime Name: {1}</li>" +
                "<li>Host: {2}</li>" +
                "<li>Server: {3}</li>" +
                "</ul>" +
                "{4}" +
                "</div>")
        SafeHtml domainDeployment(String name, String runtimeName, String host, String server, SafeHtml summary);

        @Template("<div class='preview-content'><h2>{0}</h2>" +
                "<ul>" +
                "<li>Runtime Name: {1}</li>" +
                "</ul>" +
                "{2}" +
                "</div>")
        SafeHtml standaloneDeployment(String name, String runtimeName, SafeHtml summary);

        @Template("<div class='preview-content'><h2>{0}</h2>{1}</div>")
        SafeHtml subdeployment(String name, SafeHtml summary);
    }


    static SafeHtml deploymentSummary(Deployment deployment) {
        final SafeHtmlBuilder builder = new SafeHtmlBuilder();
        if (deployment.hasSubdeployments()) {
            builder.appendHtmlConstant("<h3>").appendEscaped("Sub Deployments").appendHtmlConstant("</h3>");
            builder.appendHtmlConstant("<ul>");
            for (Subdeployment subdeployment : deployment.getSubdeployments()) {
                builder.appendHtmlConstant("<li>").appendEscaped(subdeployment.getName()).appendHtmlConstant("</li>");
            }
            builder.appendHtmlConstant("</ul>");
        } else if (!deployment.getSubsystems().isEmpty()) {
            subsystemsSummary(builder, deployment.getSubsystems());
        }
        return builder.toSafeHtml();
    }

    static SafeHtml subdeploymentSummary(Subdeployment subdeployment) {
        final SafeHtmlBuilder builder = new SafeHtmlBuilder();
        subsystemsSummary(builder, subdeployment.getSubsystems());
        return builder.toSafeHtml();
    }

    static void subsystemsSummary(SafeHtmlBuilder builder, List<Subsystem> subsystems) {
        builder.appendHtmlConstant("<h3>").appendEscaped("Subsystems").appendHtmlConstant("</h3>");
        builder.appendHtmlConstant("<ul>");
        for (Subsystem subsystem : subsystems) {
            builder.appendHtmlConstant("<li>").appendEscaped(subsystem.getName()).appendHtmlConstant("</li>");
        }
        builder.appendHtmlConstant("</ul>");
    }
}
