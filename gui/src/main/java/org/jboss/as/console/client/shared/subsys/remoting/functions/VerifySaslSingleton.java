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
package org.jboss.as.console.client.shared.subsys.remoting.functions;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.useware.kernel.gui.behaviour.StatementContext;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * Checks whether a specific security singleton exists for a given remote (HTTP) connector. Puts
 * 200 in the function context if it exists, 404 otherwise.
 *
 * @author Harald Pehl
 */
public class VerifySaslSingleton implements Function<FunctionContext> {

    private final DispatchAsync dispatcher;
    private final StatementContext statementContext;
    private final String connectorName;
    private final AddressTemplate singletonAddress;

    public VerifySaslSingleton(DispatchAsync dispatcher, StatementContext statementContext,
                               String connectorName, AddressTemplate singletonAddress) {
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
        this.connectorName = connectorName;
        this.singletonAddress = singletonAddress;
    }


    @Override
    public void execute(final Control<FunctionContext> control) {
        ModelNode op = new ModelNode();
        op.get(ADDRESS).set(singletonAddress.resolve(statementContext, connectorName));
        op.get(OP).set(READ_RESOURCE_OPERATION);
        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                // security singleton does not exist, create in next step
                control.getContext().push(404);
                control.proceed();
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode result = dmrResponse.get();
                if (result.isFailure()) {
                    // security singleton does not exist, create in next step
                    control.getContext().push(404);
                } else {
                    // security singleton already exists, signal next step to skip creation
                    control.getContext().push(200);
                }
                control.proceed();
            }
        });
    }
}
