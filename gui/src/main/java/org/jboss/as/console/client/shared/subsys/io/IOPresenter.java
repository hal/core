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
package org.jboss.as.console.client.shared.subsys.io;

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
import org.jboss.as.console.spi.AccessControl;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.Map;

/**
 * @author Harald Pehl
 */
public class IOPresenter extends CircuitPresenter<IOPresenter.MyView, IOPresenter.MyProxy> {

    public interface MyView extends View, HasPresenter<IOPresenter> {
    }

    @ProxyCodeSplit
    @NameToken(NameTokens.IO)
    @AccessControl(resources = "{selected.profile}/subsystem=io")
    public interface MyProxy extends Proxy<IOPresenter>, Place {
    }

    private final RevealStrategy revealStrategy;
    private final Dispatcher circuit;

    public IOPresenter(EventBus eventBus, MyView view, MyProxy proxy,
                       RevealStrategy revealStrategy, Dispatcher circuit) {
        super(eventBus, view, proxy);
        this.revealStrategy = revealStrategy;
        this.circuit = circuit;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    protected void onAction(Class<?> actionType) {

    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
    }

    // ------------------------------------------------------ worker methods

    public void launchAddWorkerDialog() {

    }

    public void saveWorker(String name, Map<String, Object> changedValues) {

    }

    public void removeWorker(String name) {

    }

    // ------------------------------------------------------ buffer pool methods

    public void launchAddBufferPoolDialog() {

    }

    public void saveBufferPool(String name, Map<String, Object> changedValues) {

    }

    public void removeBufferPool(String name) {

    }
}
