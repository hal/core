package org.jboss.dmr.client.dispatch;

/**
 * @author Heiko Braun
 * @date 9/17/13
 */
public class DispatchError extends Exception {
    private int statusCode;

    public DispatchError(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
