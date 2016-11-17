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
public class SecurityRealmView {

    private Dispatcher circuit;
    private ResourceDescription rootDescription;
    private SecurityContext securityContext;

    private PropertiesRealmView propertiesRealmView;
    private ElytronGenericResourceView filesystemRealmView;
    private JdbcRealmView jdbcRealmView;
    private LdapRealmView ldapView;
    private ElytronGenericResourceView keyStoreRealmView;
    private ElytronGenericResourceView aggregateRealmView;
    private ElytronGenericResourceView customModifiableRealmView;
    private ElytronGenericResourceView customRealmView;
    private ElytronGenericResourceView mappedRegexRealmMapperView;
    private ElytronGenericResourceView simpleRegexRealmMapperView;
    private ElytronGenericResourceView customRealmMapperView;

    public SecurityRealmView(final Dispatcher circuit, final ResourceDescription rootDescription,
            final SecurityContext securityFramework) {
        this.circuit = circuit;
        this.rootDescription = rootDescription;
        this.securityContext = securityFramework;
    }

    public Widget asWidget() {

        ResourceDescription propsRealmDescription = rootDescription.getChildDescription("properties-realm");
        ResourceDescription filesystemRealmDescription = rootDescription.getChildDescription("filesystem-realm");
        ResourceDescription jdbcRealmDescription = rootDescription.getChildDescription("jdbc-realm");
        ResourceDescription ldapRealmDescription = rootDescription.getChildDescription("ldap-realm");
        ResourceDescription keystoreRealmDescription = rootDescription.getChildDescription("key-store-realm");
        ResourceDescription aggregateRealmDescription = rootDescription.getChildDescription("aggregate-realm");
        ResourceDescription customModifiableRealmDescription = rootDescription.getChildDescription("custom-modifiable-realm");
        ResourceDescription customRealmDescription = rootDescription.getChildDescription("custom-realm");
        ResourceDescription mappedRegexRealmMapperDescription = rootDescription.getChildDescription("mapped-regex-realm-mapper");
        ResourceDescription simpleRegexRealmMapperDescription = rootDescription.getChildDescription("simple-regex-realm-mapper");
        ResourceDescription customRealmMapperDescription = rootDescription.getChildDescription("custom-realm-mapper");
        
        propertiesRealmView = new PropertiesRealmView(circuit, propsRealmDescription, securityContext);
        filesystemRealmView = new ElytronGenericResourceView(circuit, filesystemRealmDescription, securityContext, "Filesystem Realm",
                ElytronStore.FILESYSTEM_REALM_ADDRESS);
        jdbcRealmView = new JdbcRealmView(circuit, jdbcRealmDescription, securityContext); 
        ldapView = new LdapRealmView(circuit, ldapRealmDescription, securityContext);
        keyStoreRealmView = new ElytronGenericResourceView(circuit, keystoreRealmDescription, securityContext, "Keystore Realm",
                ElytronStore.KEYSTORE_REALM_ADDRESS);
        aggregateRealmView= new ElytronGenericResourceView(circuit, aggregateRealmDescription, securityContext, "Aggregate Realm",
                ElytronStore.AGGREGATE_REALM_ADDRESS);
        customModifiableRealmView= new ElytronGenericResourceView(circuit, customModifiableRealmDescription, securityContext, "Custom Modifiable Realm",
                ElytronStore.CUSTOM_MODIFIABLE_REALM_ADDRESS);
        customRealmView= new ElytronGenericResourceView(circuit, customRealmDescription, securityContext, "Custom Realm",
                ElytronStore.CUSTOM_REALM_ADDRESS);
        mappedRegexRealmMapperView = new ElytronGenericResourceView(circuit, mappedRegexRealmMapperDescription, securityContext, "Mapped Regex Realm Mapper",
                ElytronStore.MAPPED_REGEX_REALM_MAPPER_ADDRESS);
        simpleRegexRealmMapperView= new ElytronGenericResourceView(circuit, simpleRegexRealmMapperDescription, securityContext, "Simple Realm Mapper",
                ElytronStore.SIMPLE_REGEX_REALM_MAPPER_ADDRESS);
        customRealmMapperView= new ElytronGenericResourceView(circuit, customRealmMapperDescription, securityContext, "Custom Realm Mapper",
                ElytronStore.CUSTOM_REALM_MAPPER_ADDRESS);
        
        PagedView panel = new PagedView(true);
        panel.addPage("Properties Realm", propertiesRealmView.asWidget());
        panel.addPage("Filesystem Realm", filesystemRealmView.asWidget());
        panel.addPage("JDBC Realm", jdbcRealmView.asWidget());
        panel.addPage("LDAP Realm", ldapView.asWidget());
        panel.addPage("Key Store Realm", keyStoreRealmView.asWidget());
        panel.addPage("Aggregate Realm", aggregateRealmView.asWidget());
        panel.addPage("Custom Modifiable Realm", customModifiableRealmView.asWidget());
        panel.addPage("Custom Realm", customRealmView.asWidget());
        panel.addPage("Mapped Regex Realm Mapper", mappedRegexRealmMapperView.asWidget());
        panel.addPage("Simple Regex Realm Mapper", simpleRegexRealmMapperView.asWidget());
        panel.addPage("Custom Realm Mapper", customRealmMapperView.asWidget());
        // default page
        panel.showPage(0);
        
        return panel.asWidget();
    }
    
    public void updatePropertiesRealm(List<Property> nodes) {
        propertiesRealmView.update(nodes);
    }

    public void updateFilesystemRealm(final List<Property> models) {
        filesystemRealmView.update(models);
    }
    
    public void updateJdbcRealm(final List<Property> models) {
        jdbcRealmView.update(models);
    }
    
    public void updateLdapRealm(final List<Property> models) {
        ldapView.update(models);
    }
    
    public void updateKeystoreRealm(final List<Property> models) {
        keyStoreRealmView.update(models);
    }
    
    public void updateAggregateRealm(final List<Property> models) {
        aggregateRealmView.update(models);
    }
    
    public void updateCustomModifiableRealm(final List<Property> models) {
        customModifiableRealmView.update(models);
    }
    
    public void updateCustomRealm(final List<Property> models) {
        customRealmView.update(models);
    }
    
    public void updateMappedRegexRealmMapper(final List<Property> models) {
        mappedRegexRealmMapperView.update(models);
    }
    
    public void updateSimpleRegexRealmMapper(final List<Property> models) {
        simpleRegexRealmMapperView.update(models);
    }
    
    public void updateCustomRealmMapper(final List<Property> models) {
        customRealmMapperView.update(models);
    }
}
