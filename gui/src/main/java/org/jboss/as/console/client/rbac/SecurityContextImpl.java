package org.jboss.as.console.client.rbac;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.safehtml.shared.SafeHtml;
import org.jboss.ballroom.client.rbac.AuthorisationDecision;
import org.jboss.ballroom.client.rbac.SecurityContext;

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
    Set<ResourceRef> requiredResources;
    /**
     * A list of access constraint definitions
     * (result of :read-resource-description(access-control=true))
     */
    Map<String, Constraints> accessConstraints = new HashMap<String, Constraints>();
    Map<String, Constraints> optionalConstraints = new HashMap<String, Constraints>();
    Map<String, SecurityContext> childContexts = new HashMap<String, SecurityContext>();


    /**
     * A sealed context cannot be modified
     */
    private boolean sealed;

    public SecurityContextImpl(String nameToken, Set<ResourceRef> requiredResources) {
        this.nameToken = nameToken;
        this.requiredResources = requiredResources;
    }

    public SafeHtml asHtml() {
        return RBACUtil.dump(this);
    }

    public interface Priviledge {
        boolean isGranted(Constraints c);
    }

    /**
     * Iterates over all required (and optional) resources, grabs the related constraints and checks if the given
     * privilege for these constraints holds true.
     * @return granted if the privilege holds true for *all* tested resources
     */
    private AuthorisationDecision checkPriviledge(Priviledge p, boolean includeOptional) {

        if(!sealed)
            throw new RuntimeException("Should be sealed before policy decisions are evaluated");

        AuthorisationDecision decision = new AuthorisationDecision(true);
        for(ResourceRef ref : requiredResources)
        {
            if(ref.optional) continue; // skip optional ones

            final Constraints model = getConstraints(ref.address, includeOptional);
            if(model!=null)
            {
                if(!p.isGranted(model))
                {
                    decision.getErrorMessages().add(ref.address);
                }
            }
            else
            {
                decision.getErrorMessages().add("Missing constraints for "+ ref.address);
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
        }, false);

    }

    @Override
    public AuthorisationDecision getReadPrivilege(String resourceAddress) {
        Constraints constraints = getConstraints(resourceAddress, false);
        return new AuthorisationDecision(constraints.isReadResource());
    }

    @Override
    public AuthorisationDecision getWritePriviledge() {
        return checkPriviledge(new Priviledge() {
            @Override
            public boolean isGranted(Constraints c) {
                boolean writable = c.isWriteResource();
                if(!writable)
                    Log.info("write privilege denied for: "+c.getResourceAddress());

                return writable;
            }
        }, false);
    }

    @Override
    public AuthorisationDecision getWritePrivilege(String resourceAddress) {
        Constraints constraints = getConstraints(resourceAddress, false);
        return new AuthorisationDecision(constraints.isWriteResource());
    }

    public AuthorisationDecision getAttributeWritePriviledge(final String name) {
        return checkPriviledge(new Priviledge() {
            @Override
            public boolean isGranted(Constraints c) {
                return c.isAttributeWrite(name);
            }
        }, true);
    }

    @Override
    public AuthorisationDecision getAttributeWritePriviledge(String resourceAddress, String attributeName) {

        Constraints constraints = getConstraints(resourceAddress, true);
        Constraints.AttributePerm attributePerm = constraints.attributePermissions.get(attributeName);

        if(null==attributePerm)
            throw new RuntimeException("No such attribute: "+ attributeName);

        return new AuthorisationDecision(attributePerm.isWrite());
    }

    private Constraints getConstraints(String resourceAddress, boolean includeOptional) {
        Constraints constraints;
        if (includeOptional) {
            constraints = accessConstraints.containsKey(resourceAddress) ?
                    accessConstraints.get(resourceAddress) : optionalConstraints.get(resourceAddress);
        } else {
            constraints = accessConstraints.get(resourceAddress);
        }

        // at least here must have found something!
        if (null == constraints) {
            throw new RuntimeException(
                    "Missing constraints for " + resourceAddress + ". Make sure the resource address matches the @AccessControl annotation");
        }
        return constraints;
    }

    void setConstraints(String resourceAddress, Constraints model) {
        if (sealed) { throw new RuntimeException("Sealed security context cannot be modified"); }
        accessConstraints.put(resourceAddress, model);
    }

    void setOptionalConstraints(String resourceAddress, Constraints model) {
        if (sealed) { throw new RuntimeException("Sealed security context cannot be modified"); }
        optionalConstraints.put(resourceAddress, model);
    }

    void addChildContext(String resourceAddress, Constraints model) {
        if (sealed) { throw new RuntimeException("Sealed security context cannot be modified"); }
        ChildContext childContext = new ChildContext(resourceAddress, model);
        childContexts.put(resourceAddress, childContext);
    }

    @Override
    public boolean hasChildContext(final String resourceAddress) {
        return resourceAddress != null && childContexts.containsKey(resourceAddress);
    }

    @Override
    public SecurityContext getChildContext(String resourceAddress) {
        return childContexts.get(resourceAddress);
    }

    public void seal() {
        this.sealed = true;

        // TODO: move all policies that can be evaluated once into this method
    }

    @Override
    public AuthorisationDecision getOperationPriviledge(final String resourceAddress, final String operationName) {

        Constraints constraints = getConstraints(resourceAddress, true);
        boolean execPerm = constraints.isOperationExec(resourceAddress, operationName);
        AuthorisationDecision descision = new AuthorisationDecision(true);
        descision.setGranted(execPerm);
        return descision;
    }

    /**
     * Security context which solely is based on the resource address specified as constructor parameter. The methods
     * which take a resource address as parameter don't operate on that address, but the one given as constructor
     * parameter.
     */
    static class ChildContext implements SecurityContext {

        private final String resourceAddress;
        private final Constraints constraints;

        ChildContext(final String resourceAddress, final Constraints constraints) {
            this.resourceAddress = resourceAddress;
            this.constraints = constraints;
        }

        @Override
        public AuthorisationDecision getReadPriviledge() {
            return new AuthorisationDecision(constraints.isReadResource());
        }

        @Override
        public AuthorisationDecision getReadPrivilege(final String resourceAddress) {
            return getReadPriviledge();
        }

        @Override
        public AuthorisationDecision getWritePriviledge() {
            return new AuthorisationDecision(constraints.isWriteResource());
        }

        @Override
        public AuthorisationDecision getWritePrivilege(final String resourceAddress) {
            return getWritePriviledge();
        }

        @Override
        public AuthorisationDecision getAttributeWritePriviledge(final String attributeName) {
            Constraints.AttributePerm attributePerm = constraints.attributePermissions.get(attributeName);
            if (attributePerm == null) { throw new RuntimeException("No such attribute: " + attributeName); }
            return new AuthorisationDecision(attributePerm.isWrite());
        }

        @Override
        public AuthorisationDecision getAttributeWritePriviledge(final String resourceAddress,
                final String attributeName) {
            return getAttributeWritePriviledge(attributeName);
        }

        @Override
        public AuthorisationDecision getOperationPriviledge(final String resourceAddress, final String operationName) {
            boolean execPerm = constraints.isOperationExec(this.resourceAddress, operationName);
            AuthorisationDecision descision = new AuthorisationDecision(true);
            descision.setGranted(execPerm);
            return descision;
        }

        /**
         * @return false
         */
        @Override
        public boolean hasChildContext(final String resourceAddress) {
            return false;
        }

        /**
         * @return null
         */
        @Override
        public SecurityContext getChildContext(final String resourceAddress) {
            return null;
        }

        /**
         * Sealed by default
         */
        @Override
        public void seal() {}
    }
}

