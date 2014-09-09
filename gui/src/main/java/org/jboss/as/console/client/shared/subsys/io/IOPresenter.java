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
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.io.bufferpool.*;
import org.jboss.as.console.client.shared.subsys.io.worker.*;
import org.jboss.as.console.client.v3.stores.ModifyPayload;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.widgets.AddResourceDialog;
import org.jboss.as.console.mbui.widgets.AddressableResourceView;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.Map;

/**
 * @author Harald Pehl
 */
public class IOPresenter extends CircuitPresenter<IOPresenter.MyView, IOPresenter.MyProxy> {

    public interface MyView extends View, AddressableResourceView, HasPresenter<IOPresenter> {
    }

    @ProxyCodeSplit
    @NameToken(NameTokens.IO)
    @AccessControl(resources = "{selected.profile}/subsystem=io")
    public interface MyProxy extends Proxy<IOPresenter>, Place {
    }


    private final Dispatcher circuit;
    private final RevealStrategy revealStrategy;
    private final SecurityFramework securityFramework;
    private final BufferPoolStore bufferPoolStore;
    private final WorkerStore workerStore;
    private final ResourceAddress bufferPoolAddressTemplate;
    private final ResourceAddress workerAddressTemplate;

    private DefaultWindow window;
    private AddResourceDialog addWorkerDialog;
    private AddResourceDialog addBufferPoolDialog;

    @Inject
    public IOPresenter(EventBus eventBus, MyView view, MyProxy proxy, Dispatcher circuit,
                       RevealStrategy revealStrategy, SecurityFramework securityFramework,
                       BufferPoolStore bufferPoolStore, WorkerStore workerStore) {
        super(eventBus, view, proxy, circuit);

        this.circuit = circuit;
        this.revealStrategy = revealStrategy;
        this.securityFramework = securityFramework;
        this.bufferPoolStore = bufferPoolStore;
        this.workerStore = workerStore;

        this.bufferPoolAddressTemplate = new ResourceAddress("{selected.profile}/subsystem=io/buffer-pool=*",
                bufferPoolStore.getStatementContext());
        this.workerAddressTemplate = new ResourceAddress("{selected.profile}/subsystem=io/worker=*",
                workerStore.getStatementContext());
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
        if (actionType.equals(AddBufferPool.class) || actionType.equals(ModifyBufferPool.class)) {
            getView().update(bufferPoolAddressTemplate, bufferPoolStore.getBufferPools());
            getView().select(bufferPoolAddressTemplate, bufferPoolStore.getLastModifiedBufferPool());

        } else if (actionType.equals(RefreshBufferPools.class) || actionType.equals(RemoveBufferPool.class)) {
            getView().update(bufferPoolAddressTemplate, bufferPoolStore.getBufferPools());

        } else if (actionType.equals(AddWorker.class) || actionType.equals(ModifyWorker.class)) {
            getView().update(workerAddressTemplate, workerStore.getWorkers());
            getView().select(workerAddressTemplate, workerStore.getLastModifiedWorker());

        } else if (actionType.equals(RefreshWorkers.class) || actionType.equals(RemoveWorker.class)) {
            getView().update(workerAddressTemplate, workerStore.getWorkers());
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
        if (addWorkerDialog == null) {
            addWorkerDialog = new AddResourceDialog("{selected.profile}/subsystem=io/worker=*",
                    workerStore.getStatementContext(), securityFramework.getSecurityContext(),
                    new AddResourceDialog.Callback() {
                        @Override
                        public void onAddResource(ResourceAddress address, ModelNode payload) {
                            window.hide();
                            circuit.dispatch(new AddWorker(payload));
                        }

                        @Override
                        public void closeDialogue() {
                            window.hide();
                        }
                    }
            );
        } else {
            addWorkerDialog.clearValues();
        }

        window = new DefaultWindow("Worker");
        window.setWidth(480);
        window.setHeight(360);
        window.setWidget(addWorkerDialog);
        window.setGlassEnabled(true);
        window.center();
    }

    public void modifyWorker(String name, Map<String, Object> changedValues) {
        circuit.dispatch(new ModifyWorker(new ModifyPayload(name, changedValues)));
    }

    public void removeWorker(String name) {
        circuit.dispatch(new RemoveWorker(name));
    }


    // ------------------------------------------------------ buffer pool methods

    public void launchAddBufferPoolDialog() {
        if (addBufferPoolDialog == null) {
            addBufferPoolDialog = new AddResourceDialog("{selected.profile}/subsystem=io/buffer-pool=*",
                    bufferPoolStore.getStatementContext(), securityFramework.getSecurityContext(),
                    new AddResourceDialog.Callback() {
                        @Override
                        public void onAddResource(ResourceAddress address, ModelNode payload) {
                            window.hide();
                            circuit.dispatch(new AddBufferPool(payload));
                        }

                        @Override
                        public void closeDialogue() {
                            window.hide();
                        }
                    }
            );
        } else {
            addBufferPoolDialog.clearValues();
        }

        window = new DefaultWindow("Buffer Pool");
        window.setWidth(480);
        window.setHeight(360);
        window.setWidget(addBufferPoolDialog);
        window.setGlassEnabled(true);
        window.center();
    }

    public void modifyBufferPool(String name, Map<String, Object> changedValues) {
        circuit.dispatch(new ModifyBufferPool(new ModifyPayload(name, changedValues)));
    }

    public void removeBufferPool(String name) {
        circuit.dispatch(new RemoveBufferPool(name));
    }
}
