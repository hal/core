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

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.shared.flow.TimeoutOperation;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

/**
 * @author Harald Pehl
 */
public class StopServersOp extends TimeoutOperation {

    private final DispatchAsync dispatcher;
    private final ModelNode queryOp;

    public StopServersOp(final DispatchAsync dispatcher, final String host, final List<String> runningServers) {
        super(5);
        this.dispatcher = dispatcher;

        this.queryOp = new ModelNode();
        this.queryOp.get(ADDRESS).setEmptyList();
        this.queryOp.get(OP).set(COMPOSITE);
        List<ModelNode> steps = new LinkedList<ModelNode>();

        for (String server : runningServers) {
            ModelNode query = new ModelNode();
            query.get(ADDRESS).add("host", host);
            query.get(ADDRESS).add("server-config", server);
            query.get(OP).set(READ_ATTRIBUTE_OPERATION);
            query.get(NAME).set("status");
            steps.add(query);
        }
        queryOp.get(STEPS).set(steps);
    }

    @Override
    protected Function checker() {
        return new Function() {
            @Override
            public void execute(final Control control) {
                dispatcher.execute(new DMRAction(queryOp, false), new AsyncCallback<DMRResponse>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        // ignore
                    }

                    @Override
                    public void onSuccess(DMRResponse dmrResponse) {
                        boolean allStopped = true;
                        ModelNode response = dmrResponse.get();
                        if (!response.isFailure()) {
                            List<Property> steps = response.get(RESULT).asPropertyList();
                            for (Iterator<Property> iterator = steps.iterator(); iterator.hasNext() && allStopped; ) {
                                Property step = iterator.next();
                                ModelNode status = step.getValue().get(RESULT);
                                if (!"stopped".equalsIgnoreCase(status.asString())) {
                                    allStopped = false;
                                }
                            }
                            setConditionSatisfied(allStopped);
                        }
                    }
                });
            }
        };
    }
}
