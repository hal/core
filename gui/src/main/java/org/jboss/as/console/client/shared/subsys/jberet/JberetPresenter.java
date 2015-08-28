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
package org.jboss.as.console.client.shared.subsys.jberet;

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
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.jberet.store.InitJberet;
import org.jboss.as.console.client.shared.subsys.jberet.store.JberetAction;
import org.jboss.as.console.client.shared.subsys.jberet.store.JberetStore;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.List;

/**
 * @author Harald Pehl
 */
public class JberetPresenter extends CircuitPresenter<JberetPresenter.MyView, JberetPresenter.MyProxy> {

    // @formatter:off
    @ProxyCodeSplit
    @NameToken(NameTokens.BatchJberet)
    @RequiredResources(resources = JberetStore.ROOT)
    public interface MyProxy extends Proxy<JberetPresenter>, Place {}

    public interface MyView extends View, HasPresenter<JberetPresenter> {
        void init(ModelNode defaults, List<Property> inMemoryRepositories, List<Property> jdbcRepositories,
                List<Property> threadFactories, List<Property> threadPools);
    }
    // @formatter:on


    private final Dispatcher circuit;
    private final RevealStrategy revealStrategy;
    private final JberetStore store;

    @Inject
    public JberetPresenter(EventBus eventBus, MyView view, MyProxy proxy,
            Dispatcher circuit, RevealStrategy revealStrategy, JberetStore store) {
        super(eventBus, view, proxy, circuit);
        this.circuit = circuit;
        this.revealStrategy = revealStrategy;
        this.store = store;
    }

    @Override
    protected void onBind() {
        super.onBind();
        addChangeHandler(store);
        getView().setPresenter(this);
    }

    @Override
    protected void onAction(Action action) {
        if (action instanceof JberetAction) {
            // since any action delegates to InitJberet we can just call View.init() no matter what action was triggered
            getView().init(store.getDefaults(), store.getInMemoryRepositories(), store.getJdbcRepositories(),
                    store.getThreadFactories(), store.getThreadPools());
        }
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        circuit.dispatch(new InitJberet());
    }
}