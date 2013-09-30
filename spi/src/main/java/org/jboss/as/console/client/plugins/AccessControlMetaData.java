package org.jboss.as.console.client.plugins;

/**
 * @author Heiko Braun
 * @date 7/2/13
 */
public class AccessControlMetaData {

    private String resource;
    private String token;
    private String recursive;
    private String[] operations;

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

    public void setRecursive(boolean recursive) {
        this.recursive = String.valueOf(recursive);
    }

    public String isRecursive() {
        return recursive;
    }

    public void setOperations(String[] operations) {
        this.operations = operations;
    }

    public String[] getOperations() {
        return operations;
    }
}
