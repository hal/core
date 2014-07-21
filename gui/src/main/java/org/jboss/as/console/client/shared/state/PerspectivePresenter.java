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
package org.jboss.as.console.client.shared.state;

import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.rbac.UnauthorisedPresenter;
import org.jboss.as.console.client.rbac.UnauthorizedEvent;

/**
 * Base class for top level presenters like "Configuration", "Server Health" or "Administration". Meets two tasks:
 * <ol>
 * <li>Provides navigation logic to forward to the default place for the perspective or by showing the last known
 * place.</li>
 * <li>Handles the {@link org.jboss.as.console.client.rbac.UnauthorizedEvent}</li> and shows the {@link
 * org.jboss.as.console.client.rbac.UnauthorisedPresenter} in the content slot provided as constructor parameter
 * </ol>
 *
 * @author Harald Pehl
 */
public abstract class PerspectivePresenter<V extends View, Proxy_ extends Proxy<?>> extends Presenter<V, Proxy_>
        implements UnauthorizedEvent.UnauthorizedHandler {

    private final PlaceManager placeManager;
    private final Header header;
    private final String token;
    private final UnauthorisedPresenter unauthorisedPresenter;
    private final Object contentSlot;
    private PlaceRequest lastPlace;
    private boolean hasBeenRevealed;

    public PerspectivePresenter(final EventBus eventBus, final V view, final Proxy_ proxy,
            final PlaceManager placeManager, final Header header, final String token,
            final UnauthorisedPresenter unauthorisedPresenter, Object contentSlot) {

        super(eventBus, view, proxy);
        this.placeManager = placeManager;
        this.header = header;
        this.token = token;
        this.unauthorisedPresenter = unauthorisedPresenter;
        this.contentSlot = contentSlot;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getEventBus().addHandler(UnauthorizedEvent.TYPE, this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        header.highlight(token);

        PlaceRequest requestedPlace = placeManager.getCurrentPlaceRequest();
        boolean isChildRequest = !token.equals(requestedPlace.getNameToken());

        if (isChildRequest) {
            // remember for the next time
            lastPlace = requestedPlace;
        } else if (lastPlace != null) {
            onLastPlace(lastPlace);
            return; // ugly, but important
        }

        if (!hasBeenRevealed) {
            hasBeenRevealed = true;
            onFirstReveal(requestedPlace, placeManager, !isChildRequest);
        }
    }

    /**
     * prepare the initial perspective. most often this does at least navigate to a default place.
     */
    abstract protected void onFirstReveal(final PlaceRequest placeRequest, PlaceManager placeManager, boolean revealDefault);

    /**
     * Forwards to the last place. If you override this method don't forget to call {@code super.onLastPlace()} first.
     */
    protected void onLastPlace(PlaceRequest lastPlace) {
        placeManager.revealPlace(lastPlace);
    }

    /**
     * Sets the {@link org.jboss.as.console.client.rbac.UnauthorisedPresenter} in the content slot given as constructor
     * parameter.
     */
    @Override
    public void onUnauthorized(final UnauthorizedEvent event) {
       // resetLastPlace();
        setInSlot(contentSlot, unauthorisedPresenter);
    }

    /**
     * Clears the last place and resets the "has-been-revealed" status to false. Thus the next time
     * onFirstReveal() will be called again.
     */
    protected void resetLastPlace() {
        hasBeenRevealed = false;
        lastPlace = null;
    }

    public PlaceRequest getLastPlace() {
        return lastPlace;
    }
}
