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

    @Source("profiles_empty.html")
    ExternalTextResource profiles_empty();

    @Source("profiles_profile.html")
    ExternalTextResource profiles_profile();

    @Source("interfaces.html")
    ExternalTextResource interfaces();

    @Source("sockets.html")
    ExternalTextResource sockets();

    @Source("paths.html")
    ExternalTextResource paths();

    @Source("properties.html")
    ExternalTextResource properties();

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

    @Source("content/datasources.html")
    ExternalTextResource datasources();

    @Source("content/mail_sessions.html")
    ExternalTextResource mail_sessions();

    @Source("content/ee.html")
    ExternalTextResource ee();

    @Source("content/ejb3.html")
    ExternalTextResource ejb3();

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

    @Source("content/resource-adapters.html")
    ExternalTextResource resource_adapters();

    @Source("content/threads.html")
    ExternalTextResource threads();

    @Source("content/transactions.html")
    ExternalTextResource transactions();

    @Source("content/security-domains.html")
    ExternalTextResource security_domains();

    @Source("content/security.html")
    ExternalTextResource security();

    @Source("content/server-group.html")
    ExternalTextResource server_group();

    @Source("content/servlet.html")
    ExternalTextResource servlet();

    @Source("content/webservices.html")
    ExternalTextResource webservices();

    @Source("content/http.html")
    ExternalTextResource http();

    @Source("content/batch.html")
    ExternalTextResource batch();
}
