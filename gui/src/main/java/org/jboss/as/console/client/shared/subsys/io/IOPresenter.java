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
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.io.bufferpool.*;
import org.jboss.as.console.client.shared.subsys.io.worker.*;
import org.jboss.as.console.mbui.behaviour.ModelNodeAdapter;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.ADDRESS;

/**
 * @author Harald Pehl
 */
public class IOPresenter extends CircuitPresenter<IOPresenter.MyView, IOPresenter.MyProxy> {

    public interface MyView extends View, HasPresenter<IOPresenter> {
        void updateBufferPools(List<Property> bufferPools);

        void updateWorkers(List<Property> workers);
    }

    @ProxyCodeSplit
    @NameToken(NameTokens.IO)
    @AccessControl(resources = "{selected.profile}/subsystem=io")
    public interface MyProxy extends Proxy<IOPresenter>, Place {
    }


    private final RevealStrategy revealStrategy;
    private final Dispatcher circuit;
    private final BufferPoolStore bufferPoolStore;
    private final WorkerStore workerStore;

    @Inject
    public IOPresenter(EventBus eventBus, MyView view, MyProxy proxy,
                       RevealStrategy revealStrategy, Dispatcher circuit,
                       BufferPoolStore bufferPoolStore, WorkerStore workerStore) {
        super(eventBus, view, proxy);
        this.revealStrategy = revealStrategy;
        this.circuit = circuit;
        this.bufferPoolStore = bufferPoolStore;
        this.workerStore = workerStore;
    }


    // ------------------------------------------------------ lifecycle

    @Override
    protected void onBind() {
        super.onBind();
        addChangeHandler(bufferPoolStore);
        addChangeHandler(workerStore);
        getView().setPresenter(this);
    }

    @Override
    protected void onAction(Class<?> actionType) {
        if (actionType.equals(AddBufferPool.class) ||
                actionType.equals(ModifyBufferPool.class) ||
                actionType.equals(RefreshBufferPools.class) ||
                actionType.equals(RemoveBufferPool.class)) {
            getView().updateBufferPools(bufferPoolStore.getBufferPools());

        } else if (actionType.equals(AddWorker.class) ||
                actionType.equals(ModifyWorker.class) ||
                actionType.equals(RefreshWorkers.class) ||
                actionType.equals(RemoveWorker.class)) {
            getView().updateWorkers(workerStore.getWorkers());
        }
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        circuit.dispatch(new RefreshWorkers());
        circuit.dispatch(new RefreshBufferPools());
    }

    // ------------------------------------------------------ worker methods

    public void launchAddWorkerDialog() {
    }

    public void modifyWorker(String name, Map<String, Object> changedValues) {
        circuit.dispatch(new ModifyWorker(modifyNode("worker", name, changedValues)));
    }

    public void removeWorker(String name) {
        circuit.dispatch(new RemoveWorker(name));
    }

    // ------------------------------------------------------ buffer pool methods

    public void launchAddBufferPoolDialog() {
    }

    public void modifyBufferPool(String name, Map<String, Object> changedValues) {
        circuit.dispatch(new ModifyBufferPool(modifyNode("buffer-pool", name, changedValues)));
    }

    public void removeBufferPool(String name) {
        circuit.dispatch(new RemoveBufferPool(name));
    }


    // ------------------------------------------------------ helper methods

    private ModelNode modifyNode(String resource, String name, Map<String, Object> changedValues) {
        final ModelNodeAdapter adapter = new ModelNodeAdapter();
        ModelNode address = new ModelNode();
        address.get(ADDRESS).set(Baseadress.get());
        address.get(ADDRESS).add("subsystem", "io");
        address.get(ADDRESS).add(resource, name);
        return adapter.fromChangeset(changedValues, address);
    }
}
