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

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.TokenFormatter;
import org.jboss.as.console.client.plugins.AccessControlRegistry;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.rbac.SecurityContextChangedEvent;

/**
 * Extension to the {@link org.jboss.as.console.client.rbac.SecurityFramework} which binds
 * {@link org.jboss.ballroom.client.rbac.SecurityContext}s to parametrized
 * {@link com.gwtplatform.mvp.client.proxy.PlaceRequest}s.
 *
 * @author Harald Pehl
 */
public class PlaceRequestSecurityFramework {

    private final static SecurityContext DEFAULT_CONTEXT = new NoGatekeeperContext();

    private final SecurityFramework securityFramework;
    private final AccessControlRegistry accessControlMetaData;
    private final TokenFormatter tokenFormatter;
    private final Map<String, SecurityContext> contextCache;

    @Inject
    public PlaceRequestSecurityFramework(final SecurityFramework securityFramework,
            final AccessControlRegistry accessControlMetaData, final TokenFormatter tokenFormatter) {
        this.securityFramework = securityFramework;
        this.accessControlMetaData = accessControlMetaData;
        this.tokenFormatter = tokenFormatter;
        this.contextCache = new HashMap<String, SecurityContext>();
    }

    public void addCurrentContext(final PlaceRequest placeRequest) {
        this.contextCache.put(tokenFormatter.toPlaceToken(placeRequest), securityFramework.getSecurityContext());
    }

    /**
     * Loads a previously created security context from the internal cache or creates a one. In any case the
     * security context is bound to the given place request and an {@link org.jboss.ballroom.client.rbac.SecurityContextChangedEvent}
     * is fired.
     *
     * @param eventSource  the source for the {@code SecurityContextChangedEvent}
     * @param placeRequest the place request
     */
    public void update(final HasHandlers eventSource, final PlaceRequest placeRequest) {
        final String token = placeRequest.getNameToken();
        final String parametrizedToken = tokenFormatter.toPlaceToken(placeRequest);
        final SecurityContext context = lookupContext(placeRequest);
        if (context == null) {
            securityFramework.createSecurityContext(parametrizedToken, accessControlMetaData.getResources(token),
                    accessControlMetaData.isRecursive(token), new AsyncCallback<SecurityContext>() {
                        @Override
                        public void onFailure(final Throwable caught) {
                            SecurityContextChangedEvent.fire(eventSource, DEFAULT_CONTEXT);
                        }

                        @Override
                        public void onSuccess(final SecurityContext result) {
                            contextCache.put(parametrizedToken, result);
                            SecurityContextChangedEvent.fire(eventSource, result);
                        }
                    }
            );
        } else {
            SecurityContextChangedEvent.fire(eventSource, context);
        }
    }

    private SecurityContext lookupContext(PlaceRequest placeRequest) {
        return contextCache.get(tokenFormatter.toPlaceToken(placeRequest));
    }
}
