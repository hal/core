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
package org.jboss.as.console.client.administration.accesscontrol.store;

import org.jboss.as.console.client.Console;
import org.jboss.gwt.circuit.Action;

import static org.jboss.as.console.client.administration.accesscontrol.store.Principal.Type.USER;

/**
 * This is much like the {@link AddAssignment} action but with slightly different semantics.
 *
 * @author Harald Pehl
 */
public class AddPrincipal implements Action, ModifiesPrincipal, HasSuccessMessage {

    private final Assignment assignment;

    public AddPrincipal(final Principal principal, final Role role) {
        this.assignment = new Assignment(principal, role, true);
    }

    public Assignment getAssignment() {
        return assignment;
    }

    @Override
    public Principal getPrincipal() {
        return assignment.getPrincipal();
    }

    @Override
    public String getMessage() {
        String userOrGroup = assignment.getPrincipal().getType() == USER ? Console.CONSTANTS
                .common_label_user() : Console.CONSTANTS.common_label_group();
        return Console.MESSAGES.principalSuccessfullyAdded(userOrGroup);
    }
}
