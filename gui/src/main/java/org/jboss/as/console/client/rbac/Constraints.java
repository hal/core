package org.jboss.as.console.client.rbac;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Heiko Braun
 * @date 7/9/13
 */
public class Constraints {


    private final String resourceAddress;
    private boolean readResource, writeResource;

    Map<String, AttributePerm> attributePermissions = new HashMap<String,AttributePerm>();
    Map<String, Set<String>> execPermission = new HashMap<String, Set<String>>();

    private boolean address = true;

    public Constraints(String resourceAddress) {
        this.resourceAddress = resourceAddress;
    }

    public String getResourceAddress() {
        return resourceAddress;
    }

    public boolean isAddress() {
        return address;
    }

    public void setAddress(boolean access) {
        this.address = access;
    }

    public void setReadResource(boolean readResource) {
        this.readResource = readResource;
    }

    public void setWriteResource(boolean writeResource) {
        this.writeResource = writeResource;
    }

    public boolean isReadResource() {
        return readResource;
    }

    public boolean isWriteResource() {
        return writeResource;
    }

    public Set<String> getAttributeNames() {
        return attributePermissions.keySet();
    }

    public void setAttributeRead(String name, boolean canBeRead)
    {
        this.attributePermissions.put(name, new AttributePerm(canBeRead));
    }

    public boolean isAttributeRead(String name) {
        return attributePermissions.containsKey(name) ?
                attributePermissions.get(name).isRead() : true;
    }

    public void setAttributeWrite(String name, boolean b)
    {
        if(!attributePermissions.containsKey(name)) {
            attributePermissions.put(name, new AttributePerm(true));
        }

        attributePermissions.get(name).setWrite(b);

    }

    public boolean isAttributeWrite(String name) {
        return attributePermissions.containsKey(name) ?
                attributePermissions.get(name).isWrite() : true;
    }

    public void setOperationExec(String resourceAddress, String operationName, boolean exec) {

        if(exec)
        {
            if(!execPermission.containsKey(resourceAddress))
                execPermission.put(resourceAddress, new HashSet<String>());

            execPermission.get(resourceAddress).add(operationName);
        }

    }

    public boolean isOperationExec(String resourceAddress , String name) {

        return execPermission.containsKey(resourceAddress) ? execPermission.get(resourceAddress).contains(name) : false;
    }

    class AttributePerm
    {

        AttributePerm(boolean read) {
            this.read = read;
        }

        boolean read,write;

        boolean isRead() {
            return read;
        }

        boolean isWrite() {
            return write;
        }

        void setWrite(boolean write) {
            this.write = write;
        }
    }
}

