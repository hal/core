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
package org.jboss.as.console.client.shared.subsys.io.bufferpool;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.io.IOStore;
import org.jboss.as.console.client.shared.subsys.io.ModifyPayload;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Harald Pehl
 */
@Store
public class BufferPoolStore extends IOStore {

    private static final String RESOURCE_NAME = "buffer-pool";
    private final DispatchAsync dispatcher;
    private final List<Property> bufferPools;

    @Inject
    public BufferPoolStore(DispatchAsync dispatcher, Baseadress baseAddress) {
        super(baseAddress, RESOURCE_NAME);
        this.dispatcher = dispatcher;
        this.bufferPools = new ArrayList<>();
    }

    @Process(actionType = AddBufferPool.class)
    public void add(final ModelNode bufferPool, final Dispatcher.Channel channel) {
        channel.ack();
    }

    @Process(actionType = ModifyBufferPool.class)
    public void modify(final ModifyPayload modifyPayload, final Dispatcher.Channel channel) {
        final ModelNode op = modifyOp(modifyPayload.getName(), modifyPayload.getChangedValues());

        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                channel.nack(caught);
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    channel.nack(new RuntimeException("Failed to modify buffer-pool " + modifyPayload.getName() +
                            " using " + op + ": " + response.getFailureDescription()));
                } else {
                    refresh(channel);
                }
            }
        });
    }

    @Process(actionType = RefreshBufferPools.class)
    public void refresh(final Dispatcher.Channel channel) {
        channel.ack();
    }

    @Process(actionType = RemoveBufferPool.class)
    public void remove(final String name, final Dispatcher.Channel channel) {
        channel.ack();
    }

    public List<Property> getBufferPools() {
        return bufferPools;
    }
}
