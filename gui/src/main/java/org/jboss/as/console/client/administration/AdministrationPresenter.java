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
package org.jboss.as.console.client.administration;

import com.google.gwt.event.shared.GwtEvent;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.rbac.UnauthorisedPresenter;
import org.jboss.as.console.client.rbac.UnauthorizedEvent;

/**
 * @author Harald Pehl
 */
public class AdministrationPresenter
        extends Presenter<AdministrationPresenter.MyView, AdministrationPresenter.MyProxy> implements
        UnauthorizedEvent.UnauthorizedHandler {

    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent = new GwtEvent.Type<RevealContentHandler<?>>();
    private final PlaceManager placeManager;
    private boolean hasBeenRevealed;
    private String lastPlace;
    private Header header;
    private final UnauthorisedPresenter unauthorisedPresenter;

    @Inject
    public AdministrationPresenter(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final PlaceManager placeManager, final Header header, UnauthorisedPresenter unauthorisedPresenter) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.header = header;
        this.unauthorisedPresenter = unauthorisedPresenter;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        getEventBus().addHandler(UnauthorizedEvent.TYPE, this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        header.highlight(NameTokens.AdministrationPresenter);

        // chose sub place to reveal
        String currentToken = placeManager.getCurrentPlaceRequest().getNameToken();
        if (!currentToken.equals(getProxy().getNameToken())) {
            lastPlace = currentToken;
        } else if (lastPlace != null) {
            placeManager.revealPlace(new PlaceRequest.Builder().nameToken(lastPlace).build());
        }

        // first request, select default contents
        if (!hasBeenRevealed) {
            if (lastPlace != null) {
                placeManager.revealPlace(new PlaceRequest.Builder().nameToken(lastPlace).build());
            } else {
                placeManager
                        .revealPlace(new PlaceRequest.Builder().nameToken(NameTokens.RoleAssignmentPresenter).build());
            }
            hasBeenRevealed = true;
        }
    }

    @Override
    protected void revealInParent() {
        // reveal in main layout
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }

    @Override
    public void onUnauthorized(final UnauthorizedEvent event) {
        setInSlot(TYPE_MainContent, unauthorisedPresenter);
    }

    @NoGatekeeper // Toplevel navigation presenter - redirects to default / last place
    @ProxyCodeSplit
    @NameToken(NameTokens.AdministrationPresenter)
    public interface MyProxy extends Proxy<AdministrationPresenter>, Place {
    }

    public interface MyView extends View {

        void setPresenter(AdministrationPresenter presenter);
    }
}
