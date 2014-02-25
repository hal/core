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
package org.jboss.as.console.client.domain.topology;

import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.domain.model.impl.LifecycleOperation;
import org.jboss.gwt.flow.client.Outcome;
import org.jboss.gwt.flow.client.Precondition;

/**
 * @author Harald Pehl
 */
public abstract class TopologyOp {

    protected final int timeout;
    protected final LifecycleOperation op;
    protected final LifecycleCallback callback;
    protected boolean lifecycleReached;
    protected long start;

    protected TopologyOp(final LifecycleOperation op, final LifecycleCallback callback) {
        this.op = op;
        this.timeout = op.limit();
        this.callback = callback;
        lifecycleReached = false;
        start = System.currentTimeMillis();
    }

    public abstract void run();

    protected boolean timeout() {
        long elapsed = (System.currentTimeMillis() - start) / 1000;
        return elapsed > op.limit();
    }


    public class BooleanCallback extends SimpleCallback<Boolean> {

        @Override
        public void onSuccess(final Boolean result) {
            // nop
        }

        @Override
        public void onFailure(final Throwable caught) {
            callback.onError(caught);
        }
    }

    public class KeepGoing implements Precondition {

        @Override
        public boolean isMet() {
            return !timeout() && !lifecycleReached;
        }
    }

    public class Finish implements Outcome {

        @Override
        public void onFailure(final Object context) {
            callback.onAbort();
        }

        @Override
        public void onSuccess(final Object context) {
            if (lifecycleReached) {
                callback.onSuccess();
            } else if (timeout()) {
                callback.onTimeout();
            }
        }
    }
}
