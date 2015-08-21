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

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.ActionHandler;
import org.jboss.dmr.client.dispatch.DispatchError;
import org.jboss.dmr.client.dispatch.DispatchRequest;

import java.util.Map;

/**
 * @author Harald Pehl
 */
public class UploadHandler implements ActionHandler<UploadAction, UploadResponse> {

    private static class UploadDispatchRequest implements DispatchRequest {

        @Override
        public void cancel() {
            throw new UnsupportedOperationException("Cancel not supported in UploadHandler");
        }

        @Override
        public boolean isPending() {
            return false;
        }
    }


    private DMREndpointConfig endpointConfig = GWT.create(DMREndpointConfig.class);

    @Override
    public DispatchRequest execute(final UploadAction action, final AsyncCallback<UploadResponse> callback,
            final Map<String, String> properties) {
        upload(endpointConfig.getUploadUrl(), action.getFileInput(), action.getOperation().toJSONString(true),
                callback);
        return new UploadDispatchRequest();
    }

    private native void upload(String endpoint, Element fileInput, String operation,
            AsyncCallback<UploadResponse> callback) /*-{
        var that = this;

        var formData = new FormData();
        formData.append(fileInput.name, fileInput.files[0]);
        formData.append("operation", operation);

        var xhr = new XMLHttpRequest();
        xhr.withCredentials = true;
        xhr.onreadystatechange = $entry(function (evt) {
            var status, text, readyState;
            try {
                readyState = evt.target.readyState;
                text = evt.target.responseText;
                status = evt.target.status;
            }
            catch (e) {
                that.@org.jboss.dmr.client.dispatch.impl.UploadHandler::onError(*)(e.message, callback);
            }
            if (readyState == 4 && text) {
                that.@org.jboss.dmr.client.dispatch.impl.UploadHandler::processResponse(*)(status, text, callback);
            }
        });
        xhr.open('POST', endpoint, true);
        xhr.send(formData);
    }-*/;

    private void onError(final String error, final AsyncCallback<UploadResponse> callback) {
        callback.onFailure(new DispatchError(error, 500));
    }

    private void processResponse(final int status, final String payload, final AsyncCallback<UploadResponse> callback) {
        if (status == 200 || status == 500) { // 500 means outcome = failed, failure-description = ...
            callback.onSuccess(new UploadResponse(payload));
        } else if (401 == status || 0 == status) {
            callback.onFailure(new DispatchError("Authentication required.", status));
        } else if (403 == status) {
            callback.onFailure(new DispatchError("Authentication required.", status));
        } else if (503 == status) {
            callback.onFailure(new DispatchError("Service temporarily unavailable. Is the server still booting?",
                    status));
        } else {
            callback.onFailure(new DispatchError("Unexpected HTTP response " + status, status));
        }
    }

    @Override
    public DispatchRequest undo(final UploadAction action, final UploadResponse result,
            final AsyncCallback<Void> callback) {
        throw new UnsupportedOperationException("Undo not supported by UploadHandler");
    }
}
