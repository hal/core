package org.jboss.as.console.client.csp;

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
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;

/**
 * @author Heiko Braun
 * @since 19/08/14
 */
public class CSPPresenter extends Presenter<CSPPresenter.MyView, CSPPresenter.MyProxy> {

    private final PlaceManager placeManager;
    private String angularRef;

    @ProxyCodeSplit
    @NameToken(NameTokens.CSP)
    public interface MyProxy extends Proxy<CSPPresenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(CSPPresenter presenter);

        void setRef(String angularRef);
    }

    @Inject
    public CSPPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            PlaceManager placeManager) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }


    @Override
    public void prepareFromRequest(PlaceRequest request) {
        super.prepareFromRequest(request);

        angularRef = request.getParameter("ref", "search");
    }

    @Override
    protected void onReset() {
        super.onReset();
        getView().setRef(angularRef);
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }
}
