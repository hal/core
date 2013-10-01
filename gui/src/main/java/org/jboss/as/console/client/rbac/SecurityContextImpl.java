package org.jboss.as.console.client.rbac;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.safehtml.shared.SafeHtml;
import org.jboss.ballroom.client.rbac.AuthorisationDecision;
import org.jboss.ballroom.client.rbac.SecurityContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The security context has access to the authorisation meta data and provides policies to reason over it.
 * Each security context is associated with a specific {@link com.gwtplatform.mvp.client.proxy.PlaceRequest}.
 *
 * @see org.jboss.ballroom.client.rbac.SecurityService
 * @see com.gwtplatform.mvp.client.proxy.PlaceManager
 *
 * @author Heiko Braun
 * @date 7/3/13
 */
public class SecurityContextImpl implements SecurityContext {

    /**
     * the place name token (url)
     */
    String nameToken;

    /**
     * Set of required resources.
     * Taken from access control meta data
     */
    Set<String> requiredResources;

    /**
     * A list of access constraint definitions
     * (result of :read-resource-description(access-control=true))
     */
    Map<String, Constraints> accessConstraints = new HashMap<String, Constraints>();

    /**
     * A sealed context cannot be modified
     */
    private boolean sealed;


    public SecurityContextImpl(String nameToken, Set<String> requiredResources) {
        this.nameToken = nameToken;
        this.requiredResources = requiredResources;
    }

    public SafeHtml asHtml() {
        return RBACUtil.dump(this);
    }

    public interface Priviledge {
        boolean isGranted(Constraints c);
    }

    private AuthorisationDecision checkPriviledge(Priviledge p) {

        if(!sealed)
            throw new RuntimeException("Should be sealed before policy decisions are evaluated");

        AuthorisationDecision decision = new AuthorisationDecision(true);
        for(String address : requiredResources)
        {
            final Constraints model = accessConstraints.get(address);
            if(model!=null)
            {
                if(!p.isGranted(model))
                {
                    decision.getErrorMessages().add(address);
                }
            }
            else
            {
                decision.getErrorMessages().add("Missing constraints for "+ address);
            }

            if(decision.hasErrorMessages())
            {
                decision.setGranted(false);
                break;
            }
        }

        return decision;
    }

    /**
     * If any of the required resources is not accessible, overall access will be rejected
     * @return
     */
    public AuthorisationDecision getReadPriviledge() {

        return checkPriviledge(new Priviledge() {
            @Override
            public boolean isGranted(Constraints c) {

                boolean readable = c.isReadResource();
                if(!readable)
                    Log.info("read privilege denied for: " + c.getResourceAddress());
                return readable;
            }
        });

    }

    @Override
    public AuthorisationDecision getReadPrivilege(String resourceAddress) {
        Constraints constraints = getConstraints(resourceAddress);
        return new AuthorisationDecision(constraints.isReadResource());
    }

    public AuthorisationDecision getWritePriviledge() {
        return checkPriviledge(new Priviledge() {
            @Override
            public boolean isGranted(Constraints c) {
                boolean writable = c.isWriteResource();
                if(!writable)
                    Log.info("write privilege denied for: "+c.getResourceAddress());

                return writable;
            }
        });
    }

    @Override
    public AuthorisationDecision getWritePrivilege(String resourceAddress) {
        Constraints constraints = getConstraints(resourceAddress);
        return new AuthorisationDecision(constraints.isWriteResource());
    }

    public AuthorisationDecision getAttributeWritePriviledge(final String name) {
        return checkPriviledge(new Priviledge() {
            @Override
            public boolean isGranted(Constraints c) {
                return c.isAttributeWrite(name);
            }
        });
    }

    @Override
    public AuthorisationDecision getAttributeWritePriviledge(String resourceAddress, String attributeName) {

        Constraints constraints = getConstraints(resourceAddress);
        Constraints.AttributePerm attributePerm = constraints.attributePermissions.get(attributeName);

        if(null==attributePerm)
            throw new RuntimeException("No such attribute: "+ attributeName);

        return new AuthorisationDecision(attributePerm.isWrite());
    }

    private Constraints getConstraints(String resourceAddress) {
        Constraints constraints = accessConstraints.get(resourceAddress);
        if(constraints!=null) throw new RuntimeException("Missing constraints for "+resourceAddress+". Make sure the resource address matches the @AccessControl annotation");
        return constraints;
    }

    void updateResourceConstraints(String resourceAddress, Constraints model) {

        if(sealed)
            throw new RuntimeException("Sealed security context cannot be modified");

        accessConstraints.put(resourceAddress, model);
    }

    public void seal() {
        this.sealed = true;

        // TODO: move all policies that can be evaluated once into this method
    }

    @Override
    public AuthorisationDecision getOperationPriviledge(final String resourceAddress, final String operationName) {

        Constraints constraints = getConstraints(resourceAddress);
        boolean execPerm = constraints.isOperationExec(resourceAddress, operationName);
        AuthorisationDecision descision = new AuthorisationDecision(true);
        descision.setGranted(execPerm);
        return descision;
    }

    Map<String, Constraints> getAccessConstraints() {
        return accessConstraints;
    }
}

