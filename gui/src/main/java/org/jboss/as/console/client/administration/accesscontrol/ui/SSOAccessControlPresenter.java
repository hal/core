/*
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.console.client.administration.accesscontrol.ui;

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
import org.jboss.as.console.client.v3.presenter.Finder;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class SSOAccessControlPresenter extends
        Presenter<SSOAccessControlPresenter.MyView, SSOAccessControlPresenter.MyProxy> 
        implements Finder {

    @ProxyCodeSplit
    @NameToken(NameTokens.SSOAccessControlFinder)
    public interface MyProxy extends Proxy<SSOAccessControlPresenter>, Place {
    }

    public interface MyView extends View {
    }

    private RevealStrategy revealStrategy;

    @Inject
    public SSOAccessControlPresenter(EventBus eventBus, MyView view, MyProxy proxy,RevealStrategy revealStrategy) {
        super(eventBus, view, proxy);
        this.revealStrategy = revealStrategy;
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

    @Override
    public FinderColumn.FinderId getFinderId() {
        return FinderColumn.FinderId.ACCESS_CONTROL;
    }
}
