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

import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.Map;

/**
 * @author Harald Pehl
 */
public class ModifySaslSingleton implements Function<FunctionContext> {

    private final CrudOperationDelegate operationDelegate;
    private final String connectorName;
    private final AddressTemplate singletonAddress;
    private final Map<String, Object> changedValues;

    public ModifySaslSingleton(DispatchAsync dispatcher, StatementContext statementContext, String connectorName,
                               AddressTemplate singletonAddress, Map<String, Object> changedValues) {
        this.changedValues = changedValues;
        this.operationDelegate = new CrudOperationDelegate(statementContext, dispatcher);
        this.connectorName = connectorName;
        this.singletonAddress = singletonAddress;
    }

    @Override
    public void execute(final Control<FunctionContext> control) {
        operationDelegate.onSaveResource(singletonAddress, connectorName, changedValues,
                new CrudOperationDelegate.Callback() {
                    @Override
                    public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {
                        control.getContext().setError(t);
                        control.abort();
                    }

                    @Override
                    public void onSuccess(AddressTemplate addressTemplate, String name) {
                        control.proceed();
                    }
                });
    }
}
