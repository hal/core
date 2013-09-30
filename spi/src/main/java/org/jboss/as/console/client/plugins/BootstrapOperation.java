package org.jboss.as.console.client.plugins;

/**
 * @author Heiko Braun
 * @date 9/30/13
 */
public class BootstrapOperation {

    String token;
    String operation;


    public BootstrapOperation(String token, String operation) {
        this.token = token;
        this.operation = operation;
    }

    public String getToken() {
        return token;
    }

    public String getOperation() {
        return operation;
    }
}
