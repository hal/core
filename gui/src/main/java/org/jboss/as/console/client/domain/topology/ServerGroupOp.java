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
package org.jboss.as.console.client.domain.topology;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.domain.model.ServerGroupStore;
import org.jboss.as.console.client.domain.model.ServerInstance;
import org.jboss.as.console.client.domain.model.impl.LifecycleOperation;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
public class ServerGroupOp extends TopologyOp {

    private final DispatchAsync dispatcher;
    private final ServerGroupStore serverGroupStore;
    private final String group;
    private final ModelNode node;

    public ServerGroupOp(final LifecycleOperation op, final LifecycleCallback callback, final DispatchAsync dispatcher,
            final ServerGroupStore serverGroupStore, final String group,
            final Map<HostInfo, List<ServerInstance>> serversPerHost) {

        super(op, callback);

        this.dispatcher = dispatcher;
        this.serverGroupStore = serverGroupStore;
        this.group = group;

        this.node = new ModelNode();
        this.node.get(ADDRESS).setEmptyList();
        this.node.get(OP).set(COMPOSITE);
        List<ModelNode> steps = new LinkedList<ModelNode>();
        for (Map.Entry<HostInfo, List<ServerInstance>> entry : serversPerHost.entrySet()) {
            HostInfo hostInfo = entry.getKey();
            for (ServerInstance serverInstance : entry.getValue()) {
                ModelNode serverStateOp = new ModelNode();
                serverStateOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
                serverStateOp.get(NAME).set("status");
                serverStateOp.get(ADDRESS).add("host", hostInfo.getName());
                serverStateOp.get(ADDRESS).add("server-config", serverInstance.getName());
                steps.add(serverStateOp);
            }
        }
        this.node.get(STEPS).set(steps);
    }

    public void run() {
        BooleanCallback bc = new BooleanCallback();
        switch (op) {
            case START:
                serverGroupStore.startServerGroup(group, bc);
                break;
            case STOP:
                serverGroupStore.stopServerGroup(group, bc);
                break;
            case RESTART:
                serverGroupStore.restartServerGroup(group, bc);
                break;
            case KILL:
            case RELOAD:
                // not supported for server groups
                break;
        }
        new Async(Footer.PROGRESS_ELEMENT).whilst(new KeepGoing(), new Finish(), new QueryStatus(), 750);
    }

    class QueryStatus implements Function<Object> {

        @Override
        public void execute(final Control<Object> control) {
            dispatcher.execute(new DMRAction(node, false), new AsyncCallback<DMRResponse>() {
                @Override
                public void onFailure(Throwable caught) {
                    // ignore
                }

                @Override
                public void onSuccess(DMRResponse dmrResponse) {
                    ModelNode response = dmrResponse.get();
                    if (!response.isFailure()) {
                        List<Property> steps = response.get(RESULT).asPropertyList();
                        for (Property step : steps) {
                            ModelNode stepResult = step.getValue();
                            if (stepResult.get(RESULT).isDefined()) {
                                String status = stepResult.get(RESULT).asString();
                                switch (op) {
                                    case START:
                                    case RESTART:
                                        lifecycleReached = "started".equalsIgnoreCase(status);
                                        break;
                                    case STOP:
                                        lifecycleReached = "stopped".equalsIgnoreCase(status);
                                        break;
                                    case KILL:
                                    case RELOAD:
                                        // not supported for server groups
                                        break;
                                }
                                if (!lifecycleReached) {
                                    break;
                                }
                            }
                        }
                    }
                }
            });
        }
    }
}
