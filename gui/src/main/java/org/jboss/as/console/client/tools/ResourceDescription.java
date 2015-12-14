package org.jboss.as.console.client.tools;


import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;
import org.jboss.dmr.client.Property;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * Represents the result of a read-resource-description operation for one specific resource.
 * @author Harald Pehl
 */
public class ResourceDescription extends ModelNode {

    public final static ResourceDescription EMPTY = new ResourceDescription();

    static final String ACCESS_CONTROL = "access-control";
    static final String NOTIFICATIONS = "notifications";
    private boolean isSingleton;
    private String singletonName;

    public ResourceDescription() {
        super();
    }

    public ResourceDescription(ModelNode description) {
        set(description);
    }

    public boolean hasAttributes() {
        return hasDefined(ATTRIBUTES);
    }

    public List<Property> getAttributes() {
        return hasAttributes() ? get(ATTRIBUTES).asPropertyList() : Collections.EMPTY_LIST;
    }

    public boolean hasAccessControl() {
        return hasDefined(ACCESS_CONTROL);
    }

    public boolean hasChildrenDefined() {
        return hasDefined(CHILDREN) && !get(CHILDREN).keys().isEmpty();
    }

    public boolean hasOperations() {
        return hasDefined(OPERATIONS);
    }

    public boolean hasNotifications() {
        return hasDefined(NOTIFICATIONS);
    }

    public Set<String> getChildrenTypes() {

        Set<String> result = new HashSet<>();

        if(hasChildrenDefined())
        {
            ModelNode children = get(CHILDREN);
            List<Property> items = children.asPropertyList();
            for (Property item : items) {
                Set<String> keys = item.getValue().get(MODEL_DESCRIPTION).keys();
                if(keys.contains("*")) // regular resources (opposed to singletons, that carry distinct names)
                {
                    result.add(item.getName());
                }
            }
        }

        return result;
    }

    public Set<String> getSingletonChildrenTypes() {

        Set<String> result = new HashSet<>();

        if(hasChildrenDefined())
        {
            ModelNode children = get(CHILDREN);
            List<Property> items = children.asPropertyList();
            for (Property item : items) {
                Set<String> keys = item.getValue().get(MODEL_DESCRIPTION).keys();
                if(!keys.contains("*")) // singleton resources
                {
                    for (String key : keys) {
                        result.add(item.getName()+"="+key);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Looks for the description of a child resource.
     * @param childType The type of the child resource
     * @return the description of the child resource or {@link #EMPTY} if no such resource exists.
     */
    public ResourceDescription getChildDescription(String childType) {
        return getChildDescription(childType, "*");
    }

    /**
     * Looks for the description of a specific child resource.
     * @param type The type of the child resource
     * @param name The name of the instance
     * @return the description of the specific child resource or {@link #EMPTY} if no such resource exists.
     */
    public ResourceDescription getChildDescription(String type, String name) {
        if (hasChildrenDefined()) {
            List<Property> children = get("children").asPropertyList();
            for (Property child : children) {
                if (type.equals(child.getName()) && child.getValue().hasDefined(MODEL_DESCRIPTION)) {
                    List<Property> modelDescriptions = child.getValue().get(MODEL_DESCRIPTION).asPropertyList();
                    for (Property modelDescription : modelDescriptions) {
                        if (name.equals(modelDescription.getName())) {
                            return new ResourceDescription(modelDescription.getValue());
                        }
                    }
                }
            }
        }
        return EMPTY;
    }

    public static ResourceDescription from(ModelNode response) {

        if(!response.get(OUTCOME).asString().equals(SUCCESS))
            throw new RuntimeException(response.get(FAILURE_DESCRIPTION).asString());

        ModelNode result = response.get(RESULT);
        if(ModelType.LIST == result.getType())
        {
            // wildcard addressing
            return new ResourceDescription(result.asList().get(0).get(RESULT));

        }
        else
        {
            // specific addressing
            return new ResourceDescription(result);
        }
    }

    public String getText() {
        return get(DESCRIPTION).asString();
    }

    public boolean isSingleton() {
        return isSingleton;
    }

    public String getSingletonName() {
        return singletonName;
    }

    public void setSingletonName(String name) {
        this.isSingleton = true;
        this.singletonName = name;
    }
}
