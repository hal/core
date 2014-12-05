package org.jboss.as.console.client.shared;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.ManualRevealPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.mbui.DialogRepository;
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
public class DialogPresenter extends ManualRevealPresenter<DialogView, DialogPresenter.MyProxy> {

    private final Kernel kernel;
    private final RevealStrategy revealStrategy;
    private final DialogRepository dialogs;
    private String dialog;

    @ProxyCodeSplit
    @NameToken(NameTokens.DialogPresenter)
    @NoGatekeeper
    public interface MyProxy extends ProxyPlace<DialogPresenter> {
    }

    @Inject
    public DialogPresenter(
            final EventBus eventBus,
            final DialogView view,
            final MyProxy proxy,
            final DispatchAsync dispatcher,
            RevealStrategy revealStrategy, CoreGUIContext globalContext)
    {
        super(eventBus, view, proxy);

        this.revealStrategy = revealStrategy;


        // mbui kernel instance
        this.dialogs = new RemoteRepository();
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

        HTML blank = new HTML("<center><i style='font-size:140px;color:#cccccc;' class='icon-cogs'></i><p style='font-size:24px;color:#cccccc;'>Generating interface ...</p></center>");
        blank.getElement().setAttribute("style", "padding-top:150px;");
        blank.setStyleName("fill-layout-width");

        getView().show(blank); // clear view
        reify();
    }

    @Override
    protected void withRequest(PlaceRequest request) {
        super.prepareFromRequest(request);
        String name = request.getParameter("dialog", null);
        if(null==name)
        {
            Window.alert(("Parameter dialog is missing"));
            throw new RuntimeException("Parameter dialog is missing");
        }

        dialog = name.replace("_", "/"); // workaround for URL parameters


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
