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

package org.jboss.as.console.client.shared.deployment;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.*;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.deployment.model.DeploymentRecord;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.Feedback;

/**
 * @author Harald Pehl
 * @author Heiko Braun
 * @author Stan Silvert <ssilvert@redhat.com> (C) 2011 Red Hat Inc.
 */
public class NewDeploymentWizard {

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private final BeanFactory factory = GWT.create(BeanFactory.class);
    private final DeployCommandExecutor presenter;
    private final DefaultWindow window;
    private final boolean isUpdate;
    private final DeploymentRecord oldDeployment;

    private final DeckPanel deck;
    private final DeploymentStep1 step1;
    private DeploymentStep2 step2;

    /**
     * @param isUpdate      Are we updating content that is already in the repository?
     * @param oldDeployment The original deployment.  If isUpdate == false, this should be null.
     */
    public NewDeploymentWizard(DeployCommandExecutor presenter, DefaultWindow window, boolean isUpdate, DeploymentRecord oldDeployment) {

        this.presenter = presenter;
        this.window = window;
        this.isUpdate = isUpdate;
        this.oldDeployment = oldDeployment;

        deck = new DeckPanel();
        step1 = new DeploymentStep1(this, window);
        step2 = new DeploymentStep2(this, window);

        deck.add(step1.asWidget());
        deck.add(step2.asWidget());
        deck.showWidget(0);
    }

    public Widget asWidget() {
        return deck;
    }

    public void createManagedDeployment(String filename) {
        String name = filename;
        int fakePathIndex = filename.lastIndexOf("\\");
        if (fakePathIndex != -1) {
            name = filename.substring(fakePathIndex + 1, filename.length());
        }

        DeploymentReference deploymentRef = factory.deploymentReference().as();
        if (isUpdate) {
            deploymentRef.setName(oldDeployment.getName());
            deploymentRef.setRuntimeName(oldDeployment.getRuntimeName());
        } else {
            deploymentRef.setName(name);
            deploymentRef.setRuntimeName(name);
        }

        step2.edit(deploymentRef);
        deck.showWidget(1); // proceed to step2
    }

    public void createUnmanaged(DeploymentRecord entity) {
        presenter.onCreateUnmanaged(entity);
    }

    public void upload() {
        final PopupPanel loading = Feedback.loading(
                Console.CONSTANTS.common_label_plaseWait(),
                Console.CONSTANTS.common_label_requestProcessed(), new Feedback.LoadingCallback() {
                    @Override
                    public void onCancel() {
                    }
                });

        step1.getManagedForm().addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
            @Override
            public void onSubmitComplete(FormPanel.SubmitCompleteEvent event) {
                String html = event.getResults();

                try {
                    String json = html;

                    try {
                        if (!GWT.isScript()) // TODO: Formpanel weirdness
                            json = html.substring(html.indexOf(">") + 1, html.lastIndexOf("<"));
                    } catch (StringIndexOutOfBoundsException e) {
                        // if I get this exception it means I shouldn't strip out the html
                        // this issue still needs more research
                        Log.debug("Failed to strip out HTML.  This should be preferred?");
                    }

                    JSONObject response = JSONParser.parseLenient(json).isObject();
                    JSONObject result = response.get("result").isObject();
                    String hash = result.get("BYTES_VALUE").isString().stringValue();
                    DeploymentReference deploymentReference = step2.getDeploymentReference();
                    deploymentReference.setHash(hash);
                    assignDeployment(deploymentReference, loading);
                } catch (Exception e) {
                    loading.hide();
                    Log.error(Console.CONSTANTS.common_error_failedToDecode() + ": " + html, e);
                }
            }
        });
        step1.getManagedForm().submit();
    }

    private void assignDeployment(final DeploymentReference deployment, final PopupPanel loading) {
        String requestJSO = isUpdate ? makeFullReplaceJSO(deployment) : makeAddJSO(deployment);
        RequestBuilder rb = new RequestBuilder(
                RequestBuilder.POST,
                Console.getBootstrapContext().getProperty(BootstrapContext.DOMAIN_API)
        );
        rb.setHeader(HEADER_CONTENT_TYPE, APPLICATION_JSON);

        try {
            rb.sendRequest(requestJSO, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    if (200 != response.getStatusCode()) {
                        loading.hide();
                        onDeploymentFailed(deployment, response);
                        return;
                    }
                    loading.hide();
                    window.hide();
                    presenter.refreshDeployments();

                    String operation = Console.CONSTANTS.common_label_addContent();
                    if (isUpdate) operation = Console.CONSTANTS.common_label_updateContent();
                    Console.info(Console.CONSTANTS.common_label_success() +
                            ": " + operation +
                            ": " + deployment.getName());
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    loading.hide();
                    Console.error(Console.CONSTANTS.common_error_deploymentFailed() + ": " + exception.getMessage());
                }
            });
        } catch (RequestException e) {
            loading.hide();
            Console.error(Console.CONSTANTS.common_error_deploymentFailed() + ": " + e.getMessage());
        }
    }

    private String makeAddJSO(DeploymentReference deployment) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"address\":[").append("{\"deployment\":\"").append(deployment.getName()).append("\"}],");
        sb.append("\"operation\":\"add\",");
        sb.append("\"runtime-name\":\"").append(deployment.getRuntimeName()).append("\",");
        sb.append("\"content\":");
        sb.append("[{\"hash\":{");
        sb.append("\"BYTES_VALUE\":\"").append(deployment.getHash()).append("\"");
        sb.append("}}],");
        sb.append("\"name\":\"").append(deployment.getName()).append("\",");
        sb.append("\"enabled\":\"").append(Console.getBootstrapContext().isStandalone() ? deployment.isEnableAfterDeployment() : true).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private String makeFullReplaceJSO(DeploymentReference deployment) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"operation\":\"full-replace-deployment\",");
        sb.append("\"content\":");
        sb.append("[{\"hash\":{");
        sb.append("\"BYTES_VALUE\":\"").append(deployment.getHash()).append("\"");
        sb.append("}}],");
        sb.append("\"name\":\"").append(deployment.getName()).append("\",");
        sb.append("\"runtime-name\":\"").append(deployment.getRuntimeName()).append("\",");
        sb.append("\"enabled\":\"").append(Console.getBootstrapContext().isStandalone() ? deployment.isEnableAfterDeployment() : true).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private void onDeploymentFailed(DeploymentReference deployment, Response response) {
        Console.error(Console.CONSTANTS.common_error_deploymentFailed() +
                ": " + deployment.getName() +
                ": " + response.getText());
    }

    public boolean isUpdate() {
        return isUpdate;
    }
}
