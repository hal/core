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

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import org.jboss.as.console.client.Console;

/**
 * A {@link FormPanel} to be used for file uploads within HAL. It uses the GWT approach with a hidden iframe when
 * running in "same-origin-mode" and the new HTML5 FormData interface and XMLHttpRequest otherwise (due to CORS
 * restrictions). Please not that the latter require a <a href="http://caniuse.com/#search=FormData">modern browser</a>.
 *
 * @author Harald Pehl
 * @see <a href="https://dvcs.w3.org/hg/xhr/raw-file/tip/Overview.html#formdata">https://dvcs.w3.org/hg/xhr/raw-file/tip/Overview.html#formdata</a>
 */
public class UploadForm extends FormPanel {

    // ------------------------------------------------------ init

    private final boolean sameOrigin;
    private HandlerRegistration handlerRegistration;

    public UploadForm() {
        sameOrigin = Console.getBootstrapContext().isSameOrigin();
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        if (sameOrigin) {
            // forward GWT's submit complete handler to our own upload complete handler
            handlerRegistration = addSubmitCompleteHandler(new SubmitCompleteHandler() {
                @Override
                public void onSubmitComplete(SubmitCompleteEvent event) {
                    String payload = event.getResults();
                    if (payload != null) {
                        try {
                            if (!GWT.isScript()) // TODO: Formpanel weirdness
                                payload = payload.substring(payload.indexOf(">") + 1, payload.lastIndexOf("<"));
                        } catch (StringIndexOutOfBoundsException e) {
                            // if I get this exception it means I shouldn't strip out the html
                            // this issue still needs more research
                            Log.debug("Failed to strip out HTML.  This should be preferred?");
                        }
                    }
                    fireUploadComplete(payload);
                }
            });
        }
    }

    @Override
    protected void onDetach() {
        if (handlerRegistration != null) {
            handlerRegistration.removeHandler();
        }
        super.onDetach();
    }


    // ------------------------------------------------------ upload complete event

    /**
     * Fired when a upload has been submitted successfully.
     */
    public static class UploadCompleteEvent extends GwtEvent<UploadCompleteHandler> {
        /**
         * The event type.
         */
        private static Type<UploadCompleteHandler> TYPE;

        /**
         * Handler hook.
         *
         * @return the handler hook
         */
        public static Type<UploadCompleteHandler> getType() {
            if (TYPE == null) {
                TYPE = new Type<UploadCompleteHandler>();
            }
            return TYPE;
        }

        private String payload;

        /**
         * Create a upload complete event.
         *
         * @param payload the results from uploading the file(s)
         */
        protected UploadCompleteEvent(String payload) {
            this.payload = payload;
        }

        @Override
        public final Type<UploadCompleteHandler> getAssociatedType() {
            return getType();
        }

        /**
         * Gets the payload.
         *
         * @return the payload.
         */
        public String getPayload() {
            return payload;
        }

        @Override
        protected void dispatch(UploadCompleteHandler handler) {
            handler.onUploadComplete(this);
        }
    }

    /**
     * Handler for {@link UploadForm.UploadCompleteEvent} events.
     */
    public interface UploadCompleteHandler extends EventHandler {
        /**
         * Fired when a upload has been submitted successfully.
         *
         * @param event the event
         */
        void onUploadComplete(UploadForm.UploadCompleteEvent event);
    }

    /**
     * Adds a {@link UploadForm.UploadCompleteEvent} handler.
     *
     * @param handler the handler
     * @return the handler registration used to remove the handler
     */
    public HandlerRegistration addUploadCompleteHandler(UploadCompleteHandler handler) {
        return addHandler(handler, UploadCompleteEvent.getType());
    }

    private void fireUploadComplete(String payload) {
        fireEvent(new UploadCompleteEvent(payload));
    }


    // ------------------------------------------------------ upload methods

    public void upload(FileUpload fileInput) {
        if (sameOrigin) {
            submit();
        } else {
            uploadUsingFormData(getAction(), fileInput.getElement());
        }
    }

    private native void uploadUsingFormData(String action, Element fileInput) /*-{
        var that = this;

        var file = fileInput.files[0];
        var formData = new FormData();
        formData.append(fileInput.name, file);

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
                return;
            }
            if (readyState == 4 && status == '200' && text) {
                that.@org.jboss.as.console.client.widgets.forms.UploadForm::fireUploadComplete(Ljava/lang/String;)(text);
            }
        });
        xhr.open('POST', action, true);
        xhr.send(formData);
    }-*/;
}
