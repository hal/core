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
package org.jboss.as.console.client.core;

import org.jboss.as.console.client.rbac.ReadOnlyContext;
import org.jboss.as.console.client.rbac.ResourceRef;
import org.jboss.as.console.client.rbac.SecurityContextImpl;
import org.jboss.as.console.mbui.widgets.ResourceDescriptionRegistry;
import org.jboss.ballroom.client.rbac.SecurityContext;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Harald Pehl
 */
public class RequiredResourcesContext {

    private final String token;
    private final ResourceDescriptionRegistry resourceDescriptionRegistry;
    private final SecurityContextImpl securityContextImpl;
    private boolean readOnly;
    private Throwable error;

    public RequiredResourcesContext(String token, Set<String> requiredResources,
                                    ResourceDescriptionRegistry resourceDescriptionRegistry) {
        this.token = token;
        this.resourceDescriptionRegistry = resourceDescriptionRegistry;

        Set<ResourceRef> refs = new HashSet<>();
        for (String requiredResource : requiredResources) {
            refs.add(new ResourceRef(requiredResource));
        }
        this.securityContextImpl = new SecurityContextImpl(token, refs);
    }

    public String getToken() {
        return token;
    }

    public SecurityContext getSecurityContext() {
        return readOnly ? new ReadOnlyContext() : securityContextImpl;
    }

    SecurityContextImpl getSecurityContextImpl() {
        return securityContextImpl;
    }

    void makeReadonly() {
        this.readOnly = true;
    }

    public ResourceDescriptionRegistry getResourceDescriptionRegistry() {
        return resourceDescriptionRegistry;
    }

    public void setError(final Throwable error) {
        this.error = error;
    }

    public Throwable getError() {
        return error;
    }
}
