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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.inject.Inject;
import org.apache.xpath.operations.Bool;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.dmr.client.dispatch.impl.UploadAction;
import org.jboss.dmr.client.dispatch.impl.UploadResponse;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;
import org.jboss.gwt.flow.client.Progress;
import org.jboss.dmr.client.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.jboss.as.console.client.shared.patching.Patches.STANDALONE_HOST;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

// TODO (hpehl) Use Circuit: Turn this into a PatchStore with a dependency to the HostStore
public class PatchManager {

    private final DispatchAsync dispatcher;
    private final boolean standalone;
    private final HostStore hostStore;
    private final BeanFactory beanFactory;

    @Inject
    public PatchManager(final DispatchAsync dispatcher, final BootstrapContext bootstrapContext,
                        final HostStore hostStore, BeanFactory beanFactory) {
        this.dispatcher = dispatcher;
        this.standalone = bootstrapContext.isStandalone();
        this.hostStore = hostStore;
        this.beanFactory = beanFactory;
    }

    // TODO (hpehl) Check performance for big topologies
    public void getDomainPatches(final AsyncCallback<List<Patches>> callback) {
        final List<Patches> patches = new ArrayList<>();
        final List<Function<List<Patches>>> functions = new ArrayList<>();
        for (String host : hostStore.getHostNames()) {
            functions.add(new ReadPatches(host));
        }

        //noinspection unchecked
        new Async<List<Patches>>().parallel(patches, new Outcome<List<Patches>>() {
            @Override
            public void onFailure(List<Patches> context) {
                callback.onSuccess(context);
            }

            @Override
            public void onSuccess(List<Patches> context) {
                callback.onSuccess(context);

            }
        }, functions.toArray(new Function[functions.size()]));
    }

    public void getStandalonePatches(final AsyncCallback<Patches> callback) {
        getPatchOfHost(STANDALONE_HOST, callback);
    }

    public void getPatchOfHost(final String host, final AsyncCallback<Patches> callback) {
        final List<Patches> patches = new ArrayList<>();
        ReadPatches fn = new ReadPatches(host);

        new Async<List<Patches>>(new Progress.Nop()).parallel(patches, new Outcome<List<Patches>>() {
            @Override
            public void onFailure(List<Patches> context) {
                returnPatches(context);
            }

            @Override
            public void onSuccess(List<Patches> context) {
                returnPatches(context);
            }

            private void returnPatches(List<Patches> context) {
                callback.onSuccess(context.isEmpty() ? new Patches(host) : context.get(0));
            }
        }, fn);
    }

    public void getPatchInfo(final PatchInfo patch, final AsyncCallback<PatchInfo> callback) {
        ModelNode patchInfoOp = baseAddressForStream(getHost(), patch.getIdentityName());
        patchInfoOp.get(OP).set("patch-info");
        patchInfoOp.get("patch-id").set(patch.getId());

        dispatcher.execute(new DMRAction(patchInfoOp), new AsyncCallback<DMRResponse>() {
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
                    ModelNode payload = result.get(RESULT);
                    patch.setIdentityName(payload.get("identity-name").asString());
                    patch.setIdentityVersion(payload.get("identity-version").asString());
                    patch.setDescription(payload.get("description").asString());
                    if (payload.get("link").isDefined()) {
                        patch.setLink(payload.get("link").asString());
                    } else {
                        patch.setLink("");
                    }
                    callback.onSuccess(patch);
                }
            }
        });
    }

    public void upload(FileUpload fileUpload, boolean overrideConflict,
            AsyncCallback<UploadResponse> callback) {
        ModelNode patchOp = baseAddress();
        patchOp.get(OP).set("patch");
        patchOp.get("content").add().get("input-stream-index").set(0);
        if (overrideConflict) {
            patchOp.get("override-all").set(true);
        }
        dispatcher.execute(new UploadAction(fileUpload.getElement(), patchOp), callback);
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

    private PatchInfo historyToPatchInfo(final String identity, final ModelNode node, Boolean inEffect) {
        PatchInfo patchInfo = beanFactory.patchInfo().as();
        patchInfo.setId(node.get("patch-id").asString());
        patchInfo.setType(node.get("type").asString());
        patchInfo.setAppliedAt(node.get("applied-at").asString());
        patchInfo.setIdentityName(identity);
        patchInfo.setIdentityVersion("");
        patchInfo.setDescription("");
        patchInfo.setLink("");
        patchInfo.setInEffect(inEffect);
        return patchInfo;
    }

    String getHost(){
    	return standalone ? STANDALONE_HOST : hostStore.getSelectedHost();
    }
    
    ModelNode baseAddress() {
        return baseAddress(getHost());
    }

    ModelNode baseAddress(String host) {
        ModelNode node = new ModelNode();
        if (!host.equals(STANDALONE_HOST)) {
            node.get(ADDRESS).add("host", host);
        }
        node.get(ADDRESS).add("core-service", "patching");
        return node;
    }
    
    ModelNode baseAddressForStream(final String host, final String patchStream) {
        ModelNode node = new ModelNode();
        if (!host.equals(STANDALONE_HOST)) {
            node.get(ADDRESS).add("host", host);
        }
        node.get(ADDRESS).add("core-service", "patching").add("patch-stream",patchStream);
        return node;
    }

    private class ReadPatches implements Function<List<Patches>> {

        private final String host;

        private ReadPatches(final String host) {
            this.host = host;
        }

        @Override
        public void execute(final Control<List<Patches>> control) {
         // read: patch-streams and latest patch,
         //	NOTE: latest patch == applied to product, it does not include
         // other streams!
         final ModelNode readResourceOperation = baseAddress(host);
         readResourceOperation.get(OP).set(READ_RESOURCE_OPERATION);
         readResourceOperation.get(INCLUDE_RUNTIME).set(true);
         readResourceOperation.get(RECURSIVE).set(true);
         dispatcher.execute(new DMRAction(readResourceOperation), new AsyncCallback<DMRResponse>() {

            public void onFailure(Throwable caught) {
                Console.warning(Console.CONSTANTS.patch_manager_error(), caught.getMessage());
                addPatches(control, new Patches(host));
                control.proceed();
            }

            public void onSuccess(DMRResponse result) {
                final ModelNode readResult = result.get().get(RESULT);
                final Patches patches = new Patches(host);
                //extract latest patch
                final ModelNode patchesNode = readResult.get("patches");

                String idPatchesNodes = null;
                if (patchesNode.isDefined()) {
                    List<ModelNode> idList = patchesNode.asList();
                    if (!idList.isEmpty()) {
                    	idPatchesNodes = idList.get(0).asString(); // TODO first == latest?
                    }
                }

                final String id = (idPatchesNodes != null) ? idPatchesNodes : readResult.get("cumulative-patch-id").asString();
                final String version = readResult.get("version").asString();

                ModelNode headersNode = readResult.get("response-headers");
                if (headersNode.isDefined()) {
                   ModelNode stateNode = headersNode.get("process-state");
                   patches.setRestartRequired("restart-required".equals(stateNode.asString()));
                } else {
                   patches.setRestartRequired(false);
                }

                //read patch streams and issue comp command to read
                //patch list per stream.
                final ModelNode patchStreams = readResult.get("patch-stream");
                final String[] patchStreamIDs = sort(patchStreams.asPropertyList());
                final ModelNode comp = new ModelNode();
                comp.get(ADDRESS).setEmptyList();
                comp.get(OP).set(COMPOSITE);
                List<ModelNode> steps = new LinkedList<ModelNode>();
                for(String stream:patchStreamIDs){
                    final ModelNode steppedOp = baseAddressForStream(host,stream);
                    steppedOp.get(OP).set("show-history");
                    steppedOp.get("include-runtime").set(true);
                    steps.add(steppedOp);
                }
                comp.get(STEPS).set(steps);
                dispatcher.execute(new DMRAction(comp), new AsyncCallback<DMRResponse>() {

                    public void onFailure(Throwable caught) {
                        Console.warning(Console.CONSTANTS.patch_manager_error(), caught.getMessage());
                        addPatches(control, new Patches(host));
                        control.proceed();
                    }

                    public void onSuccess(DMRResponse result) {
                        final ModelNode compResult = result.get().get(RESULT);
                        for (int index = 0; index < patchStreamIDs.length; index++) {
                            String streamId = patchStreamIDs[index];
                            ModelNode patchStream = patchStreams.get(streamId);
                            String cumulativePatchId = patchStream.get("cumulative-patch-id").asString();
                            Set<String> patchesInEffect = stringListToSet(patchStream.get("patches").asList());
                            List<ModelNode> streamPatches = compResult.get("step-"+(index+1)).get(RESULT).asList();
                            for (ModelNode patch: streamPatches) {
                                String patchId = patch.get("patch-id").asString();
                                boolean inEffect = patchesInEffect.contains(patchId) || patchId.equals(cumulativePatchId);
                                patches.add(historyToPatchInfo(streamId, patch, inEffect));
                            }
                        }
                        // this should be done here because setLatestId is checked against the registered patches.
                        patches.setLatest(id);
                        if (patches.getLatest() != null) {
                            patches.getLatest().setVersion(version);
                        }
                        addPatches(control, patches);
                        control.proceed();
                    }
                    
                });
            }
             
         });
        }

        private void addPatches(Control<List<Patches>> control, Patches patches) {
            control.getContext().add(patches);
        }

        private String[] sort(List<Property> list){
            String[] streams = new String[list.size()];
            for(int i=0; i<list.size();i++){
                streams[i] = list.get(i).getName();
            }
            Arrays.sort(streams);
            return streams;
        }

        private Set<String> stringListToSet(List<ModelNode> list) {
            final Set<String> set  = new HashSet<>();
            for (ModelNode node: list) {
                set.add(node.asString());
            }
            return set;
        }
    }
}
