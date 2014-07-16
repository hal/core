package org.jboss.as.console.client.domain.runtime;

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
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.v3.stores.domain.ServerStore;

/**
 * @author Heiko Braun
 * @since 16/07/14
 */
public class NoServerPresenter extends Presenter<NoServerPresenter.MyView, NoServerPresenter.MyProxy> {

    private RevealStrategy revealStrategy;

    @ProxyCodeSplit
    @NameToken(NameTokens.NoServer)
    public interface MyProxy extends Proxy<NoServerPresenter>, Place {
    }

    public interface MyView extends View {
    }

    @Inject
    public NoServerPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            RevealStrategy revealStrategy, ServerStore serverStore, PlaceManager placeManager) {

        super(eventBus, view, proxy);

        this.revealStrategy = revealStrategy;
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }

}
