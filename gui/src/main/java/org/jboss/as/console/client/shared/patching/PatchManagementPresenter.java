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

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Callback;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.*;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.flow.TimeoutOperation;
import org.jboss.as.console.client.shared.patching.ui.RestartModal;
import org.jboss.as.console.client.shared.patching.wizard.apply.ApplyContext;
import org.jboss.as.console.client.shared.patching.wizard.apply.ApplyWizard;
import org.jboss.as.console.client.shared.patching.wizard.rollback.RollbackContext;
import org.jboss.as.console.client.shared.patching.wizard.rollback.RollbackWizard;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.v3.presenter.Finder;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.dmr.client.dispatch.impl.UploadHandler;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
public class PatchManagementPresenter
        extends CircuitPresenter<PatchManagementPresenter.MyView, PatchManagementPresenter.MyProxy>
        implements Finder {

    @ProxyCodeSplit
    @NameToken(NameTokens.PatchingPresenter)
    @SearchIndex(keywords = {"patching", "update", "upgrade"})
    @AccessControl(resources = {"/{implicit.host}", "/{implicit.host}/core-service=patching"}, recursive = false)
    public interface MyProxy extends Proxy<PatchManagementPresenter>, Place {
    }


    public interface MyView extends View, HasPresenter<PatchManagementPresenter>, Updateable<List<Patches>> {
        void update(List<Patches> patches);
    }


    private abstract class GetRunningServersCallback implements AsyncCallback<DMRResponse> {
        @Override
        public void onSuccess(DMRResponse result) {
            result.setIgnoreRestartHeader(true);
            ModelNode response = result.get();
            List<String> runningServers = new LinkedList<String>();
            if (!response.isFailure()) {
                List<Property> servers = response.get(RESULT).asPropertyList();
                for (Property server : servers) {
                    String name = server.getName();
                    ModelNode instance = server.getValue();
                    String state = instance.get("server-state").asString();
                    if ("running".equals(state)) {
                        runningServers.add(name);
                    }
                }
            }
            onServers(runningServers);
        }

        @Override
        public void onFailure(final Throwable caught) {
            onServers(Collections.<String>emptyList());
        }

        protected abstract void onServers(List<String> runningServers);
    }


    static final int NORMAL_WINDOW_HEIGHT = 400;
    static final int BIGGER_WINDOW_HEIGHT = NORMAL_WINDOW_HEIGHT + 100;

    private final RevealStrategy revealStrategy;
    private final PatchManager patchManager;
    private final HostStore hostStore;
    private final BootstrapContext bootstrapContext;
    private final DispatchAsync dispatcher;
    private DefaultWindow window;


    @Inject
    public PatchManagementPresenter(final EventBus eventBus, final MyView view, final MyProxy proxy,
                                    final RevealStrategy revealStrategy, final PatchManager patchManager,
                                    final Dispatcher circuit, final HostStore hostStore,
                                    final BootstrapContext bootstrapContext, final DispatchAsync dispatcher) {

        super(eventBus, view, proxy, circuit);
        this.revealStrategy = revealStrategy;
        this.patchManager = patchManager;
        this.hostStore = hostStore;
        this.bootstrapContext = bootstrapContext;
        this.dispatcher = dispatcher;

    }

    @Override
    public FinderColumn.FinderId getFinderId() {
        return FinderColumn.FinderId.PATCHING;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        addChangeHandler(hostStore);
    }

    @Override
    protected void onAction(Action action) {
        // noop
    }

    private PlaceRequest hostPlaceRequest() {
        return new PlaceRequest.Builder().nameToken(getProxy().getNameToken())
                .with("host", hostStore.getSelectedHost()).build();
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        loadPatches();
        Console.MODULES.getHeader().highlight(getProxy().getNameToken());
    }

    public void loadPatches() {
        if (bootstrapContext.isStandalone()) {
            patchManager.getStandalonePatches(new SimpleCallback<Patches>() {
                @Override
                public void onSuccess(final Patches patches) {
                    List<Patches> helper = new LinkedList<>();
                    helper.add(patches);
                    getView().update(helper);
                }
            });
        } else {
            patchManager.getDomainPatches(new SimpleCallback<List<Patches>>() {
                @Override
                public void onSuccess(List<Patches> patches) {
                    getView().update(patches);
                }
            });
        }
    }

    public void launchApplyWizard() {
        if (!UploadHandler.verifySupport()) {
            Console.warning(Console.CONSTANTS.uploadsNotSupported(), Console.CONSTANTS.noUploadDueToSecurityReasons());
        }

        // this callback is directly called from the standalone branch
        // or after the running server instances are retrieved in the domain branch
        final Callback<ApplyContext, Throwable> contextCallback = new Callback<ApplyContext, Throwable>() {
            @Override
            public void onFailure(final Throwable caught) {
                Log.error("Unable to launch apply patch wizard", caught);
                Console.error(Console.CONSTANTS.patch_manager_apply_new_wizard_error(), caught.getMessage());
            }

            @Override
            public void onSuccess(final ApplyContext context) {
                window = new DefaultWindow(Console.CONSTANTS.patch_manager_apply_patch());
                window.setWidth(480);
                window.setHeight(NORMAL_WINDOW_HEIGHT);
                window.trapWidget(new ApplyWizard(PatchManagementPresenter.this, context,
                        Console.CONSTANTS.patch_manager_apply_patch(), dispatcher, patchManager).asWidget());
                window.setGlassEnabled(true);
                window.center();
            }
        };

        if (bootstrapContext.isStandalone()) {
            contextCallback
                    .onSuccess(new ApplyContext(true, Patches.STANDALONE_HOST, Collections.<String>emptyList(),
                            patchManager.baseAddress(), bootstrapContext.getProperty(BootstrapContext.UPLOAD_API)));
        } else {
            final String host = hostStore.getSelectedHost();
            dispatcher
                    .execute(new DMRAction(getRunningServersOp(host)), new GetRunningServersCallback() {
                        @Override
                        protected void onServers(final List<String> runningServers) {
                            contextCallback.onSuccess(new ApplyContext(false, host, runningServers,
                                    patchManager.baseAddress(),
                                    bootstrapContext.getProperty(BootstrapContext.UPLOAD_API)));
                        }
                    });
        }
    }

    public void launchRollbackWizard(final PatchInfo patchInfo) {
        // this callback is directly called from the standalone branch
        // or after the running server instances are retrieved in the domain branch
        final Callback<RollbackContext, Throwable> contextCallback = new Callback<RollbackContext, Throwable>() {
            @Override
            public void onFailure(final Throwable caught) {
                Log.error("Unable to launch apply patch wizard", caught);
                Console.error(Console.CONSTANTS.patch_manager_rollback_wizard_error(), caught.getMessage());
            }

            @Override
            public void onSuccess(final RollbackContext context) {
                window = new DefaultWindow(Console.CONSTANTS.patch_manager_rollback());
                window.setWidth(480);
                window.setHeight(NORMAL_WINDOW_HEIGHT);
                window.setWidget(new RollbackWizard(PatchManagementPresenter.this, context,
                        Console.CONSTANTS.patch_manager_rollback(), dispatcher, patchManager));
                window.setGlassEnabled(true);
                window.center();
            }
        };

        if (bootstrapContext.isStandalone()) {
            contextCallback
                    .onSuccess(new RollbackContext(true, Patches.STANDALONE_HOST, Collections.<String>emptyList(),
                            patchManager.baseAddress(), patchInfo));
        } else {
            final String host = hostStore.getSelectedHost();
            dispatcher
                    .execute(new DMRAction(getRunningServersOp(host)), new GetRunningServersCallback() {
                        @Override
                        protected void onServers(final List<String> runningServers) {
                            contextCallback.onSuccess(new RollbackContext(false, host, runningServers,
                                    patchManager.baseAddress(), patchInfo));
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
        if (window != null && window.isAttached()) {
            window.setHeight(BIGGER_WINDOW_HEIGHT);
            window.center();
        }
    }

    public void normalWindow() {
        if (window != null && window.isAttached()) {
            window.setHeight(NORMAL_WINDOW_HEIGHT);
            window.center();
        }
    }

    public void restart() {
        ModelNode restartNode = new ModelNode();
        if (!bootstrapContext.isStandalone()) {
            restartNode.get(ADDRESS).add("host", hostStore.getSelectedHost());
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

    private ModelNode getRunningServersOp(final String host) {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).add("host", host);
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(CHILD_TYPE).set("server");
        operation.get(INCLUDE_RUNTIME).set(true);
        return operation;
    }
}
