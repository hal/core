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

package org.jboss.as.console.client.standalone;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.events.ProfileSelectionEvent;
import org.jboss.as.console.client.rbac.UnauthorisedPresenter;
import org.jboss.as.console.client.rbac.UnauthorizedEvent;
import org.jboss.as.console.client.shared.model.LoadProfile;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.shared.model.SubsystemStore;
import org.jboss.as.console.client.v3.presenter.Finder;
import org.jboss.as.console.client.widgets.nav.v3.ClearFinderSelectionEvent;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;

import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.PropagatesChange;

import java.util.List;

/**
 * A collection of tools to manage a standalone server instance.
 *
 * @author Heiko Braun
 */
public class ServerMgmtApplicationPresenter extends
        Presenter<ServerMgmtApplicationPresenter.ServerManagementView, ServerMgmtApplicationPresenter.ServerManagementProxy>
        implements Finder, PreviewEvent.Handler,  UnauthorizedEvent.UnauthorizedHandler, ClearFinderSelectionEvent.Handler  {

    private PlaceManager placeManager;
    private SubsystemStore subsysStore;
    private final UnauthorisedPresenter unauthorisedPresenter;
    private final Dispatcher circuit;

    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent = new GwtEvent.Type<RevealContentHandler<?>>();



    @NoGatekeeper
    @ProxyCodeSplit
    @NameToken(NameTokens.ServerProfile)
    public interface ServerManagementProxy extends ProxyPlace<ServerMgmtApplicationPresenter> {}

    public interface ServerManagementView extends View {
        void updateFrom(List<SubsystemRecord> subsystemRecords);
        void clearActiveSelection();
        void setPreview(final SafeHtml html);

        void setPresenter(ServerMgmtApplicationPresenter presenter);
    }

    @Inject
    public ServerMgmtApplicationPresenter(
            EventBus eventBus, ServerManagementView view,
            ServerManagementProxy proxy, PlaceManager placeManager, SubsystemStore subsysStore, Header header,
            UnauthorisedPresenter unauthorisedPresenter,  Dispatcher circuit) {

        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.subsysStore = subsysStore;

        this.unauthorisedPresenter = unauthorisedPresenter;
        this.circuit = circuit;
    }

    @Override
    protected void onBind() {

        getView().setPresenter(this);
        getEventBus().addHandler(ProfileSelectionEvent.TYPE, this);
        getEventBus().addHandler(PreviewEvent.TYPE, this);
        getEventBus().addHandler(ClearFinderSelectionEvent.TYPE, this);

        subsysStore.addChangeHandler(LoadProfile.class, new PropagatesChange.Handler() {
            @Override
            public void onChange(Action action) {
                List<SubsystemRecord> subsystems = subsysStore.getSubsystems("default");
                getView().updateFrom(subsystems);
            }
        });
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }

    @Override
    public void onPreview(PreviewEvent event) {
        getView().setPreview(event.getHtml());
    }

    /**
     * Sets the {@link org.jboss.as.console.client.rbac.UnauthorisedPresenter} in the content slot given as constructor
     * parameter.
     */
    @Override
    public void onUnauthorized(final UnauthorizedEvent event) {
        // resetLastPlace();
        setInSlot(TYPE_MainContent, unauthorisedPresenter);
    }

    @Override
    public void onClearActiveSelection(ClearFinderSelectionEvent event) {
        getView().clearActiveSelection();
    }

    public void loadSubsystems() {
        circuit.dispatch(new LoadProfile("default"));
    }
}
