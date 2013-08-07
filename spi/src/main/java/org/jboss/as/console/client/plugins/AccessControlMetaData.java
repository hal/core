package org.jboss.as.console.client.plugins;

/**
 * @author Heiko Braun
 * @date 7/2/13
 */
public class AccessControlMetaData {

    private String resource;
    private String token;
    private String facet;
    private String recursive;

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

    public String getFacet() {
        return facet;
    }

    public void setFacet(String facet) {
        this.facet = facet;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = String.valueOf(recursive);
    }

    public String isRecursive() {
        return recursive;
    }
}
