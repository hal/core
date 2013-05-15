package org.jboss.as.console.client.shared.subsys.undertow;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.ProxyStandard;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.tools.modelling.workbench.ActivateEvent;
import org.jboss.as.console.client.tools.modelling.workbench.PassivateEvent;
import org.jboss.as.console.client.tools.modelling.workbench.ReifyEvent;
import org.jboss.as.console.client.tools.modelling.workbench.ResetEvent;
import org.jboss.as.console.client.tools.modelling.workbench.repository.SampleRepository;
import org.jboss.as.console.mbui.DialogRepository;
import org.jboss.as.console.mbui.Framework;
import org.jboss.as.console.mbui.Kernel;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.tx.model.TransactionManager;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.BeanMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.as.console.spi.SubsystemExtension;
import org.useware.kernel.gui.behaviour.NavigationDelegate;
import org.useware.kernel.model.structure.QName;


/**
 * @author Heiko Braun
 * @date 10/25/11
 */
public class UndertowServletPresenter extends Presenter<SimpleView, UndertowServletPresenter.MyProxy>
        implements ActivateEvent.ActivateHandler, ResetEvent.ResetHandler,
        PassivateEvent.PassivateHandler, NavigationDelegate {

    private final Kernel kernel;
    private final RevealStrategy revealStrategy;
    private final UndertowDialogs dialogs;

    @ProxyCodeSplit
    @NameToken("undertow-servlet")
    @SubsystemExtension(name="Servlets", group="Web", key="undertow")
    public interface MyProxy extends Proxy<UndertowServletPresenter>, Place {
    }

    @Inject
    public UndertowServletPresenter(
            final EventBus eventBus,
            final SimpleView view,
            final MyProxy proxy,
            final DispatchAsync dispatcher,
            RevealStrategy revealStrategy)
    {
        super(eventBus, view, proxy);

        this.revealStrategy = revealStrategy;

        CoreGUIContext globalContext = new CoreGUIContext(
                Console.MODULES.getCurrentSelectedProfile(),
                Console.MODULES.getCurrentUser()
        );

        // mbui kernel instance
        this.dialogs = new UndertowDialogs();
        this.kernel = new Kernel(dialogs, new Framework() {
            @Override
            public DispatchAsync getDispatcher() {
                return dispatcher;
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
        try {
            kernel.reify("Servlet Container", new AsyncCallback<Widget>() {
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
        } catch (Exception e) {
            Log.error("Reification failed", e);
        }
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
