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
package org.jboss.as.console.client.shared.subsys.batch;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import org.jboss.as.console.client.core.CircuitPresenter;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.batch.store.*;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.widgets.AddResourceDialog;
import org.jboss.as.console.mbui.widgets.AddressableResourceView;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.Map;

import static org.jboss.as.console.client.shared.subsys.batch.store.BatchStore.*;

/**
 * @author Harald Pehl
 */
public class BatchPresenter extends CircuitPresenter<BatchPresenter.MyView, BatchPresenter.MyProxy> {

    public interface MyView extends View, AddressableResourceView, HasPresenter<BatchPresenter> {
    }


    @ProxyCodeSplit
    @NameToken(NameTokens.Batch)
    @RequiredResources(resources = "{selected.profile}/subsystem=batch")
    public interface MyProxy extends ProxyPlace<BatchPresenter> {
    }


    private final Dispatcher circuit;
    private final RevealStrategy revealStrategy;
    private final SecurityFramework securityFramework;
    private final BatchStore batchStore;
    private final ResourceAddress batchTemplate;
    private final ResourceAddress threadPoolTemplate;
    private final ResourceAddress jobRepositoryTemplate;
    private final ResourceAddress threadFactoriesTemplate;

    private DefaultWindow window;
    private AddResourceDialog addThreadFactoryDialog;


    @Inject
    public BatchPresenter(EventBus eventBus, MyView view, MyProxy proxy, Dispatcher circuit,
                          RevealStrategy revealStrategy, SecurityFramework securityFramework,
                          BatchStore batchStore) {
        super(eventBus, view, proxy, circuit);

        this.circuit = circuit;
        this.revealStrategy = revealStrategy;
        this.securityFramework = securityFramework;
        this.batchStore = batchStore;

        this.batchTemplate = new ResourceAddress(BATCH_ADDRESS, batchStore.getStatementContext());
        this.threadPoolTemplate = new ResourceAddress(THREAD_POOL_ADDRESS, batchStore.getStatementContext());
        this.jobRepositoryTemplate = new ResourceAddress(JOB_REPOSITORY_ADDRESS, batchStore.getStatementContext());
        this.threadFactoriesTemplate = new ResourceAddress(THREAD_FACTORIES_ADDRESS, batchStore.getStatementContext());
    }


    // ------------------------------------------------------ lifecycle

    @Override
    protected void onBind() {
        super.onBind();
        addChangeHandler(batchStore);
        getView().setPresenter(this);
    }

    @Override
    protected void onAction(Action action) {
        if (action instanceof InitBatch) {
            getView().update(batchTemplate, batchStore.getBatch());
            getView().update(threadPoolTemplate, batchStore.getThreadPool());
            getView().update(jobRepositoryTemplate, batchStore.getJobRepository());
            getView().update(threadFactoriesTemplate, batchStore.getThreadFactories());
        }

        else if (action instanceof ModifyBatch) {
            getView().update(batchTemplate, batchStore.getBatch());
        }

        else if (action instanceof ModifyThreadPool) {
            getView().update(threadPoolTemplate, batchStore.getThreadPool());
        }

        else if (action instanceof ModifyJobRepository) {
            getView().update(jobRepositoryTemplate, batchStore.getThreadPool());
        }

        else if (action instanceof AddThreadFactory || action instanceof ModifyThreadFactory) {
            getView().update(threadFactoriesTemplate, batchStore.getThreadFactories());
            getView().select(threadFactoriesTemplate, batchStore.getLastModifiedThreadFactory());
        }

        else if (action instanceof RefreshThreadFactories || action instanceof RemoveThreadFactory) {
            getView().update(threadFactoriesTemplate, batchStore.getThreadFactories());
        }
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        circuit.dispatch(new InitBatch());
    }


    // ------------------------------------------------------ business methods

    public void modifyBatch(Map<String, Object> changedValues) {
        circuit.dispatch(new ModifyBatch(changedValues));
    }

    public void modifyJobRepository(Map<String, Object> changedValues) {
        circuit.dispatch(new ModifyJobRepository(changedValues));
    }

    public void modifyThreadPool(Map<String, Object> changedValues) {
        circuit.dispatch(new ModifyThreadPool(changedValues));
    }

    public void launchAddThreadFactory() {
        if (addThreadFactoryDialog == null) {
            addThreadFactoryDialog = new AddResourceDialog(BatchStore.THREAD_FACTORIES_ADDRESS,
                    batchStore.getStatementContext(), securityFramework.getSecurityContext(),
                    new AddResourceDialog.Callback() {
                        @Override
                        public void onAddResource(ResourceAddress address, ModelNode payload) {
                            window.hide();
                            circuit.dispatch(new AddThreadFactory(payload));
                        }

                        @Override
                        public void closeDialogue() {
                            window.hide();
                        }
                    }
            );
        } else {
            addThreadFactoryDialog.clearValues();
        }

        window = new DefaultWindow("Worker");
        window.setWidth(480);
        window.setHeight(360);
        window.setWidget(addThreadFactoryDialog);
        window.setGlassEnabled(true);
        window.center();
    }

    public void modifyThreadFactory(String name, Map<String, Object> changedValues) {
        circuit.dispatch(new ModifyThreadFactory(name, changedValues));
    }

    public void removeThreadFactory(String name) {
        circuit.dispatch(new RemoveThreadFactory(name));
    }
}
