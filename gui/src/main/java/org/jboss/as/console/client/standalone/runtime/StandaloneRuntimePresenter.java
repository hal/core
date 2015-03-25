package org.jboss.as.console.client.standalone.runtime;

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
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.UnauthorisedPresenter;
import org.jboss.as.console.client.rbac.UnauthorizedEvent;
import org.jboss.as.console.client.shared.model.SubsystemLoader;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.v3.presenter.Finder;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;

import java.util.List;

/**
 * @author Heiko Braun
 */
public class StandaloneRuntimePresenter
        extends Presenter<StandaloneRuntimePresenter.MyView, StandaloneRuntimePresenter.MyProxy>
        implements Finder, UnauthorizedEvent.UnauthorizedHandler, PreviewEvent.Handler {

    private final PlaceManager placeManager;
    private final SubsystemLoader subsysStore;
    private final Header header;
    private final UnauthorisedPresenter unauthorisedPresenter;

    @NoGatekeeper
    @ProxyCodeSplit
    @NameToken(NameTokens.StandaloneRuntimePresenter)
    public interface MyProxy extends Proxy<StandaloneRuntimePresenter>, Place {}

    public interface MyView extends View {
        void setPresenter(StandaloneRuntimePresenter presenter);
        void setSubsystems(List<SubsystemRecord> result);
        void setPreview(final SafeHtml html);
    }

    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent = new GwtEvent.Type<RevealContentHandler<?>>();



    @Inject
    public StandaloneRuntimePresenter(EventBus eventBus, MyView view, MyProxy proxy, PlaceManager placeManager,
            SubsystemLoader subsysStore, Header header, UnauthorisedPresenter unauthorisedPresenter) {

        super(eventBus, view, proxy);
        this.placeManager = placeManager;
        this.subsysStore = subsysStore;
        this.header = header;
        this.unauthorisedPresenter = unauthorisedPresenter;
    }

    @Override
    public void onPreview(PreviewEvent event) {
        getView().setPreview(event.getHtml());
    }

    @Override
    public void onUnauthorized(final UnauthorizedEvent event) {
        // resetLastPlace();
        setInSlot(TYPE_MainContent, unauthorisedPresenter);
    }


    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        getEventBus().addHandler(UnauthorizedEvent.TYPE, this);
        getEventBus().addHandler(PreviewEvent.TYPE, this);
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

        header.highlight(getProxy().getNameToken());
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }
}
