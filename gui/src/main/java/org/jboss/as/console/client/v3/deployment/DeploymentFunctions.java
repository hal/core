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
import com.google.gwt.user.client.ui.FileUpload;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.domain.model.ServerInstance;
import org.jboss.as.console.client.domain.topology.HostInfo;
import org.jboss.as.console.client.domain.topology.TopologyFunctions;
import org.jboss.as.console.client.shared.flow.FunctionCallback;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.v3.dmr.Composite;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.client.widgets.forms.UploadForm;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * Collection of deployment related functions under a common namespace.
 *
 * @author Harald Pehl
 */
public final class DeploymentFunctions {

    public static final String ASSIGNMENTS = DeploymentFunctions.class.getName() + ".assignments";
    public static final String REFERENCE_SERVER = DeploymentFunctions.class.getName() + ".referenceServer";
    public static final String NO_REFERENCE_SERVER_WARNING = "No Reference Server found"; // TODO i18n

    private DeploymentFunctions() {}

    /**
     * Uploads a deployment, sets the deployment hash and pushes the updated {@link UploadBean} instance into
     * the context.
     */
    public static class UploadContent implements Function<FunctionContext> {

        private final UploadForm uploadForm;
        private final FileUpload fileUpload;
        private final UploadBean upload;

        public UploadContent(final UploadForm uploadForm, final FileUpload fileUpload,
                final UploadBean upload) {
            this.uploadForm = uploadForm;
            this.fileUpload = fileUpload;
            this.upload = upload;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
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
     * Adds or replaces an uploaded deployment to/in the content repository.
     * Expects a {@link UploadBean} instance on top of the context stack.
     * Pushes an {@link Content} instance into the context.
     */
    public static class AddOrReplaceContent implements Function<FunctionContext> {

        static final String HEADER_CONTENT_TYPE = "Content-Type";
        static final String APPLICATION_JSON = "application/json";

        private final BootstrapContext bootstrapContext;
        private final boolean replace;

        public AddOrReplaceContent(final BootstrapContext bootstrapContext, final boolean replace) {
            this.bootstrapContext = bootstrapContext;
            this.replace = replace;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            UploadBean upload = control.getContext().pop();

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

        private String makeAddJSO(UploadBean upload) {
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

        private String makeReplaceJSO(UploadBean upload) {
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
     * Adds an unmanaged deployment to the content repository. Pushes an {@link Content} instance into the context.
     */
    public static class AddUnmanagedContent implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final UnmanagedBean unmanaged;

        public AddUnmanagedContent(final DispatchAsync dispatcher, final UnmanagedBean unmanaged) {
            this.dispatcher = dispatcher;
            this.unmanaged = unmanaged;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            ResourceAddress address = new ResourceAddress().add("deployment", unmanaged.getName());

            List<ModelNode> content = new ArrayList<>(1);
            ModelNode path = new ModelNode();
            path.get("path").set(unmanaged.getPath());
            path.get("archive").set(unmanaged.isArchive());
            if (unmanaged.getRelativeTo() != null && !unmanaged.getRelativeTo().equals("")) {
                path.get("relative-to").set(unmanaged.getRelativeTo());
            }
            content.add(path);

            Operation op = new Operation.Builder(ADD, address)
                    .param("name", unmanaged.getName())
                    .param("runtime-name", unmanaged.getRuntimeName())
                    .param("enabled", unmanaged.isEnabled())
                    .param("content", content)
                    .build();

            dispatcher.execute(new DMRAction(op), new FunctionCallback(control) {
                @Override
                protected void onFailedOutcome(final ModelNode result) {
                    context.setErrorMessage("Unable to add unmanaged deployment: " + result.getFailureDescription());
                }

                @Override
                public void onSuccess(final ModelNode result) {
                    ModelNode node = new ModelNode();
                    node.get(NAME).set(unmanaged.getName());
                    node.get("runtime-name").set(unmanaged.getRuntimeName());
                    control.getContext().push(new Content(node));
                }
            });
        }
    }


    /**
     * Loads the contents form the content repository and pushes a {@code List&lt;Content&gt;} onto the context stack.
     */
    public static class LoadContentAssignments implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final String serverGroup;

        public LoadContentAssignments(final DispatchAsync dispatcher) {
            this(dispatcher, "*");
        }

        /**
         * @param dispatcher  the dispatcher
         * @param serverGroup use "*" to find assignments on any server group
         */
        public LoadContentAssignments(final DispatchAsync dispatcher, final String serverGroup) {
            this.dispatcher = dispatcher;
            this.serverGroup = serverGroup;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            Operation content = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, ResourceAddress.ROOT)
                    .param(CHILD_TYPE, "deployment")
                    .build();
            ResourceAddress address = new ResourceAddress()
                    .add("server-group", serverGroup)
                    .add("deployment", "*");
            Operation assignments = new Operation.Builder(READ_RESOURCE_OPERATION, address).build();

            dispatcher.execute(new DMRAction(new Composite(content, assignments)), new FunctionCallback(control) {
                @Override
                public void onSuccess(final ModelNode result) {
                    Map<String, Content> contentByName = new HashMap<>();
                    List<Property> properties = result.get(RESULT).get("step-1").get(RESULT).asPropertyList();
                    for (Property property : properties) {
                        Content content = new Content(property.getValue());
                        contentByName.put(content.getName(), content);
                    }

                    List<ModelNode> nodes = result.get(RESULT).get("step-2").get(RESULT).asList();
                    for (ModelNode node : nodes) {
                        ModelNode addressNode = node.get(ADDRESS);
                        String groupName = addressNode.asList().get(0).get("server-group").asString();
                        ModelNode assignmentNode = node.get(RESULT);
                        Assignment assignment = new Assignment(groupName, assignmentNode);
                        Content content = contentByName.get(assignment.getName());
                        if (content != null) {
                            content.addAssignment(assignment);
                        }
                    }
                    context.push(new ArrayList<>(contentByName.values()));
                }
            });
        }
    }


    /**
     * Adds and optionally enables an assignment to the specified server group. Expects a {@link Content} instance in
     * the context.
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
            Operation op = new Operation.Builder(ADD, address)
                    .param("runtime-name", content.getRuntimeName())
                    .param("enabled", enable)
                    .build();

            dispatcher.execute(new DMRAction(op), new FunctionCallback(control) {
                @Override
                protected void onFailedOutcome(final ModelNode result) {
                    control.getContext().setErrorMessage(
                            "Unable to assign deployment: " + result.getFailureDescription());
                }
            });
        }
    }


    /**
     * Loads the assignments of the specified server group and puts the result as list of {@link Assignment} instances
     * under the key {@link #ASSIGNMENTS} in the context.
     */
    public static class LoadAssignments implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final String serverGroup;

        public LoadAssignments(final DispatchAsync dispatcher, final String serverGroup) {
            this.dispatcher = dispatcher;
            this.serverGroup = serverGroup;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {

            ResourceAddress address = new ResourceAddress().add("server-group", serverGroup);
            Operation op = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, address)
                    .param(CHILD_TYPE, "deployment")
                    .build();

            dispatcher.execute(new DMRAction(op), new FunctionCallback(control) {
                @Override
                public void onSuccess(final ModelNode result) {
                    List<Assignment> assignments = new ArrayList<>();
                    ModelNode payload = result.get(RESULT);
                    List<Property> properties = payload.asPropertyList();
                    for (Property property : properties) {
                        assignments.add(new Assignment(serverGroup, property.getValue()));
                    }
                    control.getContext().set(ASSIGNMENTS, assignments);
                }
            });
        }
    }


    /**
     * Tries to find a reference (running) server for the specified server group. Expects a the list of hosts stored
     * under the key {@link org.jboss.as.console.client.domain.topology.TopologyFunctions#HOSTS_KEY} in the context.
     * If a reference server is found it's put under the key {@link #REFERENCE_SERVER} into the context.
     */
    public static class FindReferenceServer implements Function<FunctionContext> {

        private final String serverGroup;

        public FindReferenceServer(final String serverGroup) {
            this.serverGroup = serverGroup;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            ReferenceServer referenceServer = null;
            List<HostInfo> hosts = control.getContext()
                    .getOrDefault(TopologyFunctions.HOSTS_KEY, input -> Collections.emptyList());

            for (Iterator<HostInfo> i = hosts.iterator(); i.hasNext() && referenceServer == null; ) {
                HostInfo host = i.next();
                List<ServerInstance> serverInstances = host.getServerInstances();
                for (Iterator<ServerInstance> j = serverInstances.iterator();
                        j.hasNext() && referenceServer == null; ) {
                    ServerInstance server = j.next();
                    if (server.isRunning() && server.getGroup().equals(serverGroup)) {
                        referenceServer = new ReferenceServer(server.getHost(), server.getName());
                    }
                }
            }
            if (referenceServer != null) {
                control.getContext().set(REFERENCE_SERVER, referenceServer);
            }
            control.proceed();
        }
    }


    /**
     * Loads the deployments from a reference server which needs to be in the context map (key {@link
     * #REFERENCE_SERVER}. Expects the list of assignments for the related server group under the key {@link
     * #ASSIGNMENTS} in the context. Updated the assignments with the deployment instance.
     */
    public static class LoadDeploymentsFromReferenceServer implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;

        public LoadDeploymentsFromReferenceServer(final DispatchAsync dispatcher) {
            this.dispatcher = dispatcher;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            if (control.getContext().get(REFERENCE_SERVER) == null) {
                control.getContext().setErrorMessage(NO_REFERENCE_SERVER_WARNING);
                control.abort();
            } else {
                final ReferenceServer referenceServer = control.getContext().get(REFERENCE_SERVER);

                Operation op = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, referenceServer.getAddress())
                        .param(CHILD_TYPE, "deployment")
                        .param(INCLUDE_RUNTIME, true)
                        .param(RECURSIVE, true)
                        .build();

                dispatcher.execute(new DMRAction(op), new FunctionCallback(control) {
                    @Override
                    public void onSuccess(final ModelNode result) {
                        Map<String, Deployment> deploymentsByName = new HashMap<>();

                        ModelNode payload = result.get(RESULT);
                        List<Property> properties = payload.asPropertyList();
                        for (Property property : properties) {
                            final Deployment deployment = new Deployment(referenceServer, property.getValue());
                            deploymentsByName.put(deployment.getName(), deployment);
                        }

                        // update assignments
                        List<Assignment> assignments = context.get(ASSIGNMENTS);
                        for (Assignment assignment : assignments) {
                            assignment.setDeployment(deploymentsByName.get(assignment.getName()));
                        }
                    }
                });
            }
        }
    }
}
