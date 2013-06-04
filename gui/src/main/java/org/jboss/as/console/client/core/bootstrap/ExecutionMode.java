/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

package org.jboss.as.console.client.core.bootstrap;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.dmr.client.ModelNode;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

import java.util.ArrayList;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 5/19/11
 */
public class ExecutionMode implements Function<BootstrapContext> {

    private DispatchAsync dispatcher;

    public ExecutionMode(DispatchAsync dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void execute(final Control<BootstrapContext> control) {

        final BootstrapContext context = control.getContext();

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(COMPOSITE);
        operation.get(ADDRESS).setEmptyList();

        ModelNode step;
        List<ModelNode> steps = new ArrayList<ModelNode>();

        // exec type
        step = new ModelNode();
        step.get(OP).set(READ_ATTRIBUTE_OPERATION);
        step.get(NAME).set("process-type");
        step.get(ADDRESS).setEmptyList();
        steps.add(step);

        // product name
        step = new ModelNode();
        step.get(OP).set(READ_ATTRIBUTE_OPERATION);
        step.get(NAME).set("product-name");
        step.get(ADDRESS).setEmptyList();
        steps.add(step);

        // release codename
        step = new ModelNode();
        step.get(OP).set(READ_ATTRIBUTE_OPERATION);
        step.get(NAME).set("release-codename");
        step.get(ADDRESS).setEmptyList();
        steps.add(step);

        // product version
        step = new ModelNode();
        step.get(OP).set(READ_ATTRIBUTE_OPERATION);
        step.get(NAME).set("product-version");
        step.get(ADDRESS).setEmptyList();
        steps.add(step);

        // release version
        step = new ModelNode();
        step.get(OP).set(READ_ATTRIBUTE_OPERATION);
        step.get(NAME).set("release-version");
        step.get(ADDRESS).setEmptyList();
        steps.add(step);

        operation.get(STEPS).set(steps);

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {

            @Override
            public void onFailure(Throwable caught) {
                context.setlastError(caught);
                Log.error(caught.getMessage());
                control.abort();
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if(response.isFailure()) {
                    context.setlastError(new RuntimeException(response.getFailureDescription()));
                    control.abort();
                } else {
                    // capture exec mode
                    ModelNode execMode = response.get(RESULT).get("step-1");
                    boolean isServer = execMode.get(RESULT).asString().equals("Server");
                    context.setProperty(BootstrapContext.STANDALONE, Boolean.valueOf(isServer).toString());

                    // product name, release codename
                    ModelNode productName = response.get(RESULT).get("step-2");
                    ModelNode releaseCodename = response.get(RESULT).get("step-3");
                    if (productName.get(RESULT).isDefined()) {
                        context.setProductName(productName.get(RESULT).asString());
                    } else if (releaseCodename.get(RESULT).isDefined()) {
                        context.setProductName(releaseCodename.get(RESULT).asString());
                    }

                    // product version, release version
                    ModelNode productVersion = response.get(RESULT).get("step-4");
                    ModelNode releaseVersion = response.get(RESULT).get("step-5");
                    if (productVersion.get(RESULT).isDefined()) {
                        context.setProductVersion(productVersion.get(RESULT).asString());
                    } else if (releaseVersion.get(RESULT).isDefined()) {
                        context.setProductVersion(releaseVersion.get(RESULT).asString());
                    }

                    System.out.println(context.getProductName() + " " + context.getProductVersion());
                    control.proceed();
                }
            }
        });
    }
}
