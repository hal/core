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
package org.jboss.as.console.client.administration.accesscontrol.store;

/**
 * A user or a group with an optional realm.
 *
 * @author Harald Pehl
 */
public class Principal {

    private final Type type;
    private final String id;
    private final boolean transient_;
    private final String name;
    private final String realm;

    public static Principal transientPrincipal(final Type type, final String name, final String realm) {
        return new Principal(type, generateId(type, name, realm), true, name, realm);
    }

    public static Principal persistentPrincipal(final Type type, final String id, final String name, final String realm) {
        return new Principal(type, id, false, name, realm);
    }

    private static String generateId(final Type type, final String name, final String realm) {
        StringBuilder builder = new StringBuilder();
        builder.append(type.name().toLowerCase()).append("-").append(name);
        if (realm != null) {
            builder.append("@").append(realm);
        }
        return builder.toString();
    }

    private Principal(final Type type, final String id, final boolean transient_, final String name, final String realm) {
        this.type = type;
        this.id = id;
        this.transient_ = transient_;
        this.name = name;
        this.realm = realm;
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (!(o instanceof Principal)) { return false; }

        Principal principal = (Principal) o;

        if (type != principal.type) { return false; }
        if (!id.equals(principal.id)) { return false; }
        if (!name.equals(principal.name)) { return false; }
        return !(realm != null ? !realm.equals(principal.realm) : principal.realm != null);

    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + id.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (realm != null ? realm.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return transient_ ? "Transient " : "" + type + "(" + getNameAndRealm() + ")";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRealm() {
        return realm;
    }

    public String getNameAndRealm() {
        return realm == null ? name : name + "@" + realm;
    }

    public Type getType() {
        return type;
    }

    public boolean isTransient() {
        return transient_;
    }

    public enum Type {
        USER, GROUP
    }
}
