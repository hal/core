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
package org.jboss.as.console.client.shared.patching;

import static org.jboss.dmr.client.ModelDescriptionConstants.OP;
import static org.jboss.dmr.client.ModelDescriptionConstants.READ_RESOURCE_OPERATION;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.shared.flow.TimeoutOperation;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

/**
 * @author Harald Pehl
 */
public class RestartOp extends TimeoutOperation {

    private final DispatchAsync dispatcher;
    private final ModelNode alive;

    public RestartOp(final DispatchAsync dispatcher) {
        super(10);
        this.dispatcher = dispatcher;
        this.alive = new ModelNode();
        this.alive.get(OP).set(READ_RESOURCE_OPERATION);
    }

    @Override
    protected Function checker() {
        return new Function() {
            @Override
            public void execute(final Control control) {
                dispatcher.execute(new DMRAction(alive), new AsyncCallback<DMRResponse>() {
                    @Override
                    public void onFailure(final Throwable caught) {
                        // not there
                    }

                    @Override
                    public void onSuccess(final DMRResponse response) {
                        ModelNode result = response.get();
                        setConditionSatisfied(!result.isFailure());
                    }
                });
            }
        };
    }
}
