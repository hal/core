/*
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.console.client.shared.subsys.elytron.ui;

import java.util.List;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.elytron.store.ElytronStore;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class LogsView {

    private Dispatcher circuit;
    private ResourceDescription rootDescription;
    private SecurityContext securityContext;

    private ElytronGenericResourceView fileAuditLogView;
    private ElytronGenericResourceView rotatingAuditLogView;
    private ElytronGenericResourceView syslogAuditLogView;

    public LogsView(final Dispatcher circuit, final ResourceDescription rootDescription,
            final SecurityContext securityFramework) {
        this.circuit = circuit;
        this.rootDescription = rootDescription;
        this.securityContext = securityFramework;
    }

    public Widget asWidget() {

        ResourceDescription fileDescription = rootDescription.getChildDescription("file-audit-log");
        ResourceDescription rotatingDescription = rootDescription.getChildDescription("rotating-file-audit-log");
        ResourceDescription syslogDescription = rootDescription.getChildDescription("syslog-audit-log");

        fileAuditLogView = new ElytronGenericResourceView(circuit, fileDescription, securityContext, "File Audit Log",
                ElytronStore.FILE_AUDIT_LOG_ADDRESS);

        rotatingAuditLogView = new ElytronGenericResourceView(circuit, rotatingDescription, securityContext, "Rotating File Audit Log",
                ElytronStore.ROTATING_FILE_AUDIT_ADDRESS);

        syslogAuditLogView = new ElytronGenericResourceView(circuit, syslogDescription, securityContext, "Syslog Audit Log",
                ElytronStore.SYSLOG_AUDIT_LOG_ADDRESS);


        PagedView panel = new PagedView(true);
        panel.addPage("File Audit Log", fileAuditLogView.asWidget());
        panel.addPage("Rotating File Audit Log", rotatingAuditLogView.asWidget());
        panel.addPage("Syslog Audit Log", syslogAuditLogView.asWidget());
        // default page
        panel.showPage(0);

        return panel.asWidget();
    }

    public void updateFileAuditLogView(final List<Property> model) {
        this.fileAuditLogView.update(model);
    }

    public void updateRotatingAuditLogView(final List<Property> model) {
        this.rotatingAuditLogView.update(model);
    }

    public void updateSyslogAuditLogView(final List<Property> model) {
        this.syslogAuditLogView.update(model);
    }
}
