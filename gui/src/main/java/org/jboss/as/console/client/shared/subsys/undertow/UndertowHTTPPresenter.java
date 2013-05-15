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
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.tx.model.TransactionManager;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.BeanMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.as.console.spi.SubsystemExtension;


/**
 * @author Heiko Braun
 * @date 10/25/11
 */
public class UndertowHTTPPresenter extends Presenter<UndertowHTTPPresenter.MyView, UndertowHTTPPresenter.MyProxy> {

    private final PlaceManager placeManager;
    private DispatchAsync dispatcher;
    private RevealStrategy revealStrategy;
    private ApplicationMetaData metaData;
    private BeanMetaData beanMetaData ;
    private EntityAdapter<TransactionManager> entityAdapter;

    @ProxyCodeSplit
    @NameToken(NameTokens.TransactionPresenter)
    @SubsystemExtension(name="HTTP Server", group="Undertow", key="undertow")
    public interface MyProxy extends Proxy<UndertowHTTPPresenter>, Place {
    }

    public interface MyView extends View {

    }

    @Inject
    public UndertowHTTPPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            PlaceManager placeManager, DispatchAsync dispatcher,
            RevealStrategy revealStrategy,
            ApplicationMetaData metaData)
    {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;
        this.metaData = metaData;

        this.beanMetaData = metaData.getBeanMetaData(TransactionManager.class);
        this.entityAdapter = new EntityAdapter<TransactionManager>(TransactionManager.class, metaData);
    }


    @Override
    protected void onBind() {
        super.onBind();

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
