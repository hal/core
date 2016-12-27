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
public class ElytronSecurityRealmPresenter extends
        CircuitPresenter<ElytronSecurityRealmPresenter.MyView, ElytronSecurityRealmPresenter.MyProxy> {

    // @formatter:off
    @ProxyCodeSplit
    @NameToken(NameTokens.ElytronSecurityRealmPresenter)
    @RequiredResources(resources = ElytronStore.ROOT)
    public interface MyProxy extends Proxy<ElytronSecurityRealmPresenter>, Place {}

    public interface MyView extends View, HasPresenter<ElytronSecurityRealmPresenter> {

        void initSecurityRealm(
                List<Property> propertiesRealm,
                List<Property> filesystemRealm,
                List<Property> jdbcRealm,
                List<Property> ldapRealm,
                List<Property> keystoreRealm,
                List<Property> aggregateRealm,
                List<Property> customModifiableRealm,
                List<Property> customRealm,
                List<Property> identityRealm,
                List<Property> tokenRealm,
                List<Property> mappedRegexRealmMapper,
                List<Property> simpleRegexRealmMapper,
                List<Property> customRealmMapper,
                List<Property> constantRealmMapper,
                List<Property> authContext,
                List<Property> authConfiguration);

    }
    // @formatter:on


    private final Dispatcher circuit;
    private final RevealStrategy revealStrategy;
    private final ElytronStore store;
    private SecurityFramework securityFramework;
    private ResourceDescriptionRegistry descriptionRegistry;


    @Inject
    public ElytronSecurityRealmPresenter(EventBus eventBus, ElytronSecurityRealmPresenter.MyView view, ElytronSecurityRealmPresenter.MyProxy proxy,
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

            getView().initSecurityRealm(
                    store.getPropertiesRealm(),
                    store.getFilesystemRealm(),
                    store.getJdbcRealm(),
                    store.getLdapRealm(),
                    store.getKeystoreRealm(),
                    store.getAggregateRealm(),
                    store.getCustomModifiableRealm(),
                    store.getCustomRealm(),
                    store.getIdentityRealm(),
                    store.getTokenRealm(),
                    store.getMappedRegexRealmMapper(),
                    store.getSimpleRegexRealmMapper(),
                    store.getCustomRealmMapper(),
                    store.getConstantRealmMapper(),
                    store.getAuthenticationContext(),
                    store.getAuthenticationconfiguration());
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