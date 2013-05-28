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


import static org.jboss.dmr.client.ModelDescriptionConstants.OUTCOME;
import static org.jboss.dmr.client.ModelDescriptionConstants.RESULT;
import static org.jboss.dmr.client.ModelDescriptionConstants.SUCCESS;

import org.jboss.dmr.client.ModelDescriptionConstants;
import org.jboss.dmr.client.dispatch.Result;
import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @date 3/17/11
 */
public class DMRResponse implements Result<ModelNode> {

    private String method;
    private String responseText;
    private String contentType;
    //private ResponseProcessor processor;

    public DMRResponse(String method, String responseText, String contentType) {
        this.method = method;
        this.responseText = responseText;
        this.contentType = contentType;

        //this.processor = ResponseProcessorFactory.INSTANCE.get();
    }

    @Override
    public ModelNode get() {

        ModelNode response = null;
        try {
            response = ModelNode.fromBase64(responseText);

            if ("GET".equals(method)) {
                // For GET request the response is purley the model nodes result. The outcome
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

        // TODO: re-enable after refactoring, might as well leverage notification API
        //processor.process(response);

        return response;
    }

}
