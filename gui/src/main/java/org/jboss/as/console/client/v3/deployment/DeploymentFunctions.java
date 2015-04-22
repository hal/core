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

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FileUpload;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.shared.deployment.DeploymentReference;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.client.widgets.forms.UploadForm;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

import static org.jboss.dmr.client.ModelDescriptionConstants.ADD;
import static org.jboss.dmr.client.ModelDescriptionConstants.NAME;

/**
 * Collection of deployment related functions under a common namespace.
 *
 * @author Harald Pehl
 */
public final class DeploymentFunctions {

    private DeploymentFunctions() {
    }

    /**
     * Expects a {@link DeploymentReference} instance in the context.
     */
    public static class Upload implements Function<FunctionContext> {

        private final UploadForm uploadForm;
        private final FileUpload fileUpload;

        public Upload(final UploadForm uploadForm, final FileUpload fileUpload) {
            this.uploadForm = uploadForm;
            this.fileUpload = fileUpload;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            DeploymentReference upload = control.getContext().pop();
            uploadForm.addUploadCompleteHandler(event -> {
                String json = event.getPayload();
                try {
                    JSONObject response = JSONParser.parseLenient(json).isObject();
                    JSONObject result = response.get("result").isObject();
                    String hash = result.get("BYTES_VALUE").isString().stringValue();
                    upload.setHash(hash);
                    control.getContext().push(upload);
                    control.proceed();
                } catch (Exception e) {
                    control.getContext().setError(new RuntimeException("Failed to upload the deployment."));
                    control.abort();
                }
            });
            uploadForm.upload(fileUpload);
        }
    }


    /**
     * Expects a {@link DeploymentReference} instance in the context.
     */
    public static class AddContent implements Function<FunctionContext> {

        static final String HEADER_CONTENT_TYPE = "Content-Type";
        static final String APPLICATION_JSON = "application/json";

        private final BootstrapContext bootstrapContext;
        private final boolean replace;

        public AddContent(final BootstrapContext bootstrapContext, final boolean replace) {
            this.bootstrapContext = bootstrapContext;
            this.replace = replace;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            DeploymentReference upload = control.getContext().pop();

            String requestJSO = replace ? makeReplaceJSO(upload) : makeAddJSO(upload);
            RequestBuilder rb = new RequestBuilder(RequestBuilder.POST,
                    bootstrapContext.getProperty(BootstrapContext.DOMAIN_API));
            rb.setIncludeCredentials(true);
            rb.setHeader(HEADER_CONTENT_TYPE, APPLICATION_JSON);

            try {
                rb.sendRequest(requestJSO, new RequestCallback() {
                    @Override
                    public void onResponseReceived(Request request, Response response) {
                        int statusCode = response.getStatusCode();
                        if (200 != statusCode) {
                            control.getContext()
                                    .setError(new RuntimeException("Unable to add upload to the content repository."));
                            control.abort();
                        } else {
                            ModelNode node = new ModelNode();
                            node.get(NAME).set(upload.getName());
                            node.get("runtime-name").set(upload.getRuntimeName());
                            control.getContext().push(new Content(node));
                            control.proceed();
                        }
                    }

                    @Override
                    public void onError(Request request, Throwable exception) {
                        control.getContext().setError(
                                new RuntimeException("Unable to add upload to the content repository."));
                        control.abort();
                    }
                });
            } catch (RequestException e) {
                control.getContext().setError(new RuntimeException("Unable to add upload to the content repository."));
                control.abort();
            }
        }

        private String makeAddJSO(DeploymentReference upload) {
            //noinspection StringBufferReplaceableByString
            StringBuilder builder = new StringBuilder();
            builder.append("{");
            builder.append("\"address\":[").append("{\"deployment\":\"").append(upload.getName()).append("\"}],");
            builder.append("\"operation\":\"add\",");
            builder.append("\"runtime-name\":\"").append(upload.getRuntimeName()).append("\",");
            builder.append("\"content\":");
            builder.append("[{\"hash\":{");
            builder.append("\"BYTES_VALUE\":\"").append(upload.getHash()).append("\"");
            builder.append("}}],");
            builder.append("\"name\":\"").append(upload.getName()).append("\",");
            builder.append("\"enabled\":\"").append(upload.isEnableAfterDeployment()).append("\"");
            builder.append("}");
            return builder.toString();
        }

        private String makeReplaceJSO(DeploymentReference upload) {
            //noinspection StringBufferReplaceableByString
            StringBuilder builder = new StringBuilder();
            builder.append("{");
            builder.append("\"operation\":\"full-replace-deployment\",");
            builder.append("\"content\":");
            builder.append("[{\"hash\":{");
            builder.append("\"BYTES_VALUE\":\"").append(upload.getHash()).append("\"");
            builder.append("}}],");
            builder.append("\"name\":\"").append(upload.getName()).append("\",");
            builder.append("\"runtime-name\":\"").append(upload.getRuntimeName()).append("\",");
            builder.append("\"enabled\":\"").append(upload.isEnableAfterDeployment()).append("\"");
            builder.append("}");
            return builder.toString();
        }
    }


    /**
     * Expects a {@link Content} instance in the context.
     */
    public static class AddAssignment implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final String serverGroup;
        private final boolean enable;

        public AddAssignment(final DispatchAsync dispatcher, final String serverGroup, boolean enable) {
            this.dispatcher = dispatcher;
            this.serverGroup = serverGroup;
            this.enable = enable;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            Content content = control.getContext().pop();

            ResourceAddress address = new ResourceAddress()
                    .add("server-group", serverGroup)
                    .add("deployment", content.getName());
            final Operation op = new Operation.Builder(ADD, address)
                    .param("runtime-name", content.getRuntimeName())
                    .param("enabled", enable)
                    .build();

            dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
                @Override
                public void onFailure(final Throwable caught) {
                    control.getContext().setError(new RuntimeException("Unable to assign deployment"));
                    control.abort();
                }

                @Override
                public void onSuccess(final DMRResponse result) {
                    control.proceed();
                }
            });
        }
    }
}
