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
import org.jboss.as.console.client.shared.subsys.jberet.store.JberetStore;
import org.jboss.as.console.client.shared.subsys.jberet.store.LoadThreadPoolMetrics;
import org.jboss.as.console.client.shared.subsys.jberet.store.RefreshThreadPoolMetric;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.List;

/**
 * @author Harald Pehl
 */
public class JberetMetricsPresenter
        extends CircuitPresenter<JberetMetricsPresenter.MyView, JberetMetricsPresenter.MyProxy> {

    // @formatter:off
    @ProxyCodeSplit
    @NameToken(NameTokens.BatchJberetMetrics)
    @RequiredResources(resources = JberetStore.METRICS_ROOT)
    public interface MyProxy extends Proxy<JberetMetricsPresenter>, Place {}

    public interface MyView extends View, HasPresenter<JberetMetricsPresenter> {
        void refresh(ModelNode metric);
        void refresh(List<Property> metrics);
    }
    // @formatter:on


    private final Dispatcher circuit;
    private final RevealStrategy revealStrategy;
    private final JberetStore store;

    @Inject
    public JberetMetricsPresenter(EventBus eventBus, MyView view, MyProxy proxy,
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
        if (action instanceof LoadThreadPoolMetrics) {
            getView().refresh(store.getThreadPoolMetrics());
        } else if (action instanceof RefreshThreadPoolMetric) {
            getView().refresh(store.getCurrentThreadPoolMetric());
        }
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInRuntimeParent(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        circuit.dispatch(new LoadThreadPoolMetrics());
    }
}
