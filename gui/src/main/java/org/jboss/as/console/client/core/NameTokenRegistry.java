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

import com.google.inject.Inject;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.mbui.widgets.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ResourceDescriptionRegistry;
import org.jboss.ballroom.client.rbac.SecurityContext;

import java.util.HashSet;
import java.util.Set;

/**
 * Meta registry which acts as single entry point to various HAL registries.
 *
 * @author Harald Pehl
 */
public final class NameTokenRegistry {

    private final SecurityFramework securityFramework;
    private final ResourceDescriptionRegistry resourceDescriptionRegistry;
    private final Set<String> revealedTokens;

    @Inject
    public NameTokenRegistry(SecurityFramework securityFramework,
                             ResourceDescriptionRegistry resourceDescriptionRegistry) {
        this.securityFramework = securityFramework;
        this.resourceDescriptionRegistry = resourceDescriptionRegistry;
        this.revealedTokens = new HashSet<>();
    }

    public SecurityContext getSecurityContext(String token) {
        return securityFramework.getSecurityContext(token);
    }

    public ResourceDescription getModelDrivenContext(String template) {
        return resourceDescriptionRegistry.lookup(template);
    }

    public boolean wasRevealed(String token) {
        return revealedTokens.contains(token);
    }

    public void revealed(String token) {
        revealedTokens.add(token);
    }
}
