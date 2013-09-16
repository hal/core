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
package org.jboss.as.console.client.administration.role.model;

import com.google.gwt.user.client.ui.HasName;

/**
 * A user or a group of a {@link org.jboss.as.console.client.administration.role.model.RoleAssignment}
 *
 * @author Harald Pehl
 */
public class Principal implements HasName {

    private String name;
    private final Type type;

    public Principal(final Type type, final String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (!(o instanceof Principal)) { return false; }

        Principal principal = (Principal) o;

        if (!name.equals(principal.name)) { return false; }
        if (type != principal.type) { return false; }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return getId();
    }

    public String getId() {
        StringBuilder id = new StringBuilder();
        id.append(type.name().toLowerCase()).append("-").append(name);
        return id.toString();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public static enum Type {
        USER, GROUP
    }
}
