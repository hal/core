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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
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
        upload(endpointConfig.getUploadUrl(), action.getFileInput(), action.getOperation().toBase64String(),
                callback);
        return new UploadDispatchRequest();
    }

    private native void upload(String endpoint, Element fileInput, String operation,
            AsyncCallback<UploadResponse> callback) /*-{

        if (!@org.jboss.dmr.client.dispatch.impl.UploadHandler::verifySupport()()) {
            this.@org.jboss.dmr.client.dispatch.impl.UploadHandler::onError(*)("Due to security reasons, your browser is not supported for uploads. When running IE, please make sure the page is not opened in compatibility mode. Otherwise please use a more recent browser.", callback);
            return;
        }

        var that = this;
        var formData = new $wnd.FormData();
        formData.append(fileInput.name, fileInput.files[0]);
        formData.append("operation", new Blob([operation], {type : "application/dmr-encoded"}));

        var xhr;
        if ($wnd.XMLHttpRequest) {
            xhr = new $wnd.XMLHttpRequest();
        } else {
            try {
                xhr = new $wnd.ActiveXObject('MSXML2.XMLHTTP.3.0');
            } catch (e) {
                xhr = new $wnd.ActiveXObject("Microsoft.XMLHTTP");
            }
        }

        xhr.onreadystatechange = function () {
            var status, text, readyState, contentType;
            try {
                readyState = xhr.readyState;
                text = xhr.responseText;
                status = xhr.status;
                contentType = xhr.getResponseHeader("Content-Type");
            }
            catch (e) {
                that.@org.jboss.dmr.client.dispatch.impl.UploadHandler::onError(*)(e.message, callback);
            }
            if (readyState == 4) {
                that.@org.jboss.dmr.client.dispatch.impl.UploadHandler::processResponse(*)(status, text, contentType, callback);
            }
        };
        xhr.open("POST", endpoint, true);
        xhr.setRequestHeader("X-Management-Client-Name", "HAL");
        xhr.withCredentials = true; // Do not set *before* xhr.open() - see https://xhr.spec.whatwg.org/#the-withcredentials-attribute
        xhr.send(formData);
    }-*/;

    public static native boolean verifySupport() /*-{
        var fi = $doc.createElement('INPUT');
        fi.type = 'file';
        var fileApiSupport = 'files' in fi;

        var xhr;
        if ($wnd.XMLHttpRequest) {
            xhr = new $wnd.XMLHttpRequest();
        } else {
            try {
                xhr = new $wnd.ActiveXObject('MSXML2.XMLHTTP.3.0');
            } catch (e) {
                xhr = new $wnd.ActiveXObject("Microsoft.XMLHTTP");
            }
        }
        var progressEventsSupport = !!(xhr && ('upload' in xhr) && ('onprogress' in xhr.upload));

        var formDataSupport = !!$wnd.FormData;

        if (!fileApiSupport) {
            console.log("Async uploads not supported: No File API.");
            return false;
        }
        if (!progressEventsSupport) {
            console.log("Async uploads not supported: No progress events.");
            return false;
        }
        if (!formDataSupport) {
            console.log("Async uploads not supported: No FormData object.");
            return false;
        }
        return true;
    }-*/;

    private void onError(final String error, final AsyncCallback<UploadResponse> callback) {
        callback.onFailure(new DispatchError(error, 500));
    }

    private void processResponse(final int status, final String payload, final String contentType, final AsyncCallback<UploadResponse> callback) {
        if (status == 200 || status == 500) { // 500 means outcome = failed, failure-description = ...
            callback.onSuccess(new UploadResponse(payload, contentType));
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
