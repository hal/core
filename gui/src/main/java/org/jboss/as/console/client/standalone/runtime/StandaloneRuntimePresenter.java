package org.jboss.as.console.client.standalone.runtime;

import com.google.gwt.event.shared.GwtEvent;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.UnauthorisedPresenter;
import org.jboss.as.console.client.rbac.UnauthorizedEvent;
import org.jboss.as.console.client.shared.model.SubsystemLoader;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.shared.state.PerspectivePresenter;

import java.util.List;

/**
 * @author Heiko Braun
 */
public class StandaloneRuntimePresenter
        extends PerspectivePresenter<StandaloneRuntimePresenter.MyView, StandaloneRuntimePresenter.MyProxy> {

    @NoGatekeeper
    @ProxyCodeSplit
    @NameToken(NameTokens.StandaloneRuntimePresenter)
    public interface MyProxy extends ProxyPlace<StandaloneRuntimePresenter> {}

    public interface MyView extends View {
        void setPresenter(StandaloneRuntimePresenter presenter);
        void setSubsystems(List<SubsystemRecord> result);
    }

    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent = new GwtEvent.Type<RevealContentHandler<?>>();

    private final SubsystemLoader subsysStore;

    @Inject
    public StandaloneRuntimePresenter(EventBus eventBus, MyView view, MyProxy proxy, PlaceManager placeManager,
            SubsystemLoader subsysStore, Header header, UnauthorisedPresenter unauthorisedPresenter) {

        super(eventBus, view, proxy, placeManager, header, NameTokens.StandaloneRuntimePresenter, unauthorisedPresenter,
                TYPE_MainContent);

        this.subsysStore = subsysStore;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        getEventBus().addHandler(UnauthorizedEvent.TYPE, this);
    }

    @Override
    protected void onReset() {
        subsysStore.loadSubsystems("default", new SimpleCallback<List<SubsystemRecord>>() {
            @Override
            public void onSuccess(List<SubsystemRecord> result) {
                getView().setSubsystems(result);
                StandaloneRuntimePresenter.super.onReset();
            }
        });
    }

    @Override
    protected void onFirstReveal(final PlaceRequest placeRequest, PlaceManager placeManager, boolean revealDefault) {
        if(revealDefault)
        {
            placeManager.revealPlace(new PlaceRequest.Builder().nameToken(NameTokens.StandaloneServerPresenter).build());
        }
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }
}
