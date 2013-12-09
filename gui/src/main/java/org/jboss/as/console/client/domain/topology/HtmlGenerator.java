/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.jboss.as.console.client.domain.topology;

import static org.jboss.as.console.client.domain.model.ServerFlag.RELOAD_REQUIRED;
import static org.jboss.as.console.client.domain.model.ServerFlag.RESTART_REQUIRED;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import org.jboss.as.console.client.domain.model.ServerInstance;
import org.jboss.as.console.client.widgets.icons.ConsoleIcons;
import org.jboss.ballroom.client.rbac.AuthorisationDecision;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.rbac.SecurityService;
import org.jboss.ballroom.client.spi.Framework;
import org.jboss.ballroom.client.widgets.icons.Icons;

/**
 * Contains most of the html generator code used in {@link TopologyView}. The generated html contains several <a
 * href="http://dev.w3.org/html5/spec/global-attributes.html#embedding-custom-non-visible-data-with-the-data-*-attributes">HTML5
 * data attributes</a> to mark special tags:
 * <ul>
 * <li>data-group: Marks a &lt;tr&gt; element as start of a new server group</li>
 * <li>data-group-name: The name of a server group. Used for lifecycle links.</li>
 * <li>data-host-name: The name of the host. Used for lifecycle links.</li>
 * <li>data-server-name: The name of a server instance. Used for lifecycle links.</li>
 * </ul>
 *
 * @author Harald Pehl
 */
final class HtmlGenerator {

    static final String DATA_GROUP_NAME = "data-group-name";
    static final String DATA_HOST_NAME = "data-host-name";
    static final String DATA_SERVER_NAME = "data-server-name";
    static final String START_SERVER_ID = "start_server_";
    static final String STOP_SERVER_ID = "stop_server_";
    static final String KILL_SERVER_ID = "kill_server_";
    static final String RELOAD_SERVER_ID = "reload_server_";
    static final String START_GROUP_ID = "start_group_";
    static final String STOP_GROUP_ID = "stop_group_";
    static final String RESTART_GROUP_ID = "restart_group_";

    static Framework FRAMEWORK = GWT.create(Framework.class);
    static SecurityService SECURITY_SERVICE = FRAMEWORK.getSecurityService();

    final SafeHtmlBuilder html;
    final List<String> lifecycleIds;

    HtmlGenerator() {
        this.html = new SafeHtmlBuilder();
        this.lifecycleIds = new ArrayList<String>();
    }


    // ------------------------------------------------------ custom methods

    HtmlGenerator appendHost(final HostInfo host) {
        appendHtmlConstant("<th class='cellTableHeader'>");
        if (host.isController()) {
            appendIcon(ConsoleIcons.INSTANCE.star(), "Domain Controller");
        }
        startLine().appendEscaped(host.getName()).endLine();
        startLine().appendHtmlConstant("Domain: ").appendHtmlConstant(host.isController() ? "Controller" : "Member")
                .endLine();
        html.appendHtmlConstant("</th>");
        return this;
    }

    HtmlGenerator appendServerGroup(final ServerGroup group) {
        if (group.maxServersPerHost > 1) {
            appendHtmlConstant("<td class='groupColumn domainOverviewCell cellTableCell endOfServerGroup " +
                    group.cssClassname + "' rowspan='" + group.maxServersPerHost + "'>");
        } else {
            appendHtmlConstant("<td class='groupColumn domainOverviewCell cellTableCell endOfServerGroup " +
                    group.cssClassname + "'>");
        }
        startLine().appendEscaped(group.name).endLine();
        if (group.profile != null) {
            startLine().appendEscaped("Profile: " + group.profile).endLine();
        }

        SecurityContext securityContext = findContext("/server-group=" + group.name);
        startLinks(securityContext, true);
        String startId = START_GROUP_ID + group.name;
        String stopId = STOP_GROUP_ID + group.name;
        String restartId = RESTART_GROUP_ID + group.name;
        appendLifecycleLink(startId, group.name, null, null, "Start Group")
                .appendHtmlConstant("<br/>")
                .appendLifecycleLink(stopId, group.name, null, null, "Stop Group")
                .appendHtmlConstant("<br/>")
                .appendLifecycleLink(restartId, group.name, null, null, "Restart Group");
        endLinks();

        appendHtmlConstant("</td>");
        return this;
    }

    HtmlGenerator appendServer(final ServerGroup group, final String host, final ServerInstance server) {
        ImageResource icon;
        String tooltip = "";
        if (server.isRunning()) {
            if (server.getFlag() != null) {
                icon = Icons.INSTANCE.status_warn();
                if (server.getFlag() == RELOAD_REQUIRED) {
                    tooltip = "Server has to be reloaded";
                } else if (server.getFlag() == RESTART_REQUIRED) {
                    tooltip = "Server has to be restarted";
                }
            } else {
                tooltip = "Server is up and running";
                icon = Icons.INSTANCE.status_good();
            }
        } else {
            tooltip = "Server is stopped";
            icon = Icons.INSTANCE.status_bad();
        }
        appendHtmlConstant("<td class='cellTableCell domainOverviewCell " + group.cssClassname + "_light" +
                "' title='" + tooltip + "'>");

        startLine().appendIcon(icon, "Server running?").appendEscaped(server.getName()).endLine();
        if (server.getSocketBindings().size() > 0) {
            Set<String> sockets = server.getSocketBindings().keySet();
            String first = sockets.iterator().next();
            startLine().appendHtmlConstant("Socket Binding: ").appendEscaped(first).endLine();
            startLine().appendHtmlConstant("Ports: +").appendEscaped(server.getSocketBindings().get(first)).endLine();
        }

        SecurityContext securityContext = findContext("/host=" + host + "/server-config=" + server.getName());
        startLinks(securityContext, false);
        String uniqueServerName = host + "_" + server.getName();
        if (server.isRunning()) {
            appendLifecycleLink(STOP_SERVER_ID + uniqueServerName, null, host, server.getName(), "Stop Server");
            appendLifecycleLink(KILL_SERVER_ID + uniqueServerName, null, host, server.getName(), "Force Shutdown");
            if (server.getFlag() == RELOAD_REQUIRED) {
                appendHtmlConstant("<br/>");
                appendLifecycleLink(RELOAD_SERVER_ID + uniqueServerName, null, host, server.getName(),
                        "Reload Server");
            }
        } else {
            appendLifecycleLink(START_SERVER_ID + uniqueServerName, null, host, server.getName(), "Start Server");
        }
        endLinks();

        appendHtmlConstant("</td>");
        return this;
    }

    SecurityContext findContext(final String resourceAddress) {
        SecurityContext securityContext = SECURITY_SERVICE.getSecurityContext();
        if (securityContext.hasChildContext(resourceAddress)) {
            securityContext = securityContext.getChildContext(resourceAddress);
        }
        return securityContext;
    }

    HtmlGenerator appendIcon(final ImageResource img, String alt) {
        appendHtmlConstant("<img src='" + new Image(img)
                .getUrl() + "' width='16' " + "height='16' class='statusIcon' alt='" + alt + "' title='" + alt + "'/>");
        return this;
    }

    HtmlGenerator appendColumn(final int width) {
        appendHtmlConstant("<col width='" + width + "%'/>");
        return this;
    }

    HtmlGenerator appendLifecycleLink(String id, String group, String host, String server, String text) {
        lifecycleIds.add(id);
        appendHtmlConstant("<a id='" + id + "' class='lifecycleLink'" +
                (group != null ? " " + DATA_GROUP_NAME + "='" + group + "'" : "") +
                (host != null ? " " + DATA_HOST_NAME + "='" + host + "'" : "") +
                (server != null ? " " + DATA_SERVER_NAME + "='" + server + "'" : "") +
                ">").appendEscaped(text).appendHtmlConstant("</a>&nbsp;&nbsp;");
        return this;
    }

    HtmlGenerator startTable() {
        appendHtmlConstant("<table cellspacing='0' class='default-cell-table topology'>");
        return this;
    }

    HtmlGenerator endTable() {
        appendHtmlConstant("</table>");
        return this;
    }

    HtmlGenerator startLine() {
        appendHtmlConstant("<div>");
        return this;
    }

    HtmlGenerator endLine() {
        appendHtmlConstant("</div>");
        return this;
    }

    HtmlGenerator startLinks(final SecurityContext securityContext, boolean groupLinks) {
        AuthorisationDecision decision;
        if (groupLinks) {
            decision = securityContext.getOperationPriviledge("/server-group=*", "start-servers");
        } else {
            decision = securityContext.getOperationPriviledge("/{selected.host}/server-config=*", "start");
        }

        if (decision.isGranted()) {
            appendHtmlConstant("<div>");
        } else {
            appendHtmlConstant("<div class='rbac-suppressed'>");
        }

        appendHtmlConstant("<div class='lifecycleLinks'>");
        appendHtmlConstant("<span style='color:#404040'><i class='icon-caret-down'></i></span><br/>");
        return this;
    }

    HtmlGenerator endLinks() {
        // one for the lifecycle links, one for outer div wrapping the icon-caret-down and the links
        appendHtmlConstant("</div>").appendHtmlConstant("</div>");
        return this;
    }

    HtmlGenerator emptyCell() {
        appendHtmlConstant("<td class='cellTableCell domainOverviewCell'>&nbsp;</td>");
        return this;
    }

    List<String> getLifecycleIds() {
        return lifecycleIds;
    }

    HTMLPanel createPanel() {
        return new HTMLPanel(this.toSafeHtml().asString());
    }


    // ------------------------------------------------------ delegate methods

    public HtmlGenerator appendHtmlConstant(final String text) {
        html.appendHtmlConstant(text);
        return this;
    }

    public HtmlGenerator appendEscaped(final String text) {
        html.appendEscaped(text);
        return this;
    }

    public SafeHtml toSafeHtml() {
        return html.toSafeHtml();
    }
}
