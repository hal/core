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

    @Source("content/runtime_ds_metrics.html")
    ExternalTextResource runtime_ds_metrics();

    @Source("runtime_empty_standalone.html")
    ExternalTextResource runtime_empty_standalone();

    @Source("runtime_empty_domain.html")
    ExternalTextResource runtime_empty_domain();

    @Source("content/runtime_http_metrics.html")
    ExternalTextResource runtime_http_metrics();

    @Source("runtime_hosts.html")
    ExternalTextResource runtime_hosts();

    @Source("runtime_host.html")
    ExternalTextResource runtime_host();

    @Source("runtime_server_groups.html")
    ExternalTextResource runtime_server_groups();

    @Source("runtime_server_group.html")
    ExternalTextResource runtime_server_group();

    @Source("content/runtime_tx_metrics.html")
    ExternalTextResource runtime_tx_metrics();

    @Source("content/runtime_webservice_runtime.html")
    ExternalTextResource runtime_webservice_runtime();

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

    @Source("content/batch.html")
    ExternalTextResource batch_jberet();

    @Source("content/cache_container.html")
    ExternalTextResource cache_container();

    @Source("content/content_repository.html")
    ExternalTextResource content_repository();

    @Source("content/datasources.html")
    ExternalTextResource ds_finder();

    @Source("content/deployment_scanner.html")
    ExternalTextResource deployment_scanner();

    @Source("content/elytron_factory.html")
    ExternalTextResource elytron_factory();

    @Source("content/elytron_mapper.html")
    ExternalTextResource elytron_mapper();

    @Source("content/elytron_settings.html")
    ExternalTextResource elytron_settings();

    @Source("content/elytron_security_realm.html")
    ExternalTextResource elytron_security_realm();

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

    @Source("content/jms_bridge.html")
    ExternalTextResource jms_bridge();

    @Source("content/jmx.html")
    ExternalTextResource jmx();

    @Source("content/jpa.html")
    ExternalTextResource jpa();

    @Source("content/jsp_servlet.html")
    ExternalTextResource jsp_servlet();

    @Source("content/logging.html")
    ExternalTextResource logging();

    @Source("content/mail.html")
    ExternalTextResource mail();

    @Source("content/messaging_provider.html")
    ExternalTextResource messaging_provider();

    @Source("content/mod_cluster.html")
    ExternalTextResource modcluster();

    @Source("content/picketlink_federations.html")
    ExternalTextResource picketlink_federations();

    @Source("content/resource_adapters.html")
    ExternalTextResource ra_finder();

    @Source("content/security.html")
    ExternalTextResource security();

    @Source("content/security_domains.html")
    ExternalTextResource security_domains();

    @Source("content/server_group.html")
    ExternalTextResource server_group();

    @Source("content/server_group_content.html")
    ExternalTextResource server_group_content();

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

    @Source("content/undertow_filters.html")
    ExternalTextResource undertow_filters();
}
