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

import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.ChangeSupport;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Harald Pehl
 */
@Store
public class BufferPoolStore extends ChangeSupport {

    private final List<Property> bufferPools;

    public BufferPoolStore() {
        this.bufferPools = new ArrayList<>();
    }

    @Process(actionType = AddBufferPool.class)
    public void add(ModelNode bufferPool, Dispatcher.Channel channel) {
        channel.ack();
    }

    @Process(actionType = ModifyBufferPool.class)
    public void modify(ModelNode bufferPool, Dispatcher.Channel channel) {
        channel.ack();
    }

    @Process(actionType = RefreshBufferPools.class)
    public void refresh(Dispatcher.Channel channel) {
        channel.ack();
    }

    @Process(actionType = RemoveBufferPool.class)
    public void remove(String name, Dispatcher.Channel channel) {
        channel.ack();
    }

    public List<Property> getBufferPools() {
        return bufferPools;
    }
}
