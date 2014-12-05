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
import com.gwtplatform.mvp.shared.proxy.PlaceTokenRegistry;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.mbui.widgets.ModelDrivenContext;
import org.jboss.as.console.mbui.widgets.ModelDrivenRegistry;
import org.jboss.ballroom.client.rbac.SecurityContext;

import java.util.HashSet;
import java.util.Set;

/**
 * Meta registry which acts as single entry point to various HAL registries.
 *
 * @author Harald Pehl
 */
public final class NameTokenRegistry {

    private final PlaceTokenRegistry tokens;
    private final SecurityFramework securityFramework;
    private final ModelDrivenRegistry modelDrivenRegistry;
    private final Set<String> revealedTokens;

    @Inject
    public NameTokenRegistry(PlaceTokenRegistry tokens, SecurityFramework securityFramework,
                             ModelDrivenRegistry modelDrivenRegistry) {
        this.tokens = tokens;
        this.securityFramework = securityFramework;
        this.modelDrivenRegistry = modelDrivenRegistry;
        this.revealedTokens = new HashSet<>();
    }

    public Set<String> getAllTokens() {
        return tokens.getAllPlaceTokens();
    }

    public SecurityContext getSecurityContext(String token) {
        return securityFramework.getSecurityContext(token);
    }

    public ModelDrivenContext getModelDrivenContext(String template) {
        return modelDrivenRegistry.lookup(template);
    }

    public boolean wasRevealed(String token) {
        return revealedTokens.contains(token);
    }

    public void reveal(String token) {
        revealedTokens.add(token);
    }
}
