package org.jboss.as.console.client.shared.subsys.undertow;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;

/**
 * @author Heiko Braun
 * @since 05/09/14
 */
public class UndertowPresenter extends Presenter<UndertowPresenter.MyView, UndertowPresenter.MyProxy> {

    private final PlaceManager placeManager;
    private final RevealStrategy revealStrategy;

    @ProxyCodeSplit
    @NameToken(NameTokens.UndertowPresenter)
    public interface MyProxy extends Proxy<UndertowPresenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(UndertowPresenter presenter);
    }

    @Inject
    public UndertowPresenter(EventBus eventBus, MyView view, MyProxy proxy,
                         PlaceManager placeManager, RevealStrategy revealStrategy) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.revealStrategy = revealStrategy;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }


    @Override
    protected void onReset() {
        super.onReset();
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }
}
