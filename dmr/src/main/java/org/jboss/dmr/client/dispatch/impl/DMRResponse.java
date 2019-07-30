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

package org.jboss.dmr.client.dispatch.impl;


import org.jboss.as.console.client.shared.state.ResponseProcessorDelegate;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.Result;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 3/17/11
 */
@SuppressWarnings("GwtClientClassFromNonInheritedModule")
public class DMRResponse implements Result<ModelNode> {

    private static final String FILTERED_ATTRIBUTES = "filtered-attributes";
    private static final String ABSOLUTE_ADDRESS = "absolute-address";
    private static final String RELATIVE_ADDRESS = "relative-address";
    private static final String RESPONSE_HEADERS = "response-headers";
    private static final String PROCESS_STATE = "process-state";
    private static final String RESTART_NOT_REQUIRED = "restart-not-required";

    private String method;
    private String responseText;
    private String contentType;
    private boolean ignoreRestartHeader = false;

    private ResponseProcessorDelegate processor;

    public DMRResponse(String method, String responseText, String contentType) {
        this.method = method;
        this.responseText = responseText;
        this.contentType = contentType;

        this.processor = new ResponseProcessorDelegate();
    }

    public void setIgnoreRestartHeader(boolean ignoreRestartHeader) {
        this.ignoreRestartHeader = ignoreRestartHeader;
    }

    @Override
    public ModelNode get() {

        ModelNode response = null;
        try {
            response = ModelNode.fromBase64(responseText);

            /*if(response.hasDefined("response-headers"))
            {
                ModelNode responseHeaders = response.get("response-headers");
                if(responseHeaders.hasDefined("access-control"))
                {
                    ModelNode accessHeader = responseHeaders.get("access-control");
                    assert ModelType.LIST == accessHeader.getType();

                    // workaround https://issues.jboss.org/browse/WFLY-1732
                    if(isCollectionResponse())
                    {
                        List<ModelNode> collection = response.get(RESULT).asList();
                        response.get(RESULT).set(collection);
                    }

                    if(ModelType.LIST == response.get(RESULT).getType())
                    {
                        for(ModelNode item : response.get(RESULT).asList())
                        {
                            inlineAccessControlMetaData(accessHeader.asList(), item);
                        }
                    }
                    else if(ModelType.OBJECT == response.get(RESULT).getType())
                    {
                        inlineAccessControlMetaData(accessHeader.asList(), response.get(RESULT));
                    }
                    else
                    {
                        throw new RuntimeException("Unexpected response "+ response);
                    }

                }
            } */

            if ("GET".equals(method)) {
                // For GET request the response is purely the model nodes result. The outcome
                // is not send as part of the response but expressed with the HTTP status code.
                // In order to not break existing code, we repackage the payload into a
                // new model node with an "outcome" and "result" key.
                ModelNode repackaged = new ModelNode();
                repackaged.get(OUTCOME).set(SUCCESS);
                repackaged.get(RESULT).set(response);
                response = repackaged;
            }

        } catch (Throwable e) {

            ModelNode err = new ModelNode();
            err.get("outcome").set("failed");
            err.get("failure-description").set(
                    "Failed to decode response: "+
                            e.getClass().getName() +": "+e.getMessage());
            response = err;
        }

        //Do not show a restart modal window when an ignoreRestart is set to true
        if (ignoreRestartHeader) {
            response.get(RESPONSE_HEADERS).get(PROCESS_STATE).set(RESTART_NOT_REQUIRED);
        }

        processor.process(response);

        return response;
    }

    /*private void inlineAccessControlMetaData(List<ModelNode> accessHeader, ModelNode payload) {

        if(accessHeader.isEmpty())
        {
            Log.debug("response-header w/o access control meta data found!");
            return;
        }

        // match payload and inline response meta data
        for(ModelNode header : accessHeader)
        {
            List<Property> relativeAddress = header.get(RELATIVE_ADDRESS).asPropertyList();
            List<ModelNode> attributeNames = header.get(FILTERED_ATTRIBUTES).asList();

            ModelNode cursor = null;

            if(relativeAddress.isEmpty())
            {
                cursor = payload;
            }
            else
            {

                for(Property addressSegment : relativeAddress)
                {
                    ModelNode target = (cursor == null) ? payload : cursor;
                    String type = addressSegment.getName();
                    String id = addressSegment.getValue().asString();

                    // TODO: https://issues.jboss.org/browse/WFLY-1741
                    if(target.hasDefined(type))     // qualified response
                    {
                        cursor = target.get(type).get(id);
                    }
                    else if(target.hasDefined(id))  // unqualified response
                    {
                        cursor = target.get(id);
                    }
                }
            }

            if(cursor!=null)
            {
                //System.out.println("cursor @ "+ cursor);

                // attributes names should exist on this resource
                for(ModelNode att : attributeNames)
                {
                    String name = att.asString();
                    assert cursor.keys().contains(name) : "Illegal cursor: attributes not found on resource";
                }

                cursor.get("_"+FILTERED_ATTRIBUTES).set(attributeNames);
            }
        }

    }

    private static Set<String> toSet(List<ModelNode> nodeList)
    {
        final Set<String> s = new HashSet<String>(nodeList.size());
        for(ModelNode n : nodeList)
            s.add(n.asString());
        return s;
    }  */
}
