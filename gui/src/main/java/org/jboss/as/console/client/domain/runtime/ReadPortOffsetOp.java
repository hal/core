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
package org.jboss.as.console.client.domain.runtime;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import elemental.client.Browser;
import elemental.dom.Element;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.NAME;
import static org.jboss.dmr.client.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.dmr.client.ModelDescriptionConstants.RESULT;

/**
 * @author Harald Pehl
 */
class ReadPortOffsetOp {

    private final DispatchAsync dispatcher;

    ReadPortOffsetOp(final DispatchAsync dispatcher) {
        this.dispatcher = dispatcher;
    }

    void execute(final Server server, final String id) {
        ResourceAddress address = new ResourceAddress()
                .add("host", server.getHostName())
                .add("server", server.getName())
                .add("socket-binding-group", "*");
        Operation operation = new Operation.Builder(READ_ATTRIBUTE_OPERATION, address)
                .param(NAME, "port-offset")
                .build();

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable throwable) {
                feedback("n/a");
            }

            @Override
            public void onSuccess(final DMRResponse dmrResponse) {
                if (dmrResponse.get().isFailure()) {
                    feedback("n/a");
                } else {
                    List<ModelNode> nodes = dmrResponse.get().get(RESULT).asList();
                    if (!nodes.isEmpty() && !nodes.get(0).isFailure() && nodes.get(0).hasDefined(RESULT)) {
                        feedback(nodes.get(0).get(RESULT).asString());
                    } else {
                        feedback("n/a");
                    }
                }
            }

            private void feedback(final String portOffset) {
                Element element = Browser.getDocument().getElementById(id);
                if (element != null) {
                    Scheduler.get().scheduleDeferred(() -> element.setTextContent(portOffset));
                }
            }
        });
    }
}
