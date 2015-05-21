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

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Control;

import static org.jboss.dmr.client.ModelDescriptionConstants.OUTCOME;

/**
 * General purpose call back for functions inside a flow. Expects a {@link FunctionContext} as context where an error
 * is stored.
 *
 * @author Harald Pehl
 */
public class FunctionCallback implements AsyncCallback<DMRResponse> {

    private final Control<FunctionContext> control;
    protected final FunctionContext context;

    public FunctionCallback(final Control<FunctionContext> control) {
        this.control = control;
        this.context = control.getContext();
    }

    /**
     * Stores the exception in the context, calls {@link #onError(Throwable)} {@linkplain Control#abort()}s the control
     * flow.
     */
    @Override
    public void onFailure(final Throwable caught) {
        context.setError(caught);
        onError(caught);
        abort();
    }

    /**
     * Override this method to handle exceptions. Called from {@link #onFailure(Throwable)}. Do not call {@link
     * Control#abort()} from this method, {@link #onSuccess(DMRResponse)} takes care of this.
     */
    @SuppressWarnings("UnusedParameters")
    public void onError(final Throwable error) {
    }

    /**
     * Takes the payload from the response and checks if it was successful. Calls {@link #onSuccess(ModelNode)} and
     * {@link #proceed()} if no error occurred, {@link #onFailedOutcome(ModelNode)} and {@link #abort()} otherwise.
     */
    @Override
    public void onSuccess(final DMRResponse response) {
        ModelNode result = response.get();
        if (!result.hasDefined(OUTCOME) || result.isFailure()) {
            onFailedOutcome(result);
            abort();
        } else {
            onSuccess(result);
            proceed();
        }
    }

    /**
     * Called from {@link #onSuccess(DMRResponse)} if the payload contains an error. Sets the error message to
     * {@link ModelNode#getFailureDescription()}. Do not call {@link Control#abort()} from this method, {@link
     * #onSuccess(DMRResponse)} takes care of this.
     */
    protected void onFailedOutcome(final ModelNode result) {
        context.setErrorMessage(result.getFailureDescription());
    }

    /**
     * Shortcut for {@link Control#abort()}
     */
    protected void abort() {
        control.abort();
    }

    /**
     * Override this method if you want to process a successful payload. Do not call {@link Control#proceed()} from
     * this method, {@link #onSuccess(DMRResponse)} takes care of this.
     */
    public void onSuccess(final ModelNode result) {
        // noop
    }

    /**
     * Shortcut for {@link Control#proceed()}
     */
    protected void proceed() {
        control.proceed();
    }
}
