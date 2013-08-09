/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

package org.jboss.as.console.client.core;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.proxy.PlaceManagerImpl;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.RevealRootPopupContentEvent;
import com.gwtplatform.mvp.client.proxy.TokenFormatter;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.rbac.UnauthorisedPresenter;
import org.jboss.ballroom.client.layout.LHSHighlightEvent;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.rbac.SecurityService;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Heiko Braun
 * @date 2/4/11
 */
public class DefaultPlaceManager extends PlaceManagerImpl {

    private final SecurityService securityService;
    private final UnauthorisedPresenter unauthPlace;
    private BootstrapContext bootstrap;
    private EventBus eventBus;

    @Inject
    public DefaultPlaceManager(
            EventBus eventBus,
            TokenFormatter tokenFormatter, BootstrapContext bootstrap, SecurityService securityService, UnauthorisedPresenter unauthPlace) {
        super(eventBus, tokenFormatter);
        this.bootstrap = bootstrap;
        this.eventBus = eventBus;
        this.securityService = securityService;
        this.unauthPlace = unauthPlace;
    }

    @Override
    public void revealErrorPlace(String invalidHistoryToken) {

        Log.debug("Discard \"" + invalidHistoryToken + "\". Fallback to default place");
        revealDefaultPlace();
    }

    public void revealDefaultPlace() {

        List<PlaceRequest> places = new ArrayList<PlaceRequest>();
        places.add(bootstrap.getDefaultPlace());

        revealPlaceHierarchy(places);
    }

    final class ContextCreation {
        final PlaceRequest request;
        Throwable error;

        ContextCreation(PlaceRequest request) {
            this.request = request;
        }

        PlaceRequest getRequest() {
            return request;
        }

        Throwable getError() {
            return error;
        }

        void setError(Throwable error) {
            this.error = error;
        }
    }

    @Override
    protected void doRevealPlace(final PlaceRequest request, final boolean updateBrowserUrl) {

        Function<ContextCreation> createContext = new Function<ContextCreation>() {
            @Override
            public void execute(final Control<ContextCreation> control) {
                final String nameToken = control.getContext().getRequest().getNameToken();
                if(!securityService.hasContext(nameToken))
                {
                    securityService.createSecurityContext(nameToken, new AsyncCallback<SecurityContext>() {
                        @Override
                        public void onFailure(Throwable throwable) {
                            control.getContext().setError(throwable);
                            control.abort();

                        }

                        @Override
                        public void onSuccess(SecurityContext securityContext) {
                            control.proceed();
                        }
                    });
                }
                else
                {
                    control.proceed();
                }
            }
        };

        Outcome<ContextCreation> outcome = new Outcome<ContextCreation>() {
            @Override
            public void onFailure(ContextCreation context) {
                unlock();
                Log.error("Failed to create security context", context.getError());
                Console.error("Failed to create security context", context.getError().getMessage());
            }

            @Override
            public void onSuccess(final ContextCreation context) {
                unlock();

                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        final PlaceRequest placeRequest = context.getRequest();
                        DefaultPlaceManager.super.doRevealPlace(placeRequest, true);
                        eventBus.fireEvent(
                                new LHSHighlightEvent(placeRequest.getNameToken())
                        );
                    }
                });
            }
        };

        new Async().waterfall(new ContextCreation(request), outcome, createContext);

    }

    @Override
    public void revealUnauthorizedPlace(String unauthorizedHistoryToken) {

        RevealRootPopupContentEvent.fire(this, unauthPlace, true);

    }
}
