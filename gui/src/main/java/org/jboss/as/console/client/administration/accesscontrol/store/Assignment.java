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

import com.google.common.base.Joiner;

/**
 * An assignment between a principal and a role.
 *
 * @author Harald Pehl
 */
public class Assignment {

    private final Principal principal;
    private final Role role;
    private final boolean include;

    public Assignment(final Principal principal, final Role role, final boolean include) {
        this.principal = principal;
        this.role = role;
        this.include = include;
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (!(o instanceof Assignment)) { return false; }

        Assignment that = (Assignment) o;

        if (include != that.include) { return false; }
        if (!principal.equals(that.principal)) { return false; }
        return role.equals(that.role);

    }

    @Override
    public int hashCode() {
        int result = principal.hashCode();
        result = 31 * result + role.hashCode();
        result = 31 * result + (include ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return (include ? "Include " : "Exclude ") + principal + " -> " + role;
    }

    public String getId() {
        return Joiner.on('-').join(principal.getId(), role.getId(), include ? "include" : "exclude");
    }

    public Principal getPrincipal() {
        return principal;
    }

    public Role getRole() {
        return role;
    }

    public boolean isInclude() {
        return include;
    }
}
