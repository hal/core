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
package org.jboss.as.console.client.shared.subsys.elytron;

import java.util.List;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.core.CircuitPresenter;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.elytron.store.ElytronConfigAction;
import org.jboss.as.console.client.shared.subsys.elytron.store.ElytronStore;
import org.jboss.as.console.client.shared.subsys.elytron.store.InitElytron;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class ElytronMapperPresenter extends
        CircuitPresenter<ElytronMapperPresenter.MyView, ElytronMapperPresenter.MyProxy> {

    // @formatter:off
    @ProxyCodeSplit
    @NameToken(NameTokens.ElytronMapperPresenter)
    @RequiredResources(resources = ElytronStore.ROOT)
    public interface MyProxy extends Proxy<ElytronMapperPresenter>, Place {}

    public interface MyView extends View, HasPresenter<ElytronMapperPresenter> {

        void initRoleMapper(
                List<Property> addPrefixRoleMapper,
                List<Property> addSuffixRoleMapper,
                List<Property> aggregateRoleMapper,
                List<Property> constantRoleMapper,
                List<Property> customRoleMapper,
                List<Property> logicalRoleMapper
        );
        
        void initPermissionMapper(
                List<Property> simplePermissionMapper,
                List<Property> logicalPermissionMapper,
                List<Property> customPermissionMapper,
                List<Property> constantPermissionMapper
        );
        
        void initDecoder(
                List<Property> aggregatePrincipalDecoder,
                List<Property> concatenatingPrincipalDecoder,
                List<Property> constantPrincipalDecoder,
                List<Property> customPrincipalDecoder,
                List<Property> x500PrincipalDecoder,
                List<Property> customRoleDecoder,
                List<Property> simpleRoleDecoder
        );
    }
    // @formatter:on


    private final Dispatcher circuit;
    private final RevealStrategy revealStrategy;
    private final ElytronStore store;
    private SecurityFramework securityFramework;
    private ResourceDescriptionRegistry descriptionRegistry;


    @Inject
    public ElytronMapperPresenter(EventBus eventBus, ElytronMapperPresenter.MyView view, ElytronMapperPresenter.MyProxy proxy,
            Dispatcher circuit, RevealStrategy revealStrategy, ElytronStore store,
            SecurityFramework securityFramework, ResourceDescriptionRegistry descriptionRegistry) {
        super(eventBus, view, proxy, circuit);
        this.circuit = circuit;
        this.revealStrategy = revealStrategy;
        this.store = store;
        this.securityFramework = securityFramework;
        this.descriptionRegistry = descriptionRegistry;
    }

    @Override
    protected void onBind() {
        super.onBind();
        addChangeHandler(store);
        getView().setPresenter(this);
    }

    @Override
    protected void onAction(Action action) {
        if (action instanceof ElytronConfigAction) {
            
            getView().initRoleMapper(
                store.getAddPrefixRoleMapper(),
                store.getAddSuffixRoleMapper(),
                store.getAggregateRoleMapper(),
                store.getConstantRoleMapper(),
                store.getCustomRoleMapper(),
                store.getLogicalRoleMapper()
            );
            
            getView().initPermissionMapper(
                store.getSimplePermissionMapper(),
                store.getLogicalPermissionMapper(),
                store.getCustomPermissionMapper(),
                store.getConstantPermissionMapper()    
            );
            
            getView().initDecoder(
                store.getAggregatePrincipalDecoder(),
                store.getConcatenatingPrincipalDecoder(),
                store.getConstantPrincipalDecoder(),
                store.getCustomPrincipalDecoder(),
                store.getX500PrincipalDecoder(),
                store.getCustomRoleDecoder(),
                store.getSimpleRoleDecoder()
            );
            
        }
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        circuit.dispatch(new InitElytron());
    }

    public SecurityFramework getSecurityFramework() {
        return securityFramework;
    }

    public ResourceDescriptionRegistry getDescriptionRegistry() {
        return descriptionRegistry;
    }

}