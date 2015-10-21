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
package org.jboss.as.console.client.shared.subsys.picketlink;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.shared.flow.FunctionCallback;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.v3.behaviour.ModelNodeAdapter;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;

import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.ADD;
import static org.jboss.dmr.client.ModelDescriptionConstants.READ_RESOURCE_OPERATION;

/**
 * @author Harald Pehl
 */
class SingletonFunctions {

    private final DispatchAsync dispatcher;
    private final ResourceAddress address;
    private final Map<String, Object> changedValues;
    private final Operation addOperation;


    class Lookup implements Function<FunctionContext> {

        @Override
        public void execute(final Control<FunctionContext> control) {
            Operation op = new Operation.Builder(READ_RESOURCE_OPERATION, address).build();
            dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
                @Override
                public void onFailure(final Throwable throwable) {
                    control.getContext().push(404);
                    control.proceed();
                }

                @Override
                public void onSuccess(final DMRResponse dmrResponse) {
                    if (dmrResponse.get().isFailure()) {
                        control.getContext().push(404);
                    } else {
                        control.getContext().push(200);
                    }
                    control.proceed();
                }
            });
        }
    }


    class Add implements Function<FunctionContext> {

        @Override
        public void execute(final Control<FunctionContext> control) {
            int status = control.getContext().pop();
            if (status == 404) {
                Operation op = addOperation == null ? new Operation.Builder(ADD, address).build() : addOperation;
                dispatcher.execute(new DMRAction(op), new FunctionCallback(control));
            } else {
                control.proceed();
            }
        }
    }


    class Modify implements Function<FunctionContext> {

        @Override
        public void execute(final Control<FunctionContext> control) {
            ModelNodeAdapter adapter = new ModelNodeAdapter();
            ModelNode op = adapter.fromChangeSet(address, changedValues);
            dispatcher.execute(new DMRAction(op), new FunctionCallback(control));
        }
    }

    SingletonFunctions(final DispatchAsync dispatcher,
            final ResourceAddress address,
            final Map<String, Object> changedValues,
            final Operation addOperation) {
        this.dispatcher = dispatcher;
        this.address = address;
        this.changedValues = changedValues;
        this.addOperation = addOperation;
    }

    void modify(final Outcome<FunctionContext> outcome) {
        new Async<FunctionContext>(Footer.PROGRESS_ELEMENT).waterfall(new FunctionContext(), outcome,
                new Lookup(), new Add(), new Modify());
    }
}
