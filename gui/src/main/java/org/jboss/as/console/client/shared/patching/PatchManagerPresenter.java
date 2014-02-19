/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.jboss.as.console.client.shared.patching;

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

/**
 * @author Harald Pehl
 */
public class PatchManagerPresenter extends Presenter<PatchManagerPresenter.MyView, PatchManagerPresenter.MyProxy> {

    @ProxyCodeSplit
    @NameToken(NameTokens.PatchingPresenter)
    @AccessControl(resources = {"/{selected.host}/core-service=patching"}, recursive = false)
    public interface MyProxy extends Proxy<PatchManagerPresenter>, Place {}

    public interface MyView extends View {

        void setPresenter(PatchManagerPresenter presenter);
    }

    private final RevealStrategy revealStrategy;

    @Inject
    public PatchManagerPresenter(final EventBus eventBus, final MyView view, final MyProxy proxy,
            RevealStrategy revealStrategy) {

        super(eventBus, view, proxy);
        this.revealStrategy = revealStrategy;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInRuntimeParent(this);
    }

    public void launchNewPatchWizard() {

    }

    public void onApply(final PatchInfo patchInfo) {

    }

    public void onRollback(final PatchInfo patchInfo) {

    }
}
