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
package org.jboss.as.console.client.rbac;

/**
 * @author Heiko Braun
 */
public enum StandardRole implements Role {

    MONITOR("Monitor"),
    OPERATOR("Operator"),
    MAINTAINER("Maintainer"),
    DEPLOYER("Deployer"),
    ADMINISTRATOR("Administrator"),
    AUDITOR("Auditor"),
    SUPERUSER("SuperUser");

    private final String title;

    StandardRole(final String title) {this.title = title;}

    @Override
    public String getName() {
        return title;
    }

    @Override
    public void setName(final String name) {
        throw new UnsupportedOperationException("Not supported for standard roles");
    }

    public static StandardRole fromString(String s) {
        return StandardRole.valueOf(s.toUpperCase());
    }
}

