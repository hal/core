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

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;
import org.jboss.gwt.flow.client.Precondition;

/**
 * @author Harald Pehl
 */
public abstract class TimeoutOperation {

    private final long timeout;
    private long start;
    private boolean conditionSatisfied;

    public TimeoutOperation(final long timeout) {
        this.timeout = timeout;
    }

    /**
     * Executes a DMR operation and repeatedly calls {@code checker} until {@link #setConditionSatisfied(boolean)}
     * was called with {@code true} or the timeout is reached.
     *
     * @param dispatcher the dispatcher
     * @param operation  the DMR operation which should be executed
     * @param callback   the final callback
     */
    public final void start(final DispatchAsync dispatcher, final ModelNode operation, final Callback callback) {
        Command dispatchCmd = new Command() {
            @Override
            public void execute() {
                dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
                    @Override
                    public void onFailure(final Throwable caught) {
                        callback.onError(caught);
                    }

                    @Override
                    public void onSuccess(final DMRResponse result) {
                        // No action here: We're polling later on until the condition is satisfied
                        // or the timeout is reached.
                    }
                });
            }
        };
        start(dispatchCmd, callback);
    }

    /**
     * Executes {@code command} until {@link #setConditionSatisfied(boolean)} was called with {@code true} or
     * the timeout is reached.
     *
     * @param command  the command which should be executed
     * @param callback the final callback
     */
    public final void start(final Command command, final Callback callback) {
        this.start = System.currentTimeMillis();
        this.conditionSatisfied = false;
        command.execute();
        new Async().whilst(new KeepGoing(), new Finish(callback), checker(), 500);
    }

    private boolean timeout() {
        long elapsed = (System.currentTimeMillis() - start) / 1000;
        return elapsed > timeout;
    }

    protected abstract Function checker();

    protected void setConditionSatisfied(final boolean conditionSatisfied) {
        this.conditionSatisfied = conditionSatisfied;
    }

    public static interface Callback {

        /**
         * Operation was successful within the specified timeout
         */
        void onSuccess();

        /**
         * Operation ran into a timeout
         */
        void onTimeout();

        /**
         * Initial command returned with an error
         *
         * @param caught the exception thrown by the initial command
         */
        void onError(final Throwable caught);
    }


    private class KeepGoing implements Precondition {

        @Override
        public boolean isMet() {
            return !timeout() && !conditionSatisfied;
        }
    }


    private class Finish implements Outcome {

        private final Callback callback;

        Finish(final Callback callback) {
            this.callback = callback;
        }

        @Override
        public void onFailure(final Object context) {
            callback.onTimeout();
        }

        @Override
        public void onSuccess(final Object context) {
            callback.onSuccess();
        }
    }
}
