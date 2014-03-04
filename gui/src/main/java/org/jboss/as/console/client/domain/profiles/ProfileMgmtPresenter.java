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

import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
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
import org.jboss.as.console.client.core.SuspendableView;
import org.jboss.as.console.client.domain.events.ProfileSelectionEvent;
import org.jboss.as.console.client.domain.model.ProfileRecord;
import org.jboss.as.console.client.domain.model.ProfileStore;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.UnauthorisedPresenter;
import org.jboss.as.console.client.shared.SubsystemMetaData;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.shared.model.SubsystemStore;
import org.jboss.as.console.client.shared.state.PerspectivePresenter;

/**
 * @author Heiko Braun
 */
public class ProfileMgmtPresenter
        extends PerspectivePresenter<ProfileMgmtPresenter.MyView, ProfileMgmtPresenter.MyProxy>
        implements ProfileSelectionEvent.ProfileSelectionListener {

    @NoGatekeeper // Toplevel navigation presenter - redirects to default / last place
    @ProxyCodeSplit
    @NameToken(NameTokens.ProfileMgmtPresenter)
    public interface MyProxy extends Proxy<ProfileMgmtPresenter>, Place {}

    public interface MyView extends SuspendableView {
        void setProfiles(List<ProfileRecord> profileRecords);
        void setSubsystems(List<SubsystemRecord> subsystemRecords);
        void setPreselection(String preselection);
    }

    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent =
            new GwtEvent.Type<RevealContentHandler<?>>();

    private SubsystemStore subsysStore;
    private final PlaceManager placeManager;
    private ProfileStore profileStore;
    private CurrentProfileSelection profileSelection;

    @Inject
    public ProfileMgmtPresenter(EventBus eventBus, MyView view, MyProxy proxy, PlaceManager placeManager,
            ProfileStore profileStore, SubsystemStore subsysStore, CurrentProfileSelection currentProfileSelection,
            Header header, UnauthorisedPresenter unauthorisedPresenter) {

        super(eventBus, view, proxy, placeManager, header, NameTokens.ProfileMgmtPresenter, unauthorisedPresenter,
                TYPE_MainContent);

        this.placeManager = placeManager;
        this.profileStore = profileStore;
        this.subsysStore = subsysStore;
        this.profileSelection = currentProfileSelection;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getEventBus().addHandler(ProfileSelectionEvent.TYPE, this);
    }

    @Override
    protected void onFirstReveal(final PlaceRequest placeRequest, PlaceManager placeManager) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                loadProfiles();
            }
        });
        subsysStore.loadSubsystems(profileSelection.getName(), new SimpleCallback<List<SubsystemRecord>>() {
            @Override
            public void onSuccess(List<SubsystemRecord> existingSubsystems) {
                revealDefaultSubsystem(preferredPlace(), existingSubsystems);
            }
        });
    }

    private void revealDefaultSubsystem(PlaceRequest preference, List<SubsystemRecord> existingSubsystems) {
        final String[] defaultSubsystem = SubsystemMetaData
                .getDefaultSubsystem(preference.getNameToken(), existingSubsystems);
        Log.debug("reveal default subsystem : pref " + preference + "; chosen " + defaultSubsystem[1]);
        placeManager.revealPlace(new PlaceRequest.Builder().nameToken(defaultSubsystem[1]).build());
    }

    @Override
    public void prepareFromRequest(PlaceRequest request) {
        super.prepareFromRequest(request);

        final String preselection = request.getParameter("profile", null);
        if (preselection != null) {
            getView().setPreselection(preselection);
            profileSelection.setName(preselection);
            resetLastPlace();
        }
    }

    private void loadProfiles() {
        profileStore.loadProfiles(new SimpleCallback<List<ProfileRecord>>() {
            @Override
            public void onSuccess(final List<ProfileRecord> result) {
                getView().setProfiles(result);
            }
        });
    }

    @Override
    protected void revealInParent() {
        // reveal in main layout
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }

    @Override
    public void onProfileSelection(String profileName) {
        assert profileName!=null && !profileName.equals("") : "illegal profile name: "+profileName;
        if(!isVisible()) return;

        Log.debug("onProfileSelection: "+profileName + "/ "+placeManager.getCurrentPlaceRequest().getNameToken());
        profileSelection.setName(profileName);
        subsysStore.loadSubsystems(profileName, new SimpleCallback<List<SubsystemRecord>>() {
            @Override
            public void onSuccess(List<SubsystemRecord> result) {
                getView().setSubsystems(result);

                // prefer to reveal the last place, if exists in selected profile
                PlaceRequest preference = getLastPlace() != null ? getLastPlace() : preferredPlace();
                revealDefaultSubsystem(preference, result);
            }
        });
    }

    private PlaceRequest preferredPlace() {
        return new PlaceRequest.Builder().nameToken(NameTokens.DataSourcePresenter).build();
    }
}
