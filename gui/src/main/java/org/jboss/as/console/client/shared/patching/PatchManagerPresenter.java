/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.console.client.shared.patching;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Callback;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.flow.TimeoutOperation;
import org.jboss.as.console.client.shared.patching.ui.RestartModal;
import org.jboss.as.console.client.shared.patching.wizard.apply.ApplyContext;
import org.jboss.as.console.client.shared.patching.wizard.apply.ApplyWizard;
import org.jboss.as.console.client.shared.state.DomainEntityManager;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

/**
 * @author Harald Pehl
 */
public class PatchManagerPresenter extends Presenter<PatchManagerPresenter.MyView, PatchManagerPresenter.MyProxy> {

    @ProxyCodeSplit
    @NameToken(NameTokens.PatchingPresenter)
    @AccessControl(resources = {"/{selected.host}/core-service=patching"}, recursive = false)
    public interface MyProxy extends Proxy<PatchManagerPresenter>, Place {}


    public interface MyView extends View {

        void setPresenter(PatchManagerPresenter presenter);

        void update(Patches patches);
    }

    static final int NORMAL_WINDOW_HEIGHT = 400;
    static final int BIGGER_WINDOW_HEIGHT = NORMAL_WINDOW_HEIGHT + 100;

    private final RevealStrategy revealStrategy;
    private final PatchManager patchManager;
    private final DomainEntityManager domainManager;
    private final BootstrapContext bootstrapContext;
    private final DispatchAsync dispatcher;
    private DefaultWindow window;

    @Inject
    public PatchManagerPresenter(final EventBus eventBus, final MyView view, final MyProxy proxy,
            RevealStrategy revealStrategy, PatchManager patchManager, final DomainEntityManager domainManager,
            BootstrapContext bootstrapContext, DispatchAsync dispatcher) {

        super(eventBus, view, proxy);
        this.revealStrategy = revealStrategy;
        this.patchManager = patchManager;
        this.domainManager = domainManager;
        this.bootstrapContext = bootstrapContext;
        this.dispatcher = dispatcher;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        loadPatches();
    }

    public void loadPatches() {
        patchManager.getPatches(new SimpleCallback<Patches>() {
            @Override
            public void onSuccess(final Patches patches) {
                getView().update(patches);
            }
        });
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInRuntimeParent(this);
    }

    public void launchApplyPatchWizard() {
        // this callback is directly called from the standalone branch
        // or after the running server instances are retrieved in the domain branch
        final Callback<ApplyContext, Throwable> contextCallback = new Callback<ApplyContext, Throwable>() {
            @Override
            public void onFailure(final Throwable caught) {
                Log.error("Unable to launch apply patch wizard", caught);
                Console.error("Unable to launch apply patch wizard", caught.getMessage());
            }

            @Override
            public void onSuccess(final ApplyContext context) {
                window = new DefaultWindow(Console.CONSTANTS.patch_manager_apply_new());
                window.setWidth(480);
                window.setHeight(NORMAL_WINDOW_HEIGHT);
                window.setWidget(new ApplyWizard(PatchManagerPresenter.this, context, dispatcher, patchManager));
                window.setGlassEnabled(true);
                window.center();
            }
        };

        if (bootstrapContext.isStandalone()) {
            contextCallback
                    .onSuccess(new ApplyContext(true, null, Collections.<String>emptyList(), patchManager.baseAddress(),
                            bootstrapContext.getProperty(
                            BootstrapContext.PATCH_API)));
        } else {
            final String host = domainManager.getSelectedHost();
            ModelNode operation = new ModelNode();
            operation.get(ADDRESS).add("host", host);
            operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
            operation.get(CHILD_TYPE).set("server");
            operation.get(INCLUDE_RUNTIME).set(true);
            dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
                @Override
                public void onSuccess(DMRResponse result) {
                    ModelNode response = result.get();
                    List<String> runningServers = new LinkedList<String>();
                    if (response.isFailure()) {
                        // no servers
                        contextCallback.onSuccess(new ApplyContext(false, host, runningServers,
                                patchManager.baseAddress(), bootstrapContext.getProperty(BootstrapContext.PATCH_API)));
                    } else {
                        List<Property> servers = response.get(RESULT).asPropertyList();
                        for (Property server : servers) {
                            String name = server.getName();
                            ModelNode instance = server.getValue();
                            String state = instance.get("server-state").asString();
                            if ("running".equals(state)) {
                                runningServers.add(name);
                            }
                        }
                        contextCallback.onSuccess(new ApplyContext(false, host, runningServers,
                                patchManager.baseAddress(), bootstrapContext.getProperty(BootstrapContext.PATCH_API)));
                    }
                }

                @Override
                public void onFailure(Throwable caught) {
                    contextCallback.onFailure(caught);
                }
            });
        }
    }

    public void hideWindow() {
        if (window != null) {
            window.hide();
        }
    }

    public void biggerWindow() {
        if (window != null) {
            window.setHeight(BIGGER_WINDOW_HEIGHT);
            window.center();
        }
    }

    public void normalWindow() {
        if (window != null) {
            window.setHeight(NORMAL_WINDOW_HEIGHT);
            window.center();
        }
    }

    public void onRollback(final PatchInfo patchInfo) {
        patchManager.rollback(patchInfo);
    }

    public void restart() {
        ModelNode restartNode = new ModelNode();
        if (!bootstrapContext.isStandalone()) {
            restartNode.get(ADDRESS).add("host", domainManager.getSelectedHost());
        }
        restartNode.get(OP).set(SHUTDOWN);
        restartNode.get("restart").set(true);

        final RestartModal restartModal = new RestartModal();
        restartModal.center();

        RestartOp restartOp = new RestartOp(dispatcher);
        restartOp.start(dispatcher, restartNode, new TimeoutOperation.Callback() {
            @Override
            public void onSuccess() {
                // TODO Doesn't need a full reload if a non-dc host was patched
                Window.Location.reload();
            }

            @Override
            public void onTimeout() {
                // TODO Is there another way out?
                restartModal.timeout();
            }

            @Override
            public void onError(final Throwable caught) {
                // TODO Is there another way out?
                restartModal.error();
            }
        });
    }
}
