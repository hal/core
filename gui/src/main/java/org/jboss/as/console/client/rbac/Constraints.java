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


    private boolean readConfig,writeConfig,readRuntime,writeRuntime;

    Map<String, AttributePerm> attributePermissions = new HashMap<String,AttributePerm>();
    Map<String, Set<String>> execPermission = new HashMap<String, Set<String>>();

    private boolean address = true;

    public boolean isAddress() {
        return address;
    }

    public void setAddress(boolean access) {
        this.address = access;
    }

    @Deprecated
    public void setReadConfig(boolean readConfig) {
        this.readConfig = readConfig;
        this.readRuntime = readConfig;     // TODO: Fix me
    }

    @Deprecated
    public void setWriteConfig(boolean writeConfig) {
        this.writeConfig = writeConfig;
        this.writeRuntime = writeConfig;       // TODO: Fix me
    }

    @Deprecated
    public void setReadRuntime(boolean readRuntime) {
        this.readRuntime = readRuntime;
    }

    @Deprecated
    public void setWriteRuntime(boolean writeRuntime) {
        this.writeRuntime = writeRuntime;
    }

    @Deprecated
    public boolean isReadConfig() {
        return readConfig;
    }

    @Deprecated
    public boolean isWriteConfig() {
        return writeConfig;
    }

    @Deprecated
    public boolean isReadRuntime() {
        return readRuntime;
    }

    @Deprecated
    public boolean isWriteRuntime() {
        return writeRuntime;
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

