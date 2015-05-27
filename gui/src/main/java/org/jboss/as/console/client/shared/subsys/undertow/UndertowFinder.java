package org.jboss.as.console.client.shared.subsys.undertow;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.profiles.ProfileMgmtPresenter;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.standalone.ServerMgmtApplicationPresenter;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.dispatch.DispatchAsync;

/**
 * @author Heiko Braun
 */
public class UndertowFinder extends Presenter<UndertowFinder.MyView, UndertowFinder.MyProxy>
    implements PreviewEvent.Handler {

    @ProxyCodeSplit
    @NameToken(NameTokens.UndertowFinder)
    @RequiredResources(resources = {"{selected.profile}/subsystem=undertow"})
    @SearchIndex(keywords = {"web", "http", "ssl", "jsp"})
    public interface MyProxy extends Proxy<UndertowFinder>, Place {}


    public interface MyView extends View {

        void setPresenter(UndertowFinder presenter);

        void setPreview(SafeHtml html);
    }


    private final PlaceManager placeManager;
    private final RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;

    @Inject
    public UndertowFinder(EventBus eventBus, MyView view, MyProxy proxy, PlaceManager placeManager,
            DispatchAsync dispatcher, RevealStrategy revealStrategy) {

        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.revealStrategy = revealStrategy;
        this.dispatcher = dispatcher;

    }

    @Override
    protected void onBind() {
        super.onBind();
        getEventBus().addHandler(PreviewEvent.TYPE, this);
        getView().setPresenter(this);
    }

    public PlaceManager getPlaceManager() {
        return placeManager;
    }

    @Override
    protected void revealInParent() {
        if(Console.getBootstrapContext().isStandalone())
            RevealContentEvent.fire(this, ServerMgmtApplicationPresenter.TYPE_MainContent, this);
        else
            RevealContentEvent.fire(this, ProfileMgmtPresenter.TYPE_MainContent, this);
    }

    @Override
    public void onPreview(PreviewEvent event) {
        if(isVisible())
            getView().setPreview(event.getHtml());
    }
}
