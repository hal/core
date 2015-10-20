/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.v3.deployment;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;

/**
 * @author Harald Pehl
 */
public class DeploymentDetailsPresenter
        extends Presenter<DeploymentDetailsPresenter.MyView, DeploymentDetailsPresenter.MyProxy> {

    // @formatter:off
    @ProxyCodeSplit
    @NameToken(NameTokens.DeploymentDetails)
    public interface MyProxy extends ProxyPlace<DeploymentDetailsPresenter> {}

    public interface MyView extends View {
        void showDetails(ResourceAddress resourceAddress);
    }
    // @formatter:on


    private final RevealStrategy revealStrategy;
    private final DeploymentStore deploymentStore;

    @Inject
    public DeploymentDetailsPresenter(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final RevealStrategy revealStrategy, final DeploymentStore deploymentStore) {
        super(eventBus, view, proxy);
        this.revealStrategy = revealStrategy;
        this.deploymentStore = deploymentStore;
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        ResourceAddress address = deploymentStore.getSelectedDeploymentAddress();
        if (address != null) {
            getView().showDetails(address);
        }
    }
}
