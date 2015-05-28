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

package org.jboss.as.console.client.core;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ContentSlot;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.NoGatekeeper;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.client.proxy.RevealContentHandler;
import com.gwtplatform.mvp.client.proxy.RevealRootLayoutContentEvent;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.rbac.UnauthorisedPresenter;
import org.jboss.as.console.client.rbac.UnauthorizedEvent;
import org.jboss.as.console.client.shared.expr.ExpressionResolver;
import org.jboss.as.console.client.shared.expr.ExpressionTool;
import org.jboss.as.console.client.widgets.nav.v3.CloseApplicationEvent;
import org.jboss.ballroom.client.widgets.forms.ResolveExpressionEvent;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;

/**
 * @author Heiko Braun
 * @date 2/4/11
 */
public class MainLayoutPresenter
        extends Presenter<MainLayoutPresenter.MainLayoutView,
        MainLayoutPresenter.MainLayoutProxy>
        implements ResolveExpressionEvent.ExpressionResolveListener, LogoutEvent.LogoutHandler, CloseApplicationEvent.Handler,
        UnauthorizedEvent.UnauthorizedHandler {

    boolean revealDefault = true;
    private BootstrapContext bootstrap;
    private final UnauthorisedPresenter unauthorisedPresenter;

    private ExpressionTool expressionTool;

    private PlaceManager placeManager;

    public interface MainLayoutView extends View {
        void setPresenter(MainLayoutPresenter presenter);

        void closeApplication();
    }

    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_MainContent = new GwtEvent.Type<RevealContentHandler<?>>();

    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_Popup = new GwtEvent.Type<RevealContentHandler<?>>();

    @ContentSlot
    public static final GwtEvent.Type<RevealContentHandler<?>> TYPE_Hidden = new GwtEvent.Type<RevealContentHandler<?>>();

    @ProxyCodeSplit
    @NameToken(NameTokens.mainLayout)
    @NoGatekeeper
    public interface MainLayoutProxy extends ProxyPlace<MainLayoutPresenter> {}

    @Inject
    public MainLayoutPresenter(
            EventBus eventBus,
            MainLayoutView view,
            MainLayoutProxy proxy, BootstrapContext bootstrap,
            ExpressionResolver resolver, PlaceManager placeManager, UnauthorisedPresenter unauthorisedPresenter) {
        super(eventBus, view, proxy);
        this.bootstrap = bootstrap;
        this.unauthorisedPresenter = unauthorisedPresenter;
        this.expressionTool = new ExpressionTool(resolver);
        this.placeManager = placeManager;

    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        getEventBus().addHandler(ResolveExpressionEvent.TYPE, this);
        getEventBus().addHandler(LogoutEvent.TYPE, this);
        getEventBus().addHandler(CloseApplicationEvent.TYPE, this);
        getEventBus().addHandler(UnauthorizedEvent.TYPE, this);

    }

    @Override
    public void onUnauthorized(UnauthorizedEvent event) {
        DefaultWindow window = new DefaultWindow("Insufficient Privileges");
        window.setWidget(unauthorisedPresenter);
        window.setWidth(320);
        window.setHeight(240);
        window.setAutoHideOnHistoryEventsEnabled(true);
        window.setGlassEnabled(true);
        window.center();
    }

    @Override
    protected void revealInParent() {
        RevealRootLayoutContentEvent.fire(this, this);
    }

    @Override
    public void onResolveExpressionEvent(String expr) {
        expressionTool.launch();
        expressionTool.resolve(expr);

    }

    @Override
    public void onCloseApplication(CloseApplicationEvent event) {
        getView().closeApplication();
    }

    @Override
    public void onLogout(LogoutEvent event) {
        RootLayoutPanel.get().clear();
    }
}
