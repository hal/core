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
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.widgets.nav.v3.PreviewState;

/**
 * HTML templates used for the deployment finder.
 *
 * @author Harald Pehl
 */
final class Templates {

    static final Items ITEMS = GWT.create(Items.class);
    static final Tooltips TOOLTIPS = GWT.create(Tooltips.class);
    static final Previews PREVIEWS = GWT.create(Previews.class);


    interface Items extends SafeHtmlTemplates {

        @Template("<div class=\"{0}\" title=\"{2}\">{1}</div>")
        SafeHtml item(String cssClass, String name, String title);
    }


    interface Tooltips extends SafeHtmlTemplates {

        //        @Template("<i class=\"{1}\" style=\"color:{2}\"></i>&nbsp;{0}")
        @Template("{0}")
        SafeHtml assignment(String text/*, String icon, String color*/);
    }


    interface Previews extends SafeHtmlTemplates {

        @Template("<div class='preview-content'><h2>Content</h2>" +
                "<p>The content '{0}' is assigned to the following server groups:</p>{1}</div>")
        SafeHtml content(String name, SafeHtml details);

        @Template("<div class='preview-content'><h2>Content</h2>" +
                "<p>The content '{0}' is not assigned to a server group.</div>")
        SafeHtml unassignedContent(String name);

        @Template("<div class='preview-content'><h2>Server Group</h2>{0}</div>")
        SafeHtml serverGroup(SafeHtml details);

        @Template("<div class='preview-content'><h2>{0}</h2>" +
                "<p>There's no server running in server group '{1}'.</p>" +
                "{2}" +
                "</div>")
        SafeHtml noReferenceServer(String name, String serverGroup, SafeHtml status);

        @Template("<div class='preview-content'><h2>{0}</h2>" +
                "{1}{2}" +
                "</div>")
        SafeHtml standaloneDeployment(String name, SafeHtml ed, SafeHtml details);

        @Template("<div class='preview-content'><h2>{0}</h2>" +
                "{3}{4}" +
                "<h3>Reference Server</h3><p>The information was taken from host '{1}', server '{2}'.</p>" +
                "</div>")
        SafeHtml domainDeployment(String name, String host, String server,
                SafeHtml ed, SafeHtml details);

        @Template("<div class='preview-content'><h2>{0}</h2>" +
                "<p>This is a nested deployment which is part of the selected deployment." +
                "</div>")
        SafeHtml subdeployment(String name);
    }


    // ------------------------------------------------------ tooltips

    static SafeHtml deploymentTooltip(final Deployment deployment) {
        if (deployment.isEnabled()) {
            return TOOLTIPS.assignment("Deployment is enabled"/*, "icon-ok", "#3F9C35"*/);
        } else {
            return TOOLTIPS.assignment("Deployment is disabled"/*, "icon-warning-sign", "#EC7A08"*/);
        }
    }

    static SafeHtml assignmentTooltip(final Assignment assignment) {
        if (!assignment.hasDeployment()) {
            return TOOLTIPS.assignment("No reference server available"/*, "icon-ban-circle", "#cc0000"*/);
        } else {
            if (assignment.isEnabled()) {
                return TOOLTIPS.assignment("Deployment is enabled"/*, "icon-ok", "#3F9C35"*/);
            } else {
                return TOOLTIPS.assignment("Deployment is disabled"/*, "icon-warning-sign", "#EC7A08"*/);
            }
        }
    }


    // ------------------------------------------------------ previews

    static SafeHtml contentPreview(final Content content) {
        if (content.getAssignments().isEmpty()) {
            return PREVIEWS.unassignedContent(content.getName());
        } else {
            SafeHtmlBuilder details = new SafeHtmlBuilder();
            details.appendHtmlConstant("<ul>");
            for (Assignment assignment : content.getAssignments()) {
                details.appendHtmlConstant("<li>")
                        .appendEscaped(assignment.getServerGroup())
                        .appendHtmlConstant("</li>");
            }
            details.appendHtmlConstant("</ul>");
            return PREVIEWS.content(content.getName(), details.toSafeHtml());
        }
    }

    static SafeHtml serverGroupPreview(final ServerGroupRecord serverGroup, int deployments) {
        SafeHtmlBuilder builder = new SafeHtmlBuilder();
        builder.appendHtmlConstant("<p>");
        if (deployments == -1) {
            builder.appendEscaped("Deployments for server group ")
                    .appendEscaped(serverGroup.getName())
                    .appendEscaped(" cannot be read.");
        } else if (deployments == 0) {
            builder.appendEscaped("Server group ")
                    .appendEscaped(serverGroup.getName())
                    .appendEscaped(" does not contain deployments.");
        } else {
            builder.appendEscaped("Server group ")
                    .appendEscaped(serverGroup.getName())
                    .appendEscaped(" contains ")
                    .append(deployments)
                    .appendEscaped(" deployment(s).");
        }
        builder.appendHtmlConstant("</p>");
        return PREVIEWS.serverGroup(builder.toSafeHtml());
    }

    static SafeHtml assignmentPreview(final Assignment assignment) {
        Deployment deployment = assignment.getDeployment();
        if (deployment == null) {
            SafeHtmlBuilder status = new SafeHtmlBuilder();
            PreviewState.error(status, "No running server");
            return PREVIEWS.noReferenceServer(assignment.getName(), assignment.getServerGroup(), status.toSafeHtml());
        } else {
            return deploymentPreview(deployment);
        }
    }

    static SafeHtml deploymentPreview(final Deployment deployment) {
        SafeHtmlBuilder enabledDisabledBuilder = new SafeHtmlBuilder();
        if (deployment.isEnabled()) {
            PreviewState.good(enabledDisabledBuilder, "Deployment is enabled");
        } else {
            PreviewState.paused(enabledDisabledBuilder, "Deployment is disabled");
        }

        SafeHtmlBuilder details = new SafeHtmlBuilder();
        details.appendHtmlConstant("<h3>").appendEscaped("Details").appendHtmlConstant("</h3>")
                .appendHtmlConstant("<ul>");
        if (deployment.getEnabledTime() != null) {
            details.appendHtmlConstant("<li class='deployment-timestamp'>").appendEscaped("Last enabled at ")
                    .appendEscaped(deployment.getEnabledTime());
        } else {
            details.appendHtmlConstant("<li>").appendEscaped("The deployment was never enabled");
        }
        if (deployment.getDisabledTime() != null) {
            details.appendHtmlConstant("<li class='deployment-timestamp'>").appendEscaped("Last disabled at ")
                    .appendEscaped(deployment.getDisabledTime());
        } else {
            details.appendHtmlConstant("<li>").appendEscaped("The deployment was never disabled");
        }
        details.appendHtmlConstant("<li>").appendEscaped("Runtime name: ")
                .appendEscaped(deployment.getRuntimeName());
        details.appendHtmlConstant("</ul>");

        if (deployment.hasSubdeployments()) {
            details.appendHtmlConstant("<h3>").appendEscaped("Nested Deployments").appendHtmlConstant("</h3>")
                    .appendHtmlConstant("<p>")
                    .appendEscaped("The deployment contains ")
                    .appendEscaped(String.valueOf(deployment.getSubdeployments().size()))
                    .appendEscaped(" nested deployments")
                    .appendHtmlConstant("</p>");

        }

        return deployment.getReferenceServer().isStandalone() ?
                PREVIEWS.standaloneDeployment(deployment.getName(), enabledDisabledBuilder.toSafeHtml(),
                        details.toSafeHtml()) :
                PREVIEWS.domainDeployment(deployment.getName(), deployment.getReferenceServer().getHost(),
                        deployment.getReferenceServer().getServer(), enabledDisabledBuilder.toSafeHtml(),
                        details.toSafeHtml());
    }

    static SafeHtml subdeploymentPreview(final Subdeployment subdeployment) {
        return PREVIEWS.subdeployment(subdeployment.getName());
    }
}
