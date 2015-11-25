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

import static org.jboss.as.console.client.administration.accesscontrol.store.ModifiesAssignment.Relation.PRINCIPAL_TO_ROLE;
import static org.jboss.as.console.client.administration.accesscontrol.store.Principal.Type.USER;

/**
 * @author Harald Pehl
 */
public class AddAssignment implements Action, ModifiesAssignment, HasSuccessMessage {

    private final Assignment assignment;
    private final Relation relation;

    public AddAssignment(final Assignment assignment, final Relation relation) {
        this.assignment = assignment;
        this.relation = relation;
    }

    @Override
    public Assignment getAssignment() {
        return assignment;
    }

    @Override
    public Relation getRelation() {
        return relation;
    }

    @Override
    public String getMessage() {
        if (relation == PRINCIPAL_TO_ROLE) {
            if (assignment.isInclude()) {
                return Console.MESSAGES.assignmentIncludeRoleToPrincipal(assignment.getRole().getId(),
                        assignment.getPrincipal().getNameAndRealm());
            } else {
                return Console.MESSAGES.assignmentExcludeRoleToPrincipal(assignment.getRole().getId(),
                        assignment.getPrincipal().getNameAndRealm());
            }
        } else {
            String userOrGroup = assignment.getPrincipal().getType() == USER ? Console.CONSTANTS
                    .common_label_user() : Console.CONSTANTS.common_label_group();
            if (assignment.isInclude()) {
                return Console.MESSAGES.assignmentIncludePrincipalToRole(userOrGroup,
                        assignment.getPrincipal().getNameAndRealm(), assignment.getRole().getId());
            } else {
                return Console.MESSAGES.assignmentExcludePrincipalToRole(userOrGroup,
                        assignment.getPrincipal().getNameAndRealm(), assignment.getRole().getId());
            }
        }
    }
}
