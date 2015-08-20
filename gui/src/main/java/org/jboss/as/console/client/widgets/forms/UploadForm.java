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
package org.jboss.as.console.client.widgets.forms;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Hidden;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.dmr.client.ModelDescriptionConstants;
import org.jboss.dmr.client.ModelNode;

/**
 * A {@link FormPanel} to be used for file uploads within HAL. It uses the new HTML5 FormData interface which requires
 * a <a href="http://caniuse.com/#search=FormData">modern browser</a>.
 *
 * @author Harald Pehl
 * @see <a href="https://dvcs.w3.org/hg/xhr/raw-file/tip/Overview.html#formdata">https://dvcs.w3.org/hg/xhr/raw-file/tip/Overview.html#formdata</a>
 */
public class UploadForm extends FormPanel {

    @FunctionalInterface
    public interface UploadCompleteCallback {

        void onUploadComplete(String payload);
    }

    @FunctionalInterface
    public interface UploadFailedCallback {

        void onUploadFailed(String error);
    }


    private final Hidden operation;
    private UploadCompleteCallback uploadCompleteCallback;
    private UploadFailedCallback uploadFailedCallback;

    public UploadForm() {
        this(Console.getBootstrapContext().getProperty(BootstrapContext.UPLOAD_API));
    }

    public UploadForm(String endpoint) {
        this.operation = new Hidden(ModelDescriptionConstants.OP);
        setAction(endpoint);
        setEncoding(FormPanel.ENCODING_MULTIPART);
        setMethod(FormPanel.METHOD_POST);
    }

    public void setOperation(final ModelNode operation) {
        this.operation.setValue(operation.toJSONString(true));
    }

    public void onUploadComplete(UploadCompleteCallback callback) {
        this.uploadCompleteCallback = callback;
    }

    private void callUploadComplete(String payload) {
        if (uploadCompleteCallback != null) {
            uploadCompleteCallback.onUploadComplete(payload);
        }
    }

    public void onUploadFailed(UploadFailedCallback callback) {
        this.uploadFailedCallback = callback;
    }

    private void callUploadFailed(String error) {
        if (uploadFailedCallback != null) {
            uploadFailedCallback.onUploadFailed(error);
        }
    }

    public void upload(FileUpload fileInput) {
        uploadUsingFormData(getAction(), operation.getElement(), fileInput.getElement());
    }

    private native void uploadUsingFormData(String action, Element operation, Element fileInput) /*-{
        var that = this;

        var file = fileInput.files[0];
        var formData = new FormData();
        formData.append(fileInput.name, file);
        formData.append(operation.name, operation.value);

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
                that.@org.jboss.as.console.client.widgets.forms.UploadForm::callUploadFailed(Ljava/lang/String;)(e.message);
            }
            if (readyState == 4 && text) {
                that.@org.jboss.as.console.client.widgets.forms.UploadForm::callUploadComplete(Ljava/lang/String;)(text);
            }
        });
        xhr.open('POST', action, true);
        xhr.send(formData);
    }-*/;
}
