package org.jboss.as.console.client.preview;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.resources.client.ClientBundleWithLookup;
import com.google.gwt.resources.client.ExternalTextResource;

/**
 * @author Heiko Braun
 * @since 27/03/15
 */
public interface PreviewContent extends ClientBundleWithLookup {

    PreviewContent INSTANCE = GWT.create(PreviewContent.class);

    @Source("access_control_empty.html")
    ExternalTextResource access_control_empty();

    @Source("assignment.html")
    ExternalTextResource assignment();

    @Source("deployments_empty.html")
    ExternalTextResource deployments_empty();

    @Source("interfaces.html")
    ExternalTextResource interfaces();

    @Source("membership.html")
    ExternalTextResource membership();

    @Source("paths.html")
    ExternalTextResource paths();

    @Source("profiles_empty.html")
    ExternalTextResource profiles_empty();

    @Source("profile_empty.html")
    ExternalTextResource profile_empty();

    @Source("profiles_profile.html")
    ExternalTextResource profiles_profile();

    @Source("properties.html")
    ExternalTextResource properties();

    @Source("runtime_empty.html")
    ExternalTextResource runtime_empty();

    @Source("sockets.html")
    ExternalTextResource sockets();

    @Source("users.html")
    ExternalTextResource users();

    @Source("groups.html")
    ExternalTextResource groups();

    @Source("roles.html")
    ExternalTextResource roles();

    // standard role names
    // must match the lowercase names from /core-service=management/access=authorization(standard-role-names)
    @Source("roles/administrator.html")
    ExternalTextResource administrator();

    @Source("roles/auditor.html")
    ExternalTextResource auditor();

    @Source("roles/deployer.html")
    ExternalTextResource deployer();

    @Source("roles/maintainer.html")
    ExternalTextResource maintainer();

    @Source("roles/monitor.html")
    ExternalTextResource monitor();

    @Source("roles/operator.html")
    ExternalTextResource operator();

    @Source("roles/superuser.html")
    ExternalTextResource superuser();

    // content below (referenced by name token)
    @Source("content/batch.html")
    ExternalTextResource batch();

    @Source("content/content_repository.html")
    ExternalTextResource content_repository();

    @Source("content/datasources.html")
    ExternalTextResource ds_finder();

    @Source("content/mail_sessions.html")
    ExternalTextResource mail_sessions();

    @Source("content/ee.html")
    ExternalTextResource ee();

    @Source("content/ejb3.html")
    ExternalTextResource ejb3();

    @Source("content/http.html")
    ExternalTextResource http();

    @Source("content/jacorb.html")
    ExternalTextResource jacorb();

    @Source("content/jca.html")
    ExternalTextResource jca();

    @Source("content/jmx.html")
    ExternalTextResource jmx();

    @Source("content/jpa.html")
    ExternalTextResource jpa();

    @Source("content/logging.html")
    ExternalTextResource logging();

    @Source("content/mail.html")
    ExternalTextResource mail();

    @Source("content/resource_adapters.html")
    ExternalTextResource ra_finder();

    @Source("content/security.html")
    ExternalTextResource security();

    @Source("content/security_domains.html")
    ExternalTextResource security_domains();

    @Source("content/server_group.html")
    ExternalTextResource server_group();

    @Source("content/servlet.html")
    ExternalTextResource servlet();

    @Source("content/transactions.html")
    ExternalTextResource transactions();

    @Source("content/threads.html")
    ExternalTextResource threads();

    @Source("content/webservices.html")
    ExternalTextResource webservices();

    @Source("content/unassigned_content.html")
    ExternalTextResource unassigned_content();

    @Source("content/iiop_openjdk.html")
    ExternalTextResource iiop_openjdk();

    @Source("content/remoting.html")
    ExternalTextResource remoting();

    @Source("content/io_subsystem.html")
    ExternalTextResource io();

    @Source("content/jgroups.html")
    ExternalTextResource jgroups();

    @Source("content/messaging_activemq.html")
    ExternalTextResource activemq();

    @Source("content/undertow_subsystem.html")
    ExternalTextResource undertow_subsystem();
}
