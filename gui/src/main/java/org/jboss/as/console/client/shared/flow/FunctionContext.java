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
package org.jboss.as.console.client.shared.flow;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.jboss.dmr.client.dispatch.DispatchError;

/**
 * General purpose context to be used for functions inside a flow
 * @author Harald Pehl
 */
public class FunctionContext {

    private final Stack<Object> stack;
    private final Map<String, Object> data;
    private Throwable error;

    public FunctionContext() {
        stack = new Stack<Object>();
        data = new HashMap<String, Object>();
    }

    public <T> void push(T value) {
        stack.push(value);
    }

    @SuppressWarnings("unchecked")
    public <T> T pop() {
        return (T) stack.pop();
    }

    public <T> void set(String key, T value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    public void setError(final Throwable error) {
        this.error = error;
    }

    public Throwable getError() {
        return error;
    }

    public boolean hasError() {
        return error != null;
    }

    public String getErrorMessage() {
        //noinspection ThrowableResultOfMethodCallIgnored
        return hasError() ? null : getError().getMessage();
    }

    public boolean isForbidden() {
        return (error instanceof DispatchError && ((DispatchError) error).getStatusCode() == 403);
    }

    @Override
    public String toString() {
        return "FunctionContext {data: " + data + ", stack: " + stack + "}";
    }
}
