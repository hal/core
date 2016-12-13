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
public class SecurityRealmMapperView {

    private Dispatcher circuit;
    private ResourceDescription rootDescription;
    private SecurityContext securityContext;

    private ElytronGenericResourceView mappedRegexRealmMapperView;
    private ElytronGenericResourceView simpleRegexRealmMapperView;
    private ElytronGenericResourceView customRealmMapperView;
    private ElytronGenericResourceView constantRealmMapperView;

    public SecurityRealmMapperView(final Dispatcher circuit, final ResourceDescription rootDescription,
            final SecurityContext securityFramework) {
        this.circuit = circuit;
        this.rootDescription = rootDescription;
        this.securityContext = securityFramework;
    }

    public Widget asWidget() {

        ResourceDescription mappedRegexRealmMapperDescription = rootDescription.getChildDescription("mapped-regex-realm-mapper");
        ResourceDescription simpleRegexRealmMapperDescription = rootDescription.getChildDescription("simple-regex-realm-mapper");
        ResourceDescription customRealmMapperDescription = rootDescription.getChildDescription("custom-realm-mapper");
        ResourceDescription constantRealmMapperDescription = rootDescription.getChildDescription("constant-realm-mapper");
        
        mappedRegexRealmMapperView = new ElytronGenericResourceView(circuit, mappedRegexRealmMapperDescription, securityContext, "Mapped Regex Realm Mapper",
                ElytronStore.MAPPED_REGEX_REALM_MAPPER_ADDRESS);
        simpleRegexRealmMapperView= new ElytronGenericResourceView(circuit, simpleRegexRealmMapperDescription, securityContext, "Simple Realm Mapper",
                ElytronStore.SIMPLE_REGEX_REALM_MAPPER_ADDRESS);
        customRealmMapperView= new ElytronGenericResourceView(circuit, customRealmMapperDescription, securityContext, "Custom Realm Mapper",
                ElytronStore.CUSTOM_REALM_MAPPER_ADDRESS);
        constantRealmMapperView = new ElytronGenericResourceView(circuit, constantRealmMapperDescription, securityContext, "Constant Realm Mapper",
                ElytronStore.CONSTANT_REALM_MAPPER_ADDRESS);
        
        PagedView panel = new PagedView(true);
        panel.addPage("Mapped Regex Realm Mapper", mappedRegexRealmMapperView.asWidget());
        panel.addPage("Simple Regex Realm Mapper", simpleRegexRealmMapperView.asWidget());
        panel.addPage("Custom Realm Mapper", customRealmMapperView.asWidget());
        panel.addPage("Constant Realm Mapper", constantRealmMapperView.asWidget());
        // default page
        panel.showPage(0);
        
        return panel.asWidget();
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
    
    public void updateConstantRealmMapper(final List<Property> models) {
        constantRealmMapperView.update(models);
    }
}
