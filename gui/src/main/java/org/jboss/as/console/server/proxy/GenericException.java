package org.jboss.as.console.server.proxy;

import java.io.IOException;

/**
 * @author Heiko Braun
 * @date 8/6/13
 */
public class GenericException extends IOException {
    private String responseText;
    private String responseBody;

    public GenericException(String message) {
        super(message);
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
