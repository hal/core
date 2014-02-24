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
package org.jboss.as.console.client.shared.patching.wizard;

import static org.jboss.as.console.client.domain.model.impl.LifecycleOperation.STOP;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.domain.topology.LifecycleCallback;
import org.jboss.as.console.client.domain.topology.TopologyOp;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

/**
 * @author Harald Pehl
 */
class StopServersOp extends TopologyOp {

    private final DispatchAsync dispatcher;
    private final String host;
    private final List<String> servers;
    private final LifecycleCallback callback;

    StopServersOp(final DispatchAsync dispatcher, final String host, final List<String> servers,
            final LifecycleCallback callback) {
        super(STOP, callback);
        this.dispatcher = dispatcher;
        this.host = host;
        this.servers = servers;
        this.callback = callback;
    }

    @Override
    public void run() {
        final ModelNode comp = new ModelNode();
        comp.get(ADDRESS).setEmptyList();
        comp.get(OP).set(COMPOSITE);
        List<ModelNode> steps = new LinkedList<ModelNode>();

        for (String server : servers) {
            ModelNode stop = new ModelNode();
            stop.get(ADDRESS).add("host", host);
            stop.get(ADDRESS).add("server-config", server);
            stop.get(OP).set("stop");
            steps.add(stop);
        }
        comp.get(STEPS).set(steps);

        dispatcher.execute(new DMRAction(comp), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                callback.onError(caught);
            }

            @Override
            public void onSuccess(final DMRResponse result) {
                // No action here: We're polling until the servers are stopped, or the timeout is reached.
            }
        });
        new Async().whilst(new KeepGoing(), new Finish(), new QueryStatus(), 1000);
    }


    class QueryStatus implements Function<Object> {

        final ModelNode queryOp;

        QueryStatus() {
            queryOp = new ModelNode();
            queryOp.get(ADDRESS).setEmptyList();
            queryOp.get(OP).set(COMPOSITE);
            List<ModelNode> steps = new LinkedList<ModelNode>();

            for (String server : servers) {
                ModelNode query = new ModelNode();
                query.get(ADDRESS).add("host", host);
                query.get(ADDRESS).add("server-config", server);
                query.get(OP).set(READ_ATTRIBUTE_OPERATION);
                query.get(NAME).set("status");
                steps.add(query);
            }
            queryOp.get(STEPS).set(steps);
        }

        @Override
        public void execute(final Control<Object> control) {

            dispatcher.execute(new DMRAction(queryOp, false), new AsyncCallback<DMRResponse>() {
                @Override
                public void onFailure(Throwable caught) {
                    // ignore
                }

                @Override
                public void onSuccess(DMRResponse dmrResponse) {
                    boolean allStopped = true;
                    ModelNode response = dmrResponse.get();
                    if (!response.isFailure()) {
                        List<Property> steps = response.get(RESULT).asPropertyList();
                        for (Iterator<Property> iterator = steps.iterator(); iterator.hasNext() && allStopped; ) {
                            Property step = iterator.next();
                            ModelNode status = step.getValue().get(RESULT);
                            if (!"stopped".equalsIgnoreCase(status.asString())) {
                                allStopped = false;
                            }
                        }
                        lifecycleReached = allStopped;
                    }
                }
            });
        }
    }
}
