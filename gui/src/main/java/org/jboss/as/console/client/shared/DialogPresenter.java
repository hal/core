package org.jboss.as.console.client.shared;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.mbui.Framework;
import org.jboss.as.console.mbui.Kernel;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.dmr.client.dispatch.DispatchAsync;


/**
 * General purpose dialog presenter.
 *
 * @author Heiko Braun
 * @date 10/25/11
 */
public class DialogPresenter extends Presenter<DialogView, DialogPresenter.MyProxy> {

    private final Kernel kernel;
    private final RevealStrategy revealStrategy;
    private final CommonDialogs dialogs;
    private String dialog;

    @ProxyCodeSplit
    @NameToken(NameTokens.DialogPresenter)
    @NoGatekeeper
    public interface MyProxy extends Proxy<DialogPresenter>, Place {
    }

    @Inject
    public DialogPresenter(
            final EventBus eventBus,
            final DialogView view,
            final MyProxy proxy,
            final DispatchAsync dispatcher,
            RevealStrategy revealStrategy)
    {
        super(eventBus, view, proxy);

        this.revealStrategy = revealStrategy;

        CoreGUIContext globalContext = new CoreGUIContext(
                Console.MODULES.getCurrentSelectedProfile(),
                Console.MODULES.getCurrentUser() , Console.MODULES.getDomainEntityManager()
        );

        // mbui kernel instance
        this.dialogs = new CommonDialogs();
        this.kernel = new Kernel(dialogs, new Framework() {
            @Override
            public DispatchAsync getDispatcher() {
                return dispatcher;
            }

            @Override
            public SecurityFramework getSecurityFramework() {
                return Console.MODULES.getSecurityFramework();
            }
        }, globalContext);
    }

    @Override
    protected void onReset() {         // presenter API
        getView().show(new HTML("")); // clear view
        reify();
    }

    @Override
    public void prepareFromRequest(PlaceRequest request) {
        super.prepareFromRequest(request);
        dialog = request.getParameter("dialog", null);
        if(null==dialog)
        {
            System.out.println("Parameter dialog is missing");
            throw new RuntimeException("Parameter dialog is missing");
        }
    }


    private void reify() {
        kernel.reify(dialog, new AsyncCallback<Widget>() {
            @Override
            public void onFailure(Throwable throwable) {
                Console.error("Reification failed ("+dialog+")", throwable.getMessage());
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
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }
}
