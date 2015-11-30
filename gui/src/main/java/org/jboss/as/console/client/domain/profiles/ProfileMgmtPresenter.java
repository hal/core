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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableView;
import org.jboss.as.console.client.core.UIMessages;
import org.jboss.as.console.client.domain.events.ProfileSelectionEvent;
import org.jboss.as.console.client.domain.model.ProfileRecord;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.shared.model.LoadProfile;
import org.jboss.as.console.client.shared.model.SubsystemReference;
import org.jboss.as.console.client.shared.model.SubsystemStore;
import org.jboss.as.console.client.v3.presenter.Finder;
import org.jboss.as.console.client.v3.stores.domain.ProfileStore;
import org.jboss.as.console.client.v3.stores.domain.ServerGroupStore;
import org.jboss.as.console.client.v3.stores.domain.actions.CloneProfile;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshProfiles;
import org.jboss.as.console.client.v3.stores.domain.actions.RemoveProfile;
import org.jboss.as.console.client.widgets.nav.v3.ClearFinderSelectionEvent;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.FinderScrollEvent;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
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

    private DefaultWindow window;

    @ProxyCodeSplit
    @NameToken(NameTokens.ProfileMgmtPresenter)
    @RequiredResources (
            resources = {
                    "/profile=*"
            },
            recursive = false
    )
    public interface MyProxy extends Proxy<ProfileMgmtPresenter>, Place {}

    public interface MyView extends SuspendableView {
        void setProfiles(List<ProfileRecord> profileRecords);
        void setSubsystems(List<SubsystemReference> subsystemRecords);
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
    private final ServerGroupStore serverGroupStore;

    @Inject
    public ProfileMgmtPresenter(EventBus eventBus, MyView view, MyProxy proxy, PlaceManager placeManager,
                                ProfileStore profileStore, SubsystemStore subsysStore, CurrentProfileSelection currentProfileSelection,
                                Dispatcher circuit, Header header, ServerGroupStore serverGroupStore) {

        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.profileStore = profileStore;
        this.subsysStore = subsysStore;
        this.profileSelection = currentProfileSelection;

        this.circuit = circuit;
        this.header = header;
        this.serverGroupStore = serverGroupStore;
    }

    @Override
    public FinderColumn.FinderId getFinderId() {
        return FinderColumn.FinderId.CONFIGURATION;
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
                List<SubsystemReference> actualSubsystems = subsysStore.getActualSubsystems(profileSelection.getName());
                getView().setSubsystems(actualSubsystems);
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

    public void onCloneProfile(ProfileRecord profileRecord) {
        window = new DefaultWindow(Console.MESSAGES.createTitle("Clone Profile"));
        window.setWidth(480);
        window.setHeight(360);

        window.trapWidget(
                new CloneProfileWizard(this, profileRecord).asWidget()
        );

        window.setGlassEnabled(true);
        window.center();
    }

    public boolean doesExist(String profile) {
        return profileStore.getProfile(profile)!=null;
    }

    public void onSaveClonedProfile(ProfileRecord from, ProfileRecord to) {
        window.hide();
        circuit.dispatch(new CloneProfile(from.getName(), to.getName()));
    }

    public void  onRemoveProfile(ProfileRecord profileRecord) {
        ServerGroupRecord inUseBy = null;
        for (ServerGroupRecord serverGroup : serverGroupStore.getServerGroups()) {
            if(serverGroup.getProfileName().equals(profileRecord.getName())) {
                inUseBy = serverGroup;
                break;
            }
        }

        if(inUseBy!=null)
            Console.error(Console.MESSAGES.profileUsedBy(inUseBy.getName()));
        else
            circuit.dispatch(new RemoveProfile(profileRecord.getName()));

    }

    public void closeDialogue() {
        window.hide();
    }

    public PlaceManager getPlaceManager() {
        return placeManager;
    }
}
