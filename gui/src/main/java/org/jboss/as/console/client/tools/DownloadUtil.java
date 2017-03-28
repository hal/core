/*
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.console.client.tools;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class DownloadUtil {

    /**
     * This method invokes native javascript code to download content using XMLHttpRequest, it performs a HTTP GET
     * stores the response in a Blob object and asks the browser to save its content. This method should only be used
     * when in the SSO context with Keycloak authentication.
     *
     * @param path  The full URL to perform the HTTP GET command.
     * @param filename  This is the suggested filename the browser are going to save the response.
     * @param bearerToken   The bearer token created by the keycloak adapter, it is the value of
     *                      the Authorization HTTP header.
     */
    public static native void downloadHttpGet(String path, String filename, String bearerToken)/*-{
        if (path == null || filename == null) {
            console.log("Download operation: path="+path+" or filename="+filename+" is null. All parameters must be supplied with valid values.");
            return;
        }
        if (window.XMLHttpRequest) {
            var req = new window.XMLHttpRequest();
            // IE 10 or 11
            var ieArr = navigator.userAgent.match(/Trident\/7.0/g) || navigator.userAgent.match(/Trident\/6.0/g);

            req.open('GET', path, true);
            req.responseType = "blob";
            if (bearerToken)
                req.setRequestHeader('Authorization', 'Bearer ' + bearerToken);
            else
                req.withCredentials = true;

            req.onreadystatechange = function () {
                if (req.readyState == 4 && req.status == 200) {
                    if (ieArr) {
                        var blob = req.response;
                        window.navigator.msSaveBlob(blob, filename);
                    } else {
                        var anchor = document.createElement('a');
                        var windowUrl = window.URL || window.webkitURL;
                        if (typeof windowUrl.createObjectURL === 'function') {
                            document.body.appendChild(anchor);
                            anchor.style = "display: none";
                            blob = new Blob([req.response], {type: "octet/stream"});
                            url = window.URL.createObjectURL(blob);
                            anchor.href = url;
                            anchor.download = filename;
                            anchor.click();
                            windowUrl.revokeObjectURL(url);
                            document.body.removeChild(anchor);
                        } else
                            console.log("windowUrl NOT SUPPORTED");
                    }
                }
            };
            req.send();
        } else {
            console.log("XMLHttpRequest NOT SUPPORTED in Browser");
        }
    }-*/;

}
