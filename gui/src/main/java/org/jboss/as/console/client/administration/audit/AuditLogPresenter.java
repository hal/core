package org.jboss.as.console.client.administration.audit;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.dmr.client.dispatch.DispatchAsync;

/**
 * @author Harald Pehl
 * @date 08/12/2013
 */
public class AuditLogPresenter
        extends Presenter<AuditLogPresenter.MyView, AuditLogPresenter.MyProxy> {

    private final DispatchAsync dispatcher;
    private final RevealStrategy revealStrategy;

    @Inject
    public AuditLogPresenter(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final DispatchAsync dispatcher, final RevealStrategy revealStrategy) {
        super(eventBus, view, proxy);

        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInAdministration(this);
    }

    @ProxyCodeSplit
    @NameToken(NameTokens.AuditLogPresenter)
    @AccessControl(resources = {"/core-service=management/access=audit"})
    public interface MyProxy extends Proxy<AuditLogPresenter>, Place {
    }

    public interface MyView extends View {

        void setPresenter(AuditLogPresenter presenter);
    }
}
