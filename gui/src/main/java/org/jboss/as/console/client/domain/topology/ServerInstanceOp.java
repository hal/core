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

import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.domain.model.HostInformationStore;
import org.jboss.as.console.client.domain.model.impl.LifecycleOperation;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
public class ServerInstanceOp extends TopologyOp {

    private final Map<String, Object> params;
    private final DispatchAsync dispatcher;
    private final HostInformationStore hostInfoStore;
    private final String host;
    private final String server;
    private final ModelNode node;

    public ServerInstanceOp(final LifecycleOperation op, Map<String, Object> params, final LifecycleCallback callback,
                            final DispatchAsync dispatcher, final HostInformationStore hostInfoStore, final String host,
                            final String server) {
        super(op, callback);
        this.params = params;

        this.dispatcher = dispatcher;
        this.hostInfoStore = hostInfoStore;
        this.host = host;
        this.server = server;

        this.node = new ModelNode();
        this.node.get(OP).set(READ_ATTRIBUTE_OPERATION);
        this.node.get(ADDRESS).setEmptyList();
        this.node.get(ADDRESS).add("host", host);
        this.node.get(ADDRESS).add("server-config", server);
        this.node.get(NAME).set("status");
    }

    public void run() {
        BooleanCallback bc = new BooleanCallback();
        switch (op) {
            case START:
                hostInfoStore.startServer(host, server, true, bc);
                break;
            case STOP:
                hostInfoStore.startServer(host, server, false, bc);
                break;
            case KILL:
                hostInfoStore.killServer(host, server, false, bc);
                break;
            case RELOAD:
                hostInfoStore.reloadServer(host, server, bc);
                break;
            case RESTART:
                hostInfoStore.restartServer(host, server, bc);
                break;
            case SUSPEND:
                hostInfoStore.suspendServer(host, server, params, bc);
                break;
            case RESUME:
                hostInfoStore.resumeServer(host, server, bc);
                break;
        }
        new Async(Footer.PROGRESS_ELEMENT).whilst(new KeepGoing(), new Finish(), new QueryStatus(), 1000);
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
                        String status = response.get(RESULT).asString();
                        switch (op) {
                            case START:
                            case RELOAD:
                                lifecycleReached = "started".equalsIgnoreCase(status);
                                break;
                            case STOP:
                            case KILL:
                                lifecycleReached = "stopped".equalsIgnoreCase(status);
                                break;
                            case RESTART:
                                lifecycleReached = "started".equalsIgnoreCase(status);
                                break;
                            case SUSPEND:
                            case RESUME:
                                lifecycleReached = true;
                                break;
                        }
                    }
                }
            });
        }
    }
}
