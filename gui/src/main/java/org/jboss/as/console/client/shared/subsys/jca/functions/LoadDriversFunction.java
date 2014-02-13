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
package org.jboss.as.console.client.shared.subsys.jca.functions;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.shared.subsys.jca.model.DriverStrategy;
import org.jboss.as.console.client.shared.subsys.jca.model.JDBCDriver;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

/**
 * @author Harald Pehl
 */
public class LoadDriversFunction implements Function<FunctionContext> {

    public final static String KEY = "jdbc.drivers";
    private final DriverStrategy driverStrategy;

    public LoadDriversFunction(final DriverStrategy driverStrategy) {this.driverStrategy = driverStrategy;}

    @Override
    public void execute(final Control<FunctionContext> control) {
        // Will start a nested async waterfall
        driverStrategy.refreshDrivers(new AsyncCallback<List<JDBCDriver>>() {
            @Override
            public void onFailure(final Throwable caught) {
                control.getContext().setError(caught);
                control.abort();
            }

            @Override
            public void onSuccess(final List<JDBCDriver> result) {
                control.getContext().set(KEY, result);
                control.proceed();
            }
        });
    }
}
