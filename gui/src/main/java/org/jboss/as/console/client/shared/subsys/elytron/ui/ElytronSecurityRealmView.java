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

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.elytron.ElytronSecurityRealmPresenter;
import org.jboss.as.console.client.shared.subsys.elytron.store.ElytronStore;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class ElytronSecurityRealmView extends SuspendableViewImpl implements ElytronSecurityRealmPresenter.MyView {

    private final Dispatcher circuit;
    private final ResourceDescriptionRegistry resourceDescriptionRegistry;
    private final SecurityFramework securityFramework;
    private ElytronSecurityRealmPresenter presenter;
    private SecurityRealmView securityRealmView;
    private SecurityRealmMapperView securityRealmMapperView;

    @Inject
    public ElytronSecurityRealmView(final Dispatcher circuit, final ResourceDescriptionRegistry resourceDescriptionRegistry,
            final SecurityFramework securityFramework) {
        this.circuit = circuit;
        this.resourceDescriptionRegistry = resourceDescriptionRegistry;
        this.securityFramework = securityFramework;
    }

    @Override
    public Widget createWidget() {

        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());
        ResourceDescription rootDescription = resourceDescriptionRegistry.lookup(ElytronStore.ROOT_ADDRESS);
        
        DefaultTabLayoutPanel tabLayoutpanel = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        tabLayoutpanel.addStyleName("default-tabpanel");
        
        securityRealmView = new SecurityRealmView(circuit, rootDescription, securityContext);
        securityRealmMapperView = new SecurityRealmMapperView(circuit, rootDescription, securityContext);

        tabLayoutpanel.add(securityRealmView.asWidget(), "Security Realm", true);
        tabLayoutpanel.add(securityRealmMapperView.asWidget(), "Security Realm Mapper", true);
        tabLayoutpanel.selectTab(0);
        return tabLayoutpanel;
    }

    @Override
    public void initSecurityRealm(List<Property> propertiesRealm, List<Property> filesystemRealm, 
            List<Property> jdbcRealm, List<Property> ldapRealm, List<Property> keystoreRealm, 
            List<Property> aggregateRealm, List<Property> customModifiableRealm, List<Property> customRealm,
            List<Property> identityRealm, List<Property> tokenRealm,
            List<Property> mappedRegexRealmMapper, List<Property> simpleRegexRealmMapper, List<Property> customRealmMapper,
            List<Property> constantRealmMapper) {
        
        securityRealmView.updatePropertiesRealm(propertiesRealm);
        securityRealmView.updateFilesystemRealm(filesystemRealm);
        securityRealmView.updateJdbcRealm(jdbcRealm);
        securityRealmView.updateLdapRealm(ldapRealm);
        securityRealmView.updateKeystoreRealm(keystoreRealm);
        securityRealmView.updateAggregateRealm(aggregateRealm);
        securityRealmView.updateCustomModifiableRealm(customModifiableRealm);
        securityRealmView.updateCustomRealm(customRealm);
        securityRealmView.updateIdentityRealm(identityRealm);
        securityRealmView.updateTokenmRealm(tokenRealm);
        
        
        securityRealmMapperView.updateMappedRegexRealmMapper(mappedRegexRealmMapper);
        securityRealmMapperView.updateSimpleRegexRealmMapper(simpleRegexRealmMapper);
        securityRealmMapperView.updateCustomRealmMapper(customRealmMapper);
        securityRealmMapperView.updateConstantRealmMapper(constantRealmMapper);
    }

    @Override
    public void setPresenter(final ElytronSecurityRealmPresenter presenter) {
        this.presenter = presenter;
    }

}
