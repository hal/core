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
package org.jboss.as.console.client.shared.subsys.elytron.ui.mapper;

import java.util.List;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.elytron.ElytronMapperPresenter;
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
public class ElytronMapperView extends SuspendableViewImpl implements ElytronMapperPresenter.MyView {

    private final Dispatcher circuit;
    private final ResourceDescriptionRegistry resourceDescriptionRegistry;
    private final SecurityFramework securityFramework;
    private ElytronMapperPresenter presenter;
    
    private RoleMapperView roleMapperView;
    private PermissionMapperView permissionMapperView;
    private DecoderView decoderView;

    @Inject
    public ElytronMapperView(final Dispatcher circuit, final ResourceDescriptionRegistry resourceDescriptionRegistry,
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
        
        roleMapperView = new RoleMapperView(circuit, rootDescription, securityContext);
        permissionMapperView = new PermissionMapperView(circuit, rootDescription, securityContext);
        decoderView = new DecoderView(circuit, rootDescription, securityContext);

        tabLayoutpanel.add(roleMapperView.asWidget(), "Role Mapper", true);
        tabLayoutpanel.add(permissionMapperView.asWidget(), "Permission Mapper", true);
        tabLayoutpanel.add(decoderView.asWidget(), "Decoder", true);
        tabLayoutpanel.selectTab(0);

        return tabLayoutpanel;
    }

    @Override
    public void setPresenter(final ElytronMapperPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void initRoleMapper(final List<Property> addPrefixRoleMapper, final List<Property> addSuffixRoleMapper,
            final List<Property> aggregateRoleMapper, final List<Property> constantRoleMapper,
            final List<Property> customRoleMapper,
            final List<Property> logicalRoleMapper) {
     
        roleMapperView.updateAddPrefixRoleMapper(addPrefixRoleMapper);
        roleMapperView.updateAddSuffixRoleMapper(addSuffixRoleMapper);
        roleMapperView.updateAggregateRoleMapper(aggregateRoleMapper);
        roleMapperView.updateConstantRoleMapper(constantRoleMapper);
        roleMapperView.updateCustomRoleMapper(customRoleMapper);
        roleMapperView.updateLogicalRoleMapper(logicalRoleMapper);
    }

    @Override
    public void initPermissionMapper(final List<Property> simplePermissionMapper,
            final List<Property> logicalPermissionMapper,
            final List<Property> customPermissionMapper,
            final List<Property> constantPermissionMapper) {

        permissionMapperView.updateSimplePermissionMapper(simplePermissionMapper);
        permissionMapperView.updateLogicalPermissionMapper(logicalPermissionMapper);
        permissionMapperView.updateCustomPermissionMapper(customPermissionMapper);
        permissionMapperView.updateConstantPermissionMapper(constantPermissionMapper);
    }

    @Override
    public void initDecoder(final List<Property> aggregatePrincipalDecoder,
            final List<Property> concatenatingPrincipalDecoder,
            final List<Property> constantPrincipalDecoder, final List<Property> customPrincipalDecoder,
            final List<Property> x500PrincipalDecoder, final List<Property> customRoleDecoder,
            final List<Property> simpleRoleDecoder) {
        
        decoderView.updateAggregatePrincipal(aggregatePrincipalDecoder);
        decoderView.updateConcatenatingPrincipal(concatenatingPrincipalDecoder);
        decoderView.updateConstantPrincipal(constantPrincipalDecoder);
        decoderView.updateCustomPrincipal(customPrincipalDecoder);
        decoderView.updateX500AttributePrincipal(x500PrincipalDecoder);
        decoderView.updateSimpleRole(simpleRoleDecoder);
        decoderView.updateCustomRole(customRoleDecoder);

    }
}
