/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.console.client.tools.modelling.workbench.preview;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import com.gwtplatform.mvp.client.annotations.ProxyStandard;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.tools.modelling.workbench.ActivateEvent;
import org.jboss.as.console.client.tools.modelling.workbench.ApplicationPresenter;
import org.jboss.as.console.client.tools.modelling.workbench.InstrumentEvent;
import org.jboss.as.console.client.tools.modelling.workbench.PassivateEvent;
import org.jboss.as.console.client.tools.modelling.workbench.ReifyEvent;
import org.jboss.as.console.client.tools.modelling.workbench.ResetEvent;
import org.jboss.as.console.client.tools.modelling.workbench.repository.SampleRepository;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.useware.kernel.gui.behaviour.NavigationDelegate;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.Framework;
import org.jboss.as.console.mbui.Kernel;
import org.useware.kernel.model.structure.QName;

import static org.jboss.as.console.client.tools.modelling.workbench.NameTokens.preview;

/**
 * @author Harald Pehl
 * @author Heiko Braun
 * @date 10/30/2012
 */
public class PreviewPresenter extends Presenter<PreviewPresenter.MyView, PreviewPresenter.MyProxy>
        implements ReifyEvent.ReifyHandler, ActivateEvent.ActivateHandler, ResetEvent.ResetHandler,
        PassivateEvent.PassivateHandler, InstrumentEvent.InstrumentHandler, NavigationDelegate
{
    private final Kernel kernel;

    @Inject
    public PreviewPresenter(
            final EventBus eventBus,
            final MyView view,
            final MyProxy proxy,
            final DispatchAsync dispatcher,
            final SampleRepository sampleRepository)
    {
        super(eventBus, view, proxy);

        CoreGUIContext globalContext = new CoreGUIContext(
                Console.MODULES.getCurrentSelectedProfile(),
                Console.MODULES.getCurrentUser(), Console.MODULES.getDomainEntityManager()
        );

        // mbui kernel instance
        this.kernel = new Kernel(sampleRepository, new Framework() {
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
    public void onInstrument(InstrumentEvent event) {
        switch (event.getSingal())
        {
            case ENABLE_CACHE:
                kernel.setCaching(true);
                break;
            case DISABLE_CACHE:
                kernel.setCaching(false);
                break;
        }
    }

    @Override
    public void onNavigation(QName source, QName target) {
        System.out.println("absolute navigation " + source + ">" + target);
    }

    @Override
    protected void onBind()
    {
        super.onBind();
        getEventBus().addHandler(ReifyEvent.getType(), this);
        getEventBus().addHandler(ResetEvent.getType(), this);
        getEventBus().addHandler(ActivateEvent.getType(), this);
        getEventBus().addHandler(PassivateEvent.getType(), this);
        getEventBus().addHandler(InstrumentEvent.getType(), this);
    }

    @Override
    protected void revealInParent()
    {
        RevealContentEvent.fire(this, ApplicationPresenter.TYPE_SetMainContent, this);
    }

    // in real this would be wired to Presenter.onBind()
    @Override
    public void onReify(final ReifyEvent event)
    {
        kernel.reify(event.getSample().getName(), new AsyncCallback<Widget>() {
            @Override
            public void onFailure(Throwable throwable) {
                Console.error("Reification failed", throwable.getMessage());
            }

            @Override
            public void onSuccess(Widget widget) {

                getView().show(widget);
                //kernel.activate();
                kernel.reset();
            }
        });
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

    public interface MyView extends View
    {
        void show(Widget widget);
    }

    @ProxyStandard
    @NameToken(preview)
    @NoGatekeeper
    public interface MyProxy extends ProxyPlace<PreviewPresenter>
    {
    }
}
