package org.jboss.as.console.client.rbac;

class ResourceRef {
    private static final String OPT = "opt:/";
    String address;
    boolean optional;

    ResourceRef(String resourceRef) {
        if(resourceRef.startsWith(OPT))
        {
            this.address = resourceRef.substring(5, resourceRef.length());
            optional = true;
        }
        else
        {
            this.address = resourceRef;
            optional = false;
        }
    }
}
