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
package org.jboss.dmr.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.dmr.client.dispatch.Action;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.DispatchRequest;
import org.jboss.dmr.client.dispatch.Result;
import org.jboss.dmr.client.dispatch.impl.DMRAction;

import java.util.Stack;

/**
 * A static dispatcher for unit tests which holds a stack of expected results.
 *
 * @author Harald Pehl
 */
public class StaticDispatcher implements DispatchAsync {

    private final Stack<Object> results;
    private Throwable failure;
    private Action action;

    public StaticDispatcher() {
        results = new Stack<>();
        failure = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A extends Action<R>, R extends Result> DispatchRequest execute(A action, AsyncCallback<R> callback) {
        this.action = action;
        if (failure != null) {
            callback.onFailure(failure);
        } else {
            if (results.isEmpty()) {
                throw new IllegalStateException("Results stack is empty. Please call " + StaticDispatcher.class.getSimpleName() + ".push(result) to push expected results!");
            }
            callback.onSuccess((R) results.pop());
        }
        return null;
    }

    @Override
    public <A extends Action<R>, R extends Result> DispatchRequest undo(A action, R result, AsyncCallback<Void> callback) {
        throw new UnsupportedOperationException();
    }

    public <R extends Result> void push(R result) {
        results.push(result);
    }

    public ModelNode getLastOperation() {
        if (action instanceof DMRAction) {
            return ((DMRAction) action).getOperation();
        }
        return null;
    }

    public void setFailure(Throwable failure) {
        this.failure = failure;
    }

    public void clearFailure() {
        this.failure = null;
    }

    @Override
    public void setProperty(String key, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearProperty(String key) {
        throw new UnsupportedOperationException();
    }
}
