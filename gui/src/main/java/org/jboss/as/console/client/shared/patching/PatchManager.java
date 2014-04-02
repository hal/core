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

import java.util.LinkedList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.state.DomainEntityManager;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

/**
 * @author Harald Pehl
 */
public class PatchManager {

    private final DispatchAsync dispatcher;
    private final BootstrapContext bootstrapContext;
    private final DomainEntityManager domainManager;
    private final BeanFactory beanFactory;

    @Inject
    public PatchManager(final DispatchAsync dispatcher, final BootstrapContext bootstrapContext,
            final DomainEntityManager domainManager, BeanFactory beanFactory) {
        this.dispatcher = dispatcher;
        this.bootstrapContext = bootstrapContext;
        this.domainManager = domainManager;
        this.beanFactory = beanFactory;
    }

    public void getPatches(final AsyncCallback<Patches> callback) {
        ModelNode comp = new ModelNode();
        comp.get(ADDRESS).setEmptyList();
        comp.get(OP).set(COMPOSITE);
        List<ModelNode> steps = new LinkedList<ModelNode>();

        ModelNode historyOp = baseAddress();
        historyOp.get(OP).set("show-history");
        steps.add(historyOp);

        ModelNode latestPatchOp = baseAddress();
        latestPatchOp.get(OP).set(READ_RESOURCE_OPERATION);
        latestPatchOp.get(INCLUDE_RUNTIME).set(true);
        steps.add(latestPatchOp);

        comp.get(STEPS).set(steps);
        dispatcher.execute(new DMRAction(comp), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                Console.warning(Console.CONSTANTS.patch_manager_error(), caught.getMessage());
                callback.onSuccess(new Patches());
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                Patches patches = new Patches();
                ModelNode result = response.get();
                if (!result.hasDefined(OUTCOME) || result.isFailure()) {
                    Console.error(Console.CONSTANTS.patch_manager_error(), result.getFailureDescription());
                } else {
                    ModelNode stepsResult = result.get(RESULT);
                    ModelNode historyStep = stepsResult.get("step-1");
                    ModelNode historyNode = historyStep.get(RESULT);
                    if (historyNode.isDefined()) {
                        for (ModelNode node : historyNode.asList()) {
                            patches.add(historyToPatchInfo(node));
                        }
                    }

                    ModelNode latestPatchStep = stepsResult.get("step-2");
                    ModelNode latestPatchNode = latestPatchStep.get(RESULT);
                    if (latestPatchNode.isDefined()) {
                        String id = null;
                        ModelNode patchesNode = latestPatchNode.get("patches");
                        if (patchesNode.isDefined()) {
                            List<ModelNode> idList = patchesNode.asList();
                            if (!idList.isEmpty()) {
                                id = idList.get(0).asString(); // TODO first == latest?
                            }
                        }
                        if (id == null) {
                            id = latestPatchNode.get("cumulative-patch-id").asString();
                        }
                        patches.setLatest(id);

                        String version = latestPatchNode.get("version").asString();
                        if (patches.getLatest() != null) {
                            patches.getLatest().setVersion(version);
                        }
                    }

                    ModelNode headersNode = result.get("response-headers");
                    if (headersNode.isDefined()) {
                        ModelNode stateNode = headersNode.get("process-state");
                        patches.setRestartRequired("restart-required".equals(stateNode.asString()));
                    } else {
                        patches.setRestartRequired(false);
                    }
                }
                callback.onSuccess(patches);
            }
        });
    }

    public void rollback(final PatchInfo patchInfo, final boolean resetConfiguration, final boolean overrideAll,
            final AsyncCallback<Void> callback) {
        ModelNode rollbackOp = baseAddress();
        rollbackOp.get(OP).set("rollback");
        rollbackOp.get("patch-id").set(patchInfo.getId());
        rollbackOp.get("reset-configuration").set(resetConfiguration);
        rollbackOp.get("override-all").set(overrideAll);

        dispatcher.execute(new DMRAction(rollbackOp), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (!result.hasDefined(OUTCOME) || result.isFailure()) {
                    callback.onFailure(new RuntimeException(result.getFailureDescription()));
                } else {
                    callback.onSuccess(null);
                }
            }
        });
    }

    private PatchInfo historyToPatchInfo(final ModelNode node) {
        PatchInfo patchInfo = beanFactory.patchInfo().as();
        patchInfo.setId(node.get("patch-id").asString());
        patchInfo.setType(PatchType.fromLabel(node.get("type").asString()));
        patchInfo.setAppliedAt(node.get("applied-at").asString());
        return patchInfo;
    }

    public ModelNode baseAddress() {
        ModelNode node = new ModelNode();
        if (!bootstrapContext.isStandalone()) {
            node.get(ADDRESS).add("host", domainManager.getSelectedHost());
        }
        node.get(ADDRESS).add("core-service", "patching");
        return node;
    }
}
