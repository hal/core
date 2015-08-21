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
package org.jboss.dmr.client.dispatch.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.Result;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
public class UploadResponse implements Result<ModelNode> {

    private final String payload;

    public UploadResponse(final String payload) {this.payload = payload;}

    @Override
    public ModelNode get() {
        ModelNode node = new ModelNode();
        JSONObject response = JSONParser.parseLenient(payload).isObject();
        JSONString outcome = response.get(OUTCOME).isString();
        node.get(OUTCOME).set(outcome.stringValue());
        if (outcome.stringValue().equals(SUCCESS)) {
            if (response.containsKey(RESULT) && response.get(RESULT).isObject() != null) {
                node.get(RESULT).set(stringify(response.get(RESULT).isObject().getJavaScriptObject(), 2));
            } else {
                node.get(RESULT).set(new ModelNode());
            }
        } else {
            String failure = extractFailure(response);
            node.get(FAILURE_DESCRIPTION).set(failure);
        }
        return node;
    }

    private String extractFailure(final JSONObject response) {
        String failure = "n/a";
        JSONValue failureValue = response.get(FAILURE_DESCRIPTION);
        if (failureValue.isString() != null) {
            failure = failureValue.isString().stringValue();
        } else if (failureValue.isObject() != null) {
            JSONObject failureObject = failureValue.isObject();
            for (String key : failureObject.keySet()) {
                if (key.contains("failure") && failureObject.get(key).isString() != null) {
                    failure = failureObject.get(key).isString().stringValue();
                    break;
                }
            }
        }
        return failure;
    }

    private native String stringify(JavaScriptObject json, int indent) /*-{
        return JSON.stringify(json, null, indent);
    }-*/;
}
