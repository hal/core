package org.jboss.as.console.client.rbac;

import org.jboss.dmr.client.ModelNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The security context has access to the authorisation meta data and provides policies to reason over it.
 * Each security context is associated with a specific {@link com.gwtplatform.mvp.client.proxy.PlaceRequest}.
 *
 * @see SecurityService
 * @see com.gwtplatform.mvp.client.proxy.PlaceManager
 * @see org.jboss.as.console.spi.AccessControl
 *
 * @author Heiko Braun
 * @date 7/3/13
 */
public class SecurityContext {

    /**
     * the place name token (url)
     */
    private String nameToken;

    /**
     * Set of required resources.
     * Taken form {@link org.jboss.as.console.spi.AccessControl}
     */
    private Set<String> requiredResources;

    /**
     * A list of access constraint definitions
     * (result of :read-resource-description(access-control=true))
     */
    private Map<String, ModelNode> accessConstraints = new HashMap<String,ModelNode>();

    public SecurityContext(String nameToken, Set<String> requiredResources) {
        this.nameToken = nameToken;
        this.requiredResources = requiredResources;
    }

    /**
     * If any of the required resources is not accessible, overall access will be rejected
     * @see org.jboss.as.console.spi.AccessControl
     * @return
     */
    public boolean doesGrantPlaceAccess() {
        boolean accessGranted = true;
        for(String address : requiredResources)
        {
            final ModelNode model = accessConstraints.get(address);
            if(model!=null && !model.get("read-config").asBoolean())
            {
                accessGranted = false;
                break; // the first rule that fails rejects access
            }
        }

        return accessGranted;
    }

    public void updateResourceConstraints(String resourceAddress, ModelNode model) {
        accessConstraints.put(resourceAddress, model);
    }
}
