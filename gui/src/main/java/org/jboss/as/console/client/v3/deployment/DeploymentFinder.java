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
package org.jboss.as.console.client.v3.deployment;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.shared.state.PerspectivePresenter;
import org.jboss.as.console.client.v3.presenter.Finder;
import org.jboss.as.console.client.widgets.nav.v3.ClearFinderSelectionEvent;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.FinderScrollEvent;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;

/**
 * @author Harald Pehl
 */
public abstract class DeploymentFinder<V extends DeploymentFinder.DeploymentView, Proxy_ extends Proxy<?>>
        extends PerspectivePresenter<V, Proxy_>
        implements Finder, PreviewEvent.Handler, FinderScrollEvent.Handler, ClearFinderSelectionEvent.Handler {

    // ------------------------------------------------------ inner classes

    public interface DeploymentView<F extends DeploymentFinder> extends View, HasPresenter<F> {

        void setPreview(SafeHtml html);

        void clearActiveSelection(ClearFinderSelectionEvent event);

        void toggleScrolling(boolean enforceScrolling, int requiredWidth);
    }


    // ------------------------------------------------------ presenter lifecycle

    public DeploymentFinder(final EventBus eventBus, final V view,
            final Proxy_ proxy, final PlaceManager placeManager,
            final Header header, final String token, final Object contentSlot) {
        super(eventBus, view, proxy, placeManager, header, token, contentSlot);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);

        // GWT event handler
        registerHandler(getEventBus().addHandler(PreviewEvent.TYPE, this));
        registerHandler(getEventBus().addHandler(FinderScrollEvent.TYPE, this));
        registerHandler(getEventBus().addHandler(ClearFinderSelectionEvent.TYPE, this));
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }


    // ------------------------------------------------------ finder related

    @Override
    public FinderColumn.FinderId getFinderId() {
        return FinderColumn.FinderId.DEPLOYMENT;
    }

    @Override
    public void onPreview(PreviewEvent event) {
        if (isVisible()) { getView().setPreview(event.getHtml()); }
    }

    @Override
    public void onToggleScrolling(final FinderScrollEvent event) {
        if (isVisible()) { getView().toggleScrolling(event.isEnforceScrolling(), event.getRequiredWidth()); }
    }

    @Override
    public void onClearActiveSelection(final ClearFinderSelectionEvent event) {
        if (isVisible()) { getView().clearActiveSelection(event); }
    }
}
