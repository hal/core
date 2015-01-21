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

import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import com.gwtplatform.mvp.shared.proxy.TokenFormatter;
import org.jboss.as.console.client.plugins.RequiredResourcesRegistry;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.rbac.SecurityContextChangedEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Extension to the {@link org.jboss.as.console.client.rbac.SecurityFramework} which binds
 * {@link org.jboss.ballroom.client.rbac.SecurityContext}s to parametrized
 * {@link com.gwtplatform.mvp.client.proxy.PlaceRequest}s.
 * <p/>
 * This kind of security framework comes into play when the security context does *not* depend solely on the
 * name token, but on a combination of name token and place request parameter. A typical example is the
 * {@code HostJVMPresenter} where the security context depends on the selected host. Thus the security context is
 * bound to {@code #host-jvms;host=slave} (instead of {@code #host-jvms}).
 *
 * @author Harald Pehl
 */
public class PlaceRequestSecurityFramework {

    private final static SecurityContext DEFAULT_CONTEXT = new NoGatekeeperContext();

    private final SecurityFramework securityFramework;
    private final RequiredResourcesRegistry requiredResourcesRegistry;
    private final TokenFormatter tokenFormatter;
    private final Map<String, SecurityContext> contextCache;

    @Inject
    public PlaceRequestSecurityFramework(final SecurityFramework securityFramework,
                                         final RequiredResourcesRegistry requiredResourcesRegistry, final TokenFormatter tokenFormatter) {
        this.securityFramework = securityFramework;
        this.requiredResourcesRegistry = requiredResourcesRegistry;
        this.tokenFormatter = tokenFormatter;
        this.contextCache = new HashMap<String, SecurityContext>();
    }

    public void addCurrentContext(final PlaceRequest placeRequest) {
        String token = tokenFormatter.toPlaceToken(placeRequest);
        this.contextCache.put(token, securityFramework.getSecurityContext(token));
    }

    /**
     * Loads a previously created security context from the internal cache or creates a one. In any case the
     * security context is bound to the given place request and a {@link org.jboss.ballroom.client.rbac.SecurityContextChangedEvent}
     * is fired.
     *
     * @param eventSource  the source for the {@code SecurityContextChangedEvent}
     * @param placeRequest the place request
     */
    public void update(final Presenter eventSource, final PlaceRequest placeRequest) {
        final String token = placeRequest.getNameToken();
        final String parametrizedToken = tokenFormatter.toPlaceToken(placeRequest);
        final SecurityContext context = lookupContext(placeRequest);
        if (context == null) {
            securityFramework.createSecurityContext(parametrizedToken, requiredResourcesRegistry.getResources(token),
                    requiredResourcesRegistry.isRecursive(token), new AsyncCallback<SecurityContext>() {
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
