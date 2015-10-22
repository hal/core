package org.jboss.as.console.client.shared.subsys.generic;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;

/**
 * @author Heiko Braun
 */
public class GenericSubsystemPresenter
        extends Presenter<GenericSubsystemPresenter.MyView, GenericSubsystemPresenter.MyProxy> {

    private String subsystemKey;
    private final static AddressTemplate BASE_ADDRESS = AddressTemplate.of("{selected.profile}/subsystem=*");


    // @formatter:off
    @ProxyCodeSplit
    @NameToken(NameTokens.GenericSubsystem)
    public interface MyProxy extends ProxyPlace<GenericSubsystemPresenter> {}

    public interface MyView extends View {
        void showDetails(ResourceAddress resourceAddress);
    }
    // @formatter:on


    private final RevealStrategy revealStrategy;
    private final CoreGUIContext statementContext;

    @Inject
    public GenericSubsystemPresenter(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final RevealStrategy revealStrategy, CoreGUIContext statementContext) {
        super(eventBus, view, proxy);
        this.revealStrategy = revealStrategy;

        this.statementContext = statementContext;
    }

    @Override
    public void prepareFromRequest(PlaceRequest request) {
        subsystemKey = request.getParameter("key", null);
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    @Override
    protected void onReset() {
        super.onReset();

        if(subsystemKey!=null)
        {
            ResourceAddress address = BASE_ADDRESS.resolve(statementContext, subsystemKey);
            getView().showDetails(address);
        }
    }
}