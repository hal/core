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
package org.jboss.as.console.client.administration.role.operation;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.StatusCodeException;
import org.jboss.as.console.client.Console;
import org.jboss.dmr.client.dispatch.DispatchError;

/**
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

    public void showError() {
        String details = null;
        String message = Console.CONSTANTS.common_error_unknownError();
        if (error != null) {
            details = error.getMessage();
            Log.error(details, error);
            if (isForbidden()) {
                message = Console.CONSTANTS.unauthorized();
                details = Console.CONSTANTS.unauthorized_desc();
            }
        }
        Console.error(message, details);
    }

    public String getErrorMessage() {
        if (error != null) {
            return error.getMessage();
        }
        return null;
    }

    public boolean isForbidden() {
        return (error instanceof DispatchError && ((DispatchError) error).getStatusCode() == 403);
    }

    @Override
    public String toString() {
        return "FunctionContext {data: " + data + ", stack: " + stack + "}";
    }

}
