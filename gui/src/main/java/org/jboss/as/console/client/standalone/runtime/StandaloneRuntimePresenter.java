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
import org.jboss.as.console.client.rbac.UnauthorizedEvent;
import org.jboss.as.console.client.shared.model.SubsystemLoader;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.v3.presenter.Finder;
import org.jboss.as.console.client.widgets.nav.v3.FinderScrollEvent;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;

import java.util.List;

/**
 * @author Heiko Braun
 */
public class StandaloneRuntimePresenter
        extends Presenter<StandaloneRuntimePresenter.MyView, StandaloneRuntimePresenter.MyProxy>
        implements Finder, PreviewEvent.Handler, FinderScrollEvent.Handler {

    private final PlaceManager placeManager;
    private final SubsystemLoader subsysStore;
    private final Header header;

    @NoGatekeeper
    @ProxyCodeSplit
    @NameToken(NameTokens.StandaloneRuntimePresenter)
    public interface MyProxy extends Proxy<StandaloneRuntimePresenter>, Place {}

    public interface MyView extends View {
        void setPresenter(StandaloneRuntimePresenter presenter);
        void setSubsystems(List<SubsystemRecord> result);
        void setPreview(final SafeHtml html);

        void toggleScrolling(boolean enforceScrolling, int requiredWidth);
    }

    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent = new GwtEvent.Type<RevealContentHandler<?>>();



    @Inject
    public StandaloneRuntimePresenter(EventBus eventBus, MyView view, MyProxy proxy, PlaceManager placeManager,
            SubsystemLoader subsysStore, Header header) {

        super(eventBus, view, proxy);
        this.placeManager = placeManager;
        this.subsysStore = subsysStore;
        this.header = header;
    }

    @Override
    public void onPreview(PreviewEvent event) {
        getView().setPreview(event.getHtml());
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        getEventBus().addHandler(PreviewEvent.TYPE, this);
        getEventBus().addHandler(FinderScrollEvent.TYPE, this);
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

    @Override
    public void onToggleScrolling(FinderScrollEvent event) {
        if(isVisible())
            getView().toggleScrolling(event.isEnforceScrolling(), event.getRequiredWidth());
    }
}
