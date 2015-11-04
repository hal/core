package org.jboss.as.console.client.rbac;

import org.jboss.as.console.client.v3.dmr.AddressTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A set of constraints for a resource.
 *
 * @author Heiko Braun
 * @date 7/9/13
 */
public class Constraints {


    private final AddressTemplate resourceAddress;
    private boolean readResource, writeResource;

    Map<String, AttributePerm> attributePermissions = new HashMap<String,AttributePerm>();
    Set<String> execPermission = new HashSet<>();

    private boolean address = true;

    private AddressTemplate parent;

    public Constraints(AddressTemplate resourceAddress) {
        this.resourceAddress = resourceAddress;
    }

    public AddressTemplate getResourceAddress() {
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

    public void setOperationExec(String operationName, boolean exec) {

        if(exec)
        {
            execPermission.add(operationName);
        }

    }

    public boolean isOperationExec(String operationName) {
        return execPermission.contains(operationName);
    }

    public boolean isChildContext() {
        return parent!=null;
    }

    void setParent(AddressTemplate parent) {
        this.parent = parent;
    }

    public AddressTemplate getParent() {
        return parent;
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

