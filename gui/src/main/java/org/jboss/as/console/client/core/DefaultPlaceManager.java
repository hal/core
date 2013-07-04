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
import com.gwtplatform.mvp.client.proxy.TokenFormatter;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.rbac.SecurityContext;
import org.jboss.as.console.client.rbac.SecurityService;
import org.jboss.as.console.client.shared.Preferences;
import org.jboss.ballroom.client.layout.LHSHighlightEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Heiko Braun
 * @date 2/4/11
 */
public class DefaultPlaceManager extends PlaceManagerImpl {

    private final SecurityService securityService;
    private BootstrapContext bootstrap;
    private EventBus eventBus;

    @Inject
    public DefaultPlaceManager(
            EventBus eventBus,
            TokenFormatter tokenFormatter, BootstrapContext bootstrap, SecurityService securityService ) {
        super(eventBus, tokenFormatter);
        this.bootstrap = bootstrap;
        this.eventBus = eventBus;
        this.securityService = securityService;
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

    @Override
    protected void doRevealPlace(final PlaceRequest request, final boolean updateBrowserUrl) {

        final String nameToken = request.getNameToken();

        if(!securityService.hasContext(nameToken))
        {
            securityService.createSecurityContext(nameToken, new AsyncCallback<SecurityContext>() {
                @Override
                public void onFailure(Throwable throwable) {
                    Console.error("SecurityServiceException", throwable.getMessage());
                    unlock();
                    revealUnauthorizedPlace(nameToken);
                }

                @Override
                public void onSuccess(SecurityContext securityContext) {

                    unlock();
                    doRevealPlace(request, updateBrowserUrl);

                }
            });
        }
        else
        {
            // this is where the gatekeeper kicks in ...
            super.doRevealPlace(request, updateBrowserUrl);

            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    eventBus.fireEvent(
                            new LHSHighlightEvent(nameToken)
                    );
                }
            });


            // ability to invalidate the security context
            if(Preferences.get(Preferences.Key.SECURITY_CONTEXT, "true").equals("false"))
                securityService.flushContext(nameToken);
        }


    }

    @Override
    public void revealUnauthorizedPlace(String unauthorizedHistoryToken) {

        revealPlace(new PlaceRequest(NameTokens.Unauthorized));

    }
}
