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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.proxy.PlaceManagerImpl;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import com.gwtplatform.mvp.shared.proxy.TokenFormatter;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.rbac.UnauthorizedEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Heiko Braun
 * @date 2/4/11
 */
public class DefaultPlaceManager extends PlaceManagerImpl {

    private final RequiredResourcesProcessor requiredResourcesProcessor;
    private BootstrapContext bootstrap;
    private EventBus eventBus;

    @Inject
    public DefaultPlaceManager(EventBus eventBus, TokenFormatter tokenFormatter, BootstrapContext bootstrap,
            RequiredResourcesProcessor requiredResourcesProcessor) {
        super(eventBus, tokenFormatter);
        this.bootstrap = bootstrap;
        this.eventBus = eventBus;
        this.requiredResourcesProcessor = requiredResourcesProcessor;
    }

    @Override
    public void revealErrorPlace(String invalidHistoryToken) {
        revealDefaultPlace();
    }

    public void revealDefaultPlace() {
        List<PlaceRequest> places = new ArrayList<PlaceRequest>();
        places.add(bootstrap.getDefaultPlace());
        revealPlaceHierarchy(places);
    }

    @Override
    protected void doRevealPlace(final PlaceRequest request, final boolean updateBrowserUrl) {
        requiredResourcesProcessor.process(request.getNameToken(), new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                unlock();
                revealDefaultPlace();
                Console.error(((UIConstants) GWT.create(UIConstants.class)).failedToCreateSecurityContext(), caught.getMessage());
            }

            @Override
            public void onSuccess(Void result) {
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        DefaultPlaceManager.super.doRevealPlace(request, updateBrowserUrl);
                       /* if (updateBrowserUrl) {
                            eventBus.fireEvent(new LHSHighlightEvent(request.getNameToken()));
                        }*/
                    }
                });
            }
        });
    }

    @Override
    public void revealUnauthorizedPlace(String unauthorizedHistoryToken) {
       /* if (NameTokens.DomainRuntimePresenter.equals(unauthorizedHistoryToken)) {
            // a runtime constrain is not given
            // see DomainRuntimeGatekeeper
            revealPlace(new PlaceRequest.Builder().nameToken(NameTokens.NoServer).build());
        } else {

            // Update the history token for the user to see the unauthorized token, but don't navigate!
            updateHistory(new PlaceRequest.Builder().nameToken(unauthorizedHistoryToken).build(), true);
            // Send an unauthorized event notifying the top level presenters to show
            // the unauthorized presenter widget in the main content slot
            UnauthorizedEvent.fire(this, unauthorizedHistoryToken);
        }*/

        UnauthorizedEvent.fire(this, unauthorizedHistoryToken);
    }
}
