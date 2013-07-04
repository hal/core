package org.jboss.as.console.client.plugins;

/**
 * @author Heiko Braun
 * @date 7/2/13
 */
public class AccessControlMetaData {

    private String resource;
    private String token;

    public AccessControlMetaData(String token, String resource) {
        this.token = token;
        this.resource = resource;
    }

    public String getToken() {
        return token;
    }

    public String getResource() {
        return resource;
    }
}
