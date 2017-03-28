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

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.ApplicationProperties;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.tools.DownloadUtil;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRHandler;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import static org.jboss.dmr.client.ModelDescriptionConstants.BROWSE_CONTENT;
import static org.jboss.dmr.client.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.dmr.client.ModelDescriptionConstants.RESULT;

/**
 * @author Claudio Miranda
 */
public class DeploymentBrowseContentPresenter
        extends Presenter<DeploymentBrowseContentPresenter.MyView, DeploymentBrowseContentPresenter.MyProxy> {

    // @formatter:off
    @ProxyCodeSplit
    @NameToken(NameTokens.DeploymentBrowseContent)
    public interface MyProxy extends ProxyPlace<DeploymentBrowseContentPresenter> {}

    public interface MyView extends View {
        void browseContent(List<ModelNode> contentItems);
        void setPresenter(DeploymentBrowseContentPresenter presenter);}
    // @formatter:on


    private final RevealStrategy revealStrategy;
    private final DeploymentStore deploymentStore;
    private final DispatchAsync dispatcher;
    private final BootstrapContext bootstrap;
    private String deploymentName;

    @Inject
    public DeploymentBrowseContentPresenter(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final RevealStrategy revealStrategy, final DeploymentStore deploymentStore,
            final DispatchAsync dispatcher, BootstrapContext bootstrap) {
        super(eventBus, view, proxy);

        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;
        this.deploymentStore = deploymentStore;
        this.bootstrap = bootstrap;
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    @Override
    public void prepareFromRequest(final PlaceRequest request) {
        deploymentName = request.getParameter(DEPLOYMENT, null);
        ResourceAddress address = deploymentStore.getSelectedDeploymentAddress();
        if (address != null)
            deploymentName = address.get(0).get(DEPLOYMENT).asString();
    }

    @Override
    protected void onReset() {
        super.onReset();
        getView().setPresenter(this);
        browseContent();
    }

    private void browseContent() {
        Operation operation = new Operation.Builder(BROWSE_CONTENT, new ResourceAddress().add("deployment", deploymentName))
                .build();

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                Console.error(Console.CONSTANTS.unableToReadDeployment(), caught.getMessage());
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (result.isFailure()) {
                    Console.error(Console.CONSTANTS.unableToReadDeployment(), result.getFailureDescription());
                } else {
                    List<ModelNode> res = result.get(RESULT).asList();
                    getView().browseContent(res);

                }
            }
        });
    }

    public void downloadFile(String filepath) {
        String filename = filepath.substring(filepath.lastIndexOf("/") + 1);
        String url = streamUrl(deploymentName, filepath);
        DownloadUtil.downloadHttpGet(url, filename, DMRHandler.getBearerToken());
    }

    private String streamUrl(final String deploymentName, String filepath) {
        StringBuilder url = new StringBuilder();
        url.append(bootstrap.getProperty(ApplicationProperties.DOMAIN_API)).append("/");
        url.append("deployment/").append(deploymentName).append("?operation=read-content&path=" + filepath + "&useStreamAsResponse");
        return url.toString();
    }

}
