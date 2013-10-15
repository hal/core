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

import static org.jboss.dmr.client.ModelDescriptionConstants.OUTCOME;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.StatusCodeException;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Control;

/**
 * @author Harald Pehl
 */
public class FunctionCallback implements AsyncCallback<DMRResponse> {

    private final Control<FunctionContext> control;

    public FunctionCallback(final Control<FunctionContext> control) {this.control = control;}

    @Override
    public final void onSuccess(final DMRResponse response) {
        ModelNode result = response.get();
        if (!result.hasDefined(OUTCOME) || result.isFailure()) {
            control.getContext().setError(new RuntimeException(result.getFailureDescription()));
            abort();
        } else {
            onSuccess(result);
            proceed();
        }
    }

    @Override
    public final void onFailure(final Throwable caught) {
        control.getContext().setError(caught);
        abort();
    }

    protected void onSuccess(final ModelNode result) {
        // empty
    }

    protected void abort() {
        control.abort();
    }

    protected void proceed() {
        control.proceed();
    }
}
