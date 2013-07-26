package org.jboss.as.console.client.administration;

import com.google.gwt.event.shared.GwtEvent;
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
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
import org.jboss.as.console.client.core.Header;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;


/**
 * @author Harald Pehl
 * @date 07/25/2013
 */
public class AdministrationPresenter
        extends Presenter<AdministrationPresenter.MyView, AdministrationPresenter.MyProxy> {

    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent = new GwtEvent.Type<RevealContentHandler<?>>();
    private final PlaceManager placeManager;
    private boolean hasBeenRevealed;
    private String lastPlace;
    private Header header;

    @Inject
    public AdministrationPresenter(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final PlaceManager placeManager, final Header header) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.header = header;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        header.highlight(NameTokens.AdministrationPresenter);

        // chose sub place to reveal
        String currentToken = placeManager.getCurrentPlaceRequest().getNameToken();
        if (!currentToken.equals(getProxy().getNameToken())) {
            lastPlace = currentToken;
        } else if (lastPlace != null) {
            placeManager.revealPlace(new PlaceRequest.Builder().nameToken(lastPlace).build());
        }

        // first request, select default contents
        if (!hasBeenRevealed) {
            placeManager.revealPlace(new PlaceRequest.Builder().nameToken(NameTokens.RoleAssignmentPresenter).build());
            hasBeenRevealed = true;
        }
    }

    @Override
    protected void revealInParent() {
        // reveal in main layout
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }

    @NoGatekeeper // Toplevel navigation presenter - redirects to default / last place
    @ProxyCodeSplit
    @NameToken(NameTokens.AdministrationPresenter)
    public interface MyProxy extends Proxy<AdministrationPresenter>, Place {
    }

    public interface MyView extends View {

        void setPresenter(AdministrationPresenter presenter);
    }
}
