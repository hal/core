package org.jboss.as.console.client.rbac;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.ballroom.client.rbac.AuthorisationDecision;
import org.jboss.ballroom.client.rbac.SecurityContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The security context has access to the authorisation meta data and provides policies to reason over it.
 * Each security context is associated with a specific ID (usually the place token).
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
     * Set of {@link org.jboss.as.console.spi.RequiredResources}
     */
    Set<AddressTemplate> requiredResources;

    /**
     * A list of access constraint definitions
     * (result of :read-resource-description(access-control=true))
     */
    private Map<AddressTemplate, Map<String,Constraints>> accessConstraints = new HashMap<>();
    private Map<AddressTemplate, String> activeConstraints = new HashMap<>();
    private AddressTemplate[] resourceAddresses;

    public SecurityContextImpl(String nameToken, Set<AddressTemplate> requiredResources) {
        this.nameToken = nameToken;
        this.requiredResources = requiredResources;
    }

    public SafeHtml asHtml() {
        try {
            return RBACUtil.dump(this);
        } catch (Throwable e) {
            e.printStackTrace();
            return new SafeHtmlBuilder().appendEscaped(e.getMessage()).toSafeHtml();
        }
    }

    Set<AddressTemplate> getResourceAddresses() {
        return accessConstraints.keySet();
    }

    public Set<String> getConstraintsKeys(AddressTemplate address) {
        return accessConstraints.get(address).keySet();
    }

    public String getActiveKey(AddressTemplate address) {
        return activeConstraints.get(address);
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


        AuthorisationDecision decision = new AuthorisationDecision(true);
        for(AddressTemplate ref : requiredResources)
        {
            if(ref.isOptional()) continue; // skip optional ones

            final Constraints model = getConstraints(ref, includeOptional);
            if(model!=null)
            {
                if(!p.isGranted(model))
                {
                    decision.getErrorMessages().add(ref.toString());
                }
            }
            else
            {
                decision.getErrorMessages().add("Missing constraints for "+ ref.toString());
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
                if (!readable)
                    Log.info("read privilege denied for: " + c.getResourceAddress());
                return readable;
            }
        }, false);

    }

    @Override
    public AuthorisationDecision getReadPrivilege(String resourceAddress) {
        AddressTemplate addr = AddressTemplate.of(resourceAddress);
        Constraints constraints = getConstraints(addr, false);
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
        AddressTemplate addr = AddressTemplate.of(resourceAddress);
        Constraints constraints = getConstraints(addr, false);
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
    public AuthorisationDecision getAttributeReadPriviledge(final String name) {
        return checkPriviledge(new Priviledge() {
             @Override
             public boolean isGranted(Constraints c) {
                 return c.isAttributeRead(name);
             }
         }, true);
    }

    @Override
    public AuthorisationDecision getAttributeWritePriviledge(String resourceAddress, String attributeName) {
        AddressTemplate addr = AddressTemplate.of(resourceAddress);
        Constraints constraints = getConstraints(addr, true);
        Constraints.AttributePerm attributePerm = constraints.attributePermissions.get(attributeName);

        if(null==attributePerm)
            throw new RuntimeException("No such attribute: "+ attributeName);

        return new AuthorisationDecision(attributePerm.isWrite());
    }

    @Override
    public AuthorisationDecision getAttributeReadPriviledge(String resourceAddress, String attributeName) {
        AddressTemplate addr = AddressTemplate.of(resourceAddress);
        Constraints constraints = getConstraints(addr, true);
        Constraints.AttributePerm attributePerm = constraints.attributePermissions.get(attributeName);

        if(null==attributePerm)
            throw new RuntimeException("No such attribute: "+ attributeName);

        return new AuthorisationDecision(attributePerm.isRead());
    }

    Constraints getActiveConstraints(AddressTemplate address) {

        String key = activeConstraints.containsKey(address) ? activeConstraints.get(address) : address.getTemplate(); // default
        Map<String, Constraints> availableConstraints = accessConstraints.get(address);
        Constraints constraints = availableConstraints.get(key);
        return constraints;
    }

    Constraints getConstraints(AddressTemplate resourceAddress, boolean includeOptional) {

        Constraints constraints = getActiveConstraints(resourceAddress);

        // at least here we must have found something!
        if (null == constraints) {
            throw new RuntimeException(
                    "Missing constraints for " + resourceAddress + ". Make sure the resource address matches the @AccessControl annotation");
        }
        return constraints;
    }

    public void addConstraints(AddressTemplate resourceAddress, Constraints model) {

        if(!accessConstraints.containsKey(resourceAddress))
            accessConstraints.put(resourceAddress, new HashMap<>());


        // by default the orig template will act as key
        Map<String, Constraints> scope = accessConstraints.get(resourceAddress);

        String resolvedKey = resourceAddress.getTemplate();
        if(scope.containsKey(resolvedKey))
            throw new IllegalStateException("Constraints already registered: "+resolvedKey);

        scope.put(resolvedKey, model);
    }

    public void addChildContext(AddressTemplate resourceAddress, String resolvedKey, Constraints constraints) {

        if(!accessConstraints.containsKey(resourceAddress))
            throw new IllegalStateException("Missing parent context for address "+ resourceAddress);

        // link parent
        constraints.setParent(resourceAddress);

        Map<String, Constraints> scope = accessConstraints.get(resourceAddress);

        if(scope.containsKey(resolvedKey)) {
            new IllegalStateException("Child context already exists, skipping: " + resolvedKey).printStackTrace();
            return;
        }

        scope.put(resolvedKey, constraints);
    }

    @Override
    public boolean hasChildContext(final Object resourceAddress, String resolvedKey) {
        return resourceAddress != null && accessConstraints.get((AddressTemplate)resourceAddress).get(resolvedKey)!=null;
    }

    @Override
    public void activateChildContext(Object resourceAddress, String resolvedKey) {
        if(null==resolvedKey)
            activeConstraints.remove(resourceAddress);
        else
            activeConstraints.put((AddressTemplate) resourceAddress, resolvedKey);
    }

    public void seal() {
       // not used anymore
    }

    @Override
    public AuthorisationDecision getOperationPriviledge(final String resourceAddress, final String operationName) {
        AddressTemplate addr = AddressTemplate.of(resourceAddress);
        Constraints constraints = getConstraints(addr, true);

        // the constraints resolved at this point can be child constraints,
        // i.e. a specific server or server-group
        // the provided operation address however points to a parent resource (unspecific)
        // but since we can assume that the resolved constraints are correct, we simply match the operation perms
        // by the resource type, opposed to the full resource address.

        boolean execPerm = constraints.isOperationExec(addr, operationName);
        AuthorisationDecision decision = new AuthorisationDecision(true);
        decision.setGranted(execPerm);
        return decision;
    }

}

