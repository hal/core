package org.jboss.as.console.client.shared.subsys.undertow;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.tools.modelling.workbench.ActivateEvent;
import org.jboss.as.console.client.tools.modelling.workbench.PassivateEvent;
import org.jboss.as.console.client.tools.modelling.workbench.ResetEvent;
import org.jboss.as.console.mbui.Framework;
import org.jboss.as.console.mbui.Kernel;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.useware.kernel.gui.behaviour.NavigationDelegate;
import org.useware.kernel.model.structure.QName;


/**
 * @author Heiko Braun
 * @date 10/25/11
 */
public class CorePresenter extends Presenter<CoreView, CorePresenter.MyProxy>
        implements ActivateEvent.ActivateHandler, ResetEvent.ResetHandler,
        PassivateEvent.PassivateHandler, NavigationDelegate {

    private final Kernel kernel;
    private final RevealStrategy revealStrategy;
    private final UndertowDialogs dialogs;

    @ProxyCodeSplit
    @NameToken(NameTokens.UndertowCore)
    @AccessControl(resources = {
            "{selected.profile}/subsystem=undertow"
    })
    public interface MyProxy extends Proxy<CorePresenter>, Place {
    }

    @Inject
    public CorePresenter(
            final EventBus eventBus,
            final CoreView view,
            final MyProxy proxy,
            final DispatchAsync dispatcher,
            RevealStrategy revealStrategy)
    {
        super(eventBus, view, proxy);

        this.revealStrategy = revealStrategy;

        CoreGUIContext globalContext = new CoreGUIContext(
                Console.MODULES.getCurrentSelectedProfile(),
                Console.MODULES.getCurrentUser(), Console.MODULES.getDomainEntityManager()
        );

        // mbui kernel instance
        this.dialogs = new UndertowDialogs();
        this.kernel = new Kernel(dialogs, new Framework() {
            @Override
            public DispatchAsync getDispatcher() {
                return dispatcher;
            }

            @Override
            public SecurityContext getSecurityContext() {
                return Console.MODULES.getSecurityService().getSecurityContext(
                        Console.MODULES.getPlaceManager().getCurrentPlaceRequest().getNameToken()
                );
            }
        }, globalContext);
    }


    @Override
    public void onNavigation(QName source, QName dialog) {
        System.out.println("Absolute navigation " + source + ">" + dialog);
    }

    @Override
    public void onActivate(ActivateEvent event) {
        kernel.activate();
    }

    @Override
    public void onReset(ResetEvent event) {
        kernel.reset();
    }

    @Override
    public void onPassivate(PassivateEvent event) {
        kernel.passivate();
    }

    @Override
    protected void onBind() {
        super.onBind();

        reify();

        getEventBus().addHandler(ResetEvent.getType(), this);
    }

    private void reify() {
        kernel.reify("Undertow", new AsyncCallback<Widget>() {
            @Override
            public void onFailure(Throwable throwable) {
                Console.error("Reification failed", throwable.getMessage());
            }

            @Override
            public void onSuccess(Widget widget) {

                getView().show(widget);
                kernel.activate();
                kernel.reset();
            }
        });

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
