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
package org.jboss.as.console.client.administration.role.operation;

import com.allen_sauer.gwt.log.client.Log;
import org.jboss.as.console.client.administration.role.model.ModelHelper;
import org.jboss.as.console.client.administration.accesscontrol.store.Principal;
import org.jboss.as.console.client.administration.accesscontrol.store.Principals;
import org.jboss.as.console.client.administration.accesscontrol.store.Role;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Outcome;

import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
* @author Harald Pehl
*/
public class ShowMembersOperation implements ManagementOperation<RoleAssignment.Internal> {

    private final DispatchAsync dispatcher;
    private final Role role;
    private final Principals principals;

    public ShowMembersOperation(final DispatchAsync dispatcher, final Role role, final Principals principals) {
        this.dispatcher = dispatcher;
        this.role = role;
        this.principals = principals;
    }

    @Override
    public void execute(final Outcome<RoleAssignment.Internal> outcome) {
        ModelNode node = ModelHelper.roleMapping(role);
        node.get("recursive-depth").set("2");
        node.get(OP).set(READ_RESOURCE_OPERATION);
        dispatcher.execute(new DMRAction(node), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (result.isFailure()) {
                    Log.error("Cannot read members of " + role + ": " + result.getFailureDescription());
                    // Return an empty internal
                    outcome.onFailure(new RoleAssignment.Internal(role));
                } else {
                    RoleAssignment.Internal internal = new RoleAssignment.Internal(role);
                    ModelNode assignmentNode = result.get(RESULT);
                    if (assignmentNode.hasDefined("include")) {
                        List<Property> inclusions = assignmentNode.get("include").asPropertyList();
                        for (Property inclusion : inclusions) {
                            RoleAssignment.PrincipalRealmTupel prt = mapPrincipal(inclusion.getName(),
                                    inclusion.getValue());
                            if (prt != null) {
                                internal.include(prt);
                            }
                        }
                    }
                    if (assignmentNode.hasDefined("exclude")) {
                        List<Property> exclusions = assignmentNode.get("exclude").asPropertyList();
                        for (Property exclusion : exclusions) {
                            RoleAssignment.PrincipalRealmTupel prt = mapPrincipal(exclusion.getName(),
                                    exclusion.getValue());
                            if (prt != null) {
                                internal.exclude(prt);
                            }
                        }
                    }
                    outcome.onSuccess(internal);
                }
            }
        });
    }

    @Override
    public boolean isPending() {
        throw new UnsupportedOperationException("not implemented");
    }

    private RoleAssignment.PrincipalRealmTupel mapPrincipal(String id, ModelNode principalNode) {
        String principalName = principalNode.get("name").asString();
        if (ModelHelper.LOCAL_USERNAME.equals(principalName)) {
            // Skip the local user
            return null;
        }
        Principal.Type type = Principal.Type.valueOf(principalNode.get("type").asString());
        String realm = null;
        if (principalNode.hasDefined("realm")) {
            realm = principalNode.get("realm").asString();
        }
        Principal principal = principals.get(id);
        if (principal != null) {
            return new RoleAssignment.PrincipalRealmTupel(principal, realm);
        }
        return null;
    }
}
