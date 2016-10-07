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

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;
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
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import static com.google.gwt.http.client.URL.encode;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

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

    private String managementUrl;

    @Inject
    public DeploymentBrowseContentPresenter(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final RevealStrategy revealStrategy, final DeploymentStore deploymentStore,
            final DispatchAsync dispatcher, BootstrapContext bootstrap) {
        super(eventBus, view, proxy);
        
        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;
        this.deploymentStore = deploymentStore;
        this.bootstrap = bootstrap;
        managementUrl = bootstrap.getProperty(ApplicationProperties.DOMAIN_API) + "/";
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    static java.util.logging.Logger _log = java.util.logging.Logger.getLogger("org.jboss");

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

        if (filepath != null) {

            Operation operation = new Operation.Builder(READ_CONTENT_OPERATION, new ResourceAddress().add("deployment", deploymentName))
                    .param(PATH, filepath)
                    .build();

            dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
                @Override
                public void onFailure(final Throwable caught) {
                    Console.error(Console.CONSTANTS.unableToReadDeployment(), caught.getMessage());
                }

                @Override
                public void onSuccess(final DMRResponse dmrResponse) {
                    ModelNode result = dmrResponse.get();
                    if (result.isFailure()) {
                        Console.error(Console.CONSTANTS.unableToReadDeployment(), result.getFailureDescription());
                    } else {

                        //_log.info("  read-content: " + result.asString());
                        String mimeType = result.get("response-headers").get("attached-streams").get(0).get("mime-type").asString();
                        //_log.info("  read-content mime-type: " + mimeType);

                        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, encode(managementUrl));
                        requestBuilder.setHeader("Accept", "application/dmr-encoded");
                        requestBuilder.setHeader("Content-Type", "application/json");
                        requestBuilder.setHeader("org.wildfly.useStreamAsResponse", "0");
                        requestBuilder.setIncludeCredentials(true);

                        ResourceAddress deployAddress = new ResourceAddress();
                        deployAddress.add(DEPLOYMENT, deploymentName);

                        Operation op = new Operation.Builder(READ_CONTENT_OPERATION, deployAddress)
                                .param(PATH, filepath)
                                .build();
                        try {
                            requestBuilder.sendRequest(op.toBase64String(), new RequestCallback() {
                                @Override
                                public void onResponseReceived(Request request, final Response response) {
                                    if (response.getStatusCode() >= 400) {
                                        Console.error("Failed to download file " +
                                                filepath + " from deployment " + deploymentName + ". HTTP status code: "
                                                + response.getStatusCode() + ". Message: "
                                                + response.getStatusText());
                                    } else {
                                        //_log.info(" file 1: " + response.getText());
                                        //openPrintWindow("<pre>" + SafeHtmlUtils.htmlEscape(response.getText()) + "</pre>");
                                        String filename = filepath.substring(filepath.lastIndexOf("/") + 1);
                                        downloadFile2(filename, response.getText(), mimeType);
                                    }
                                }
                                @Override
                                public void onError(Request request, Throwable exception) {
                                    Console.error("Failed to download file " + filepath + " from deployment " + deploymentName + ". " + exception.getMessage());
                                }
                            });
                        } catch (RequestException e) {
                            Console.error("Failed to download file " + filepath + " from deployment " + deploymentName + ". " + e.getMessage());
                        }
                    }
                }
            });
        }
    }
    public void downloadFileGET(String filepath) {

        //_log.info("  deploystore selected: " + deploymentName);
        Window.open(streamUrl(deploymentName, filepath), "arquivao", "");
    }

    private String streamUrl(final String deploymentName, String filepath) {
        StringBuilder url = new StringBuilder();
        url.append(bootstrap.getProperty(ApplicationProperties.DOMAIN_API)).append("/");
        url.append("deployment/").append(deploymentName).append("?operation=read-content&path=" + filepath + "&useStreamAsResponse");
        return url.toString();
    }


    native void openPrintWindow(String contents) /*-{
        var printWindow = window.open("", "");
        if (printWindow && printWindow.top) {
            printWindow.document.write(contents);
        } else {
            alert("The print feature works by opening a popup window, but our popup window was blocked by your browser.  If you can disable the blocker temporarily, you'll be able to print here.  Sorry!");
        }
    }-*/;
    
    // encodeURIComponent(
    private native void downloadFile2(String filename, String content, String mime) /*-{
        var pom = document.createElement('a');
        pom.setAttribute('href', 'data:' + mime + ';charset=utf-8,' + encodeURIComponent(content));
        pom.setAttribute('download', filename);
    
        if (document.createEvent) {
            var event = document.createEvent('MouseEvents');
            event.initEvent('click', true, true);
            pom.dispatchEvent(event);
        }
        else {
            pom.click();
        }
    }-*/;
    

    private ModelNode baseAddress() {
        ModelNode address = new ModelNode();
        if (!bootstrap.isStandalone()) {
            //address.add("host", serverStore.getSelectedServer().getHostName());
            //address.add("server", serverStore.getSelectedServer().getServerName());
        }
        address.add("deployment", "logging");
        return address;
    }


}
