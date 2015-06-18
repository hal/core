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

package org.jboss.as.console.client.domain.profiles;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableView;
import org.jboss.as.console.client.domain.events.ProfileSelectionEvent;
import org.jboss.as.console.client.domain.model.ProfileRecord;
import org.jboss.as.console.client.shared.model.LoadProfile;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.shared.model.SubsystemStore;
import org.jboss.as.console.client.v3.presenter.Finder;
import org.jboss.as.console.client.v3.stores.domain.ProfileStore;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshProfiles;
import org.jboss.as.console.client.widgets.nav.v3.ClearFinderSelectionEvent;
import org.jboss.as.console.client.widgets.nav.v3.FinderScrollEvent;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.PropagatesChange;

import java.util.List;

/**
 * @author Heiko Braun
 */
public class ProfileMgmtPresenter
        extends Presenter<ProfileMgmtPresenter.MyView, ProfileMgmtPresenter.MyProxy>
        implements Finder, ProfileSelectionEvent.ProfileSelectionListener, PreviewEvent.Handler,
         ClearFinderSelectionEvent.Handler, FinderScrollEvent.Handler {

    @NoGatekeeper // Toplevel navigation presenter - redirects to default / last place
    @ProxyCodeSplit
    @NameToken(NameTokens.ProfileMgmtPresenter)
    public interface MyProxy extends Proxy<ProfileMgmtPresenter>, Place {}

    public interface MyView extends SuspendableView {
        void setProfiles(List<ProfileRecord> profileRecords);
        void setSubsystems(List<SubsystemRecord> subsystemRecords);
        void setPresenter(ProfileMgmtPresenter presenter);
        void setPreview(SafeHtml html);

        void clearActiveSelection();

        void toogleScrolling(boolean enforceScrolling, int requiredSize);
    }

    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent =
            new GwtEvent.Type<RevealContentHandler<?>>();

    private SubsystemStore subsysStore;
    private final PlaceManager placeManager;
    private ProfileStore profileStore;
    private CurrentProfileSelection profileSelection;
    private final Dispatcher circuit;
    private final Header header;

    @Inject
    public ProfileMgmtPresenter(EventBus eventBus, MyView view, MyProxy proxy, PlaceManager placeManager,
            ProfileStore profileStore, SubsystemStore subsysStore, CurrentProfileSelection currentProfileSelection, Dispatcher circuit, Header header) {

        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.profileStore = profileStore;
        this.subsysStore = subsysStore;
        this.profileSelection = currentProfileSelection;

        this.circuit = circuit;
        this.header = header;
    }

    @Override
    public void onToggleScrolling(FinderScrollEvent event) {
        if(isVisible())
            getView().toogleScrolling(event.isEnforceScrolling(), event.getRequiredWidth());
    }


    @Override
    public void onClearActiveSelection(ClearFinderSelectionEvent event) {
        getView().clearActiveSelection();
    }

    @Override
    protected void onReset() {
        super.onReset();
        header.highlight(getProxy().getNameToken());
    }

    @Override
    protected void onBind() {
        super.onBind();

        getView().setPresenter(this);
        getEventBus().addHandler(ProfileSelectionEvent.TYPE, this);
        getEventBus().addHandler(PreviewEvent.TYPE, this);
        getEventBus().addHandler(ClearFinderSelectionEvent.TYPE, this);
        getEventBus().addHandler(FinderScrollEvent.TYPE, this);

        subsysStore.addChangeHandler(LoadProfile.class, new PropagatesChange.Handler() {
            @Override
            public void onChange(Action action) {
                List<SubsystemRecord> subsystems = subsysStore.getSubsystems(profileSelection.getName());
                getView().setSubsystems(subsystems);
            }
        });

        profileStore.addChangeHandler(new PropagatesChange.Handler() {
            @Override
            public void onChange(Action action) {
                 getView().setProfiles(profileStore.getProfiles());
            }
        });
    }

    public void loadProfiles() {
        circuit.dispatch(new RefreshProfiles());
    }

    @Override
    protected void revealInParent() {
        // reveal in main layout
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }

    @Override
    public void onProfileSelection(String profileName) {
        if(!isVisible()) return;
        profileSelection.setName(profileName);
        circuit.dispatch(new LoadProfile(profileName));
    }

    @Override
    public void onPreview(PreviewEvent event) {
        getView().setPreview(event.getHtml());
    }
}
