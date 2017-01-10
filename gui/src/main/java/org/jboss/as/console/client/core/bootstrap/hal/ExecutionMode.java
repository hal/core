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

package org.jboss.as.console.client.core.bootstrap.hal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.semver.ManagementModel;
import org.jboss.as.console.client.semver.Version;
import org.jboss.as.console.client.shared.Preferences;
import org.jboss.as.console.client.v3.dmr.Composite;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Control;

import static org.jboss.dmr.client.ModelDescriptionConstants.ATTRIBUTES_ONLY;
import static org.jboss.dmr.client.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.dmr.client.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.dmr.client.ModelDescriptionConstants.RESULT;

/**
 * @author Heiko Braun
 * @date 5/19/11
 */
public class ExecutionMode implements BootstrapStep {

    private final  DispatchAsync dispatcher;

    @Inject
    public ExecutionMode(DispatchAsync dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void execute(final Control<BootstrapContext> control) {

        final BootstrapContext context = control.getContext();

        // root resource attributes
        Operation op1 = new Operation.Builder(READ_RESOURCE_OPERATION, ResourceAddress.ROOT)
                .param(ATTRIBUTES_ONLY, true)
                .param(INCLUDE_RUNTIME, true)
                .build();

        // whoami
        Operation op2 = new Operation.Builder("whoami", ResourceAddress.ROOT).param("verbose", true).build();

        dispatcher.execute(new DMRAction(new Composite(op1, op2)), new AsyncCallback<DMRResponse>() {

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
                    ModelNode rootAttributes = response.get(RESULT).get("step-1").get(RESULT);

                    // capture exec mode
                    boolean isServer = rootAttributes.get("process-type").asString().equals("Server");
                    context.setProperty(BootstrapContext.STANDALONE, Boolean.valueOf(isServer).toString());


                    // product name, release codename
                    ModelNode productName = rootAttributes.get("product-name");
                    ModelNode releaseCodename = rootAttributes.get("release-codename");
                    if (productName.isDefined()) {
                        context.setProductName(productName.asString());
                    } else if (releaseCodename.isDefined()) {
                        context.setProductName(releaseCodename.asString());
                    }

                    // product version, release version
                    ModelNode productVersion = rootAttributes.get("product-version");
                    ModelNode releaseVersion = rootAttributes.get("release-version");
                    if (productVersion.isDefined()) {
                        context.setProductVersion(productVersion.asString());
                    } else if (releaseVersion.isDefined()) {
                        context.setProductVersion(releaseVersion.asString());
                    }

                    ModelNode serverName = rootAttributes.get("name");
                    if (serverName.isDefined()) {
                        context.setServerName(serverName.asString());
                    }
                    ModelNode majorVersion = rootAttributes.get("management-major-version");
                    if (majorVersion.isDefined()) {
                        context.setMajorVersion(majorVersion.asLong());
                    }
                    Version version = ManagementModel.parseVersion(rootAttributes);
                    context.setManagementVersion(version);

                    ModelNode whoami = response.get(RESULT).get("step-2").get(RESULT);

                    String username = whoami.get("identity").get("username").asString();
                    context.setPrincipal(username);
                    Set<String> mappedRoles = new HashSet<String>();
                    if(whoami.hasDefined("mapped-roles"))
                    {
                        List<ModelNode> roles = whoami.get("mapped-roles").asList();
                        for(ModelNode role : roles)
                        {
                            final String roleName = role.asString();
                            mappedRoles.add(roleName);
                        }
                    }
                    context.setRoles(mappedRoles);

                    if(context.isSuperUser() && Preferences.has(Preferences.Key.RUN_AS_ROLE))
                    {
                        String runAsRole = Preferences.get(Preferences.Key.RUN_AS_ROLE);
                        dispatcher.setProperty("run_as", runAsRole);
                        context.setRunAs(runAsRole);
                    }
                    Preferences.clear(Preferences.Key.RUN_AS_ROLE);

                    control.proceed();
                }
            }
        });
    }
}
