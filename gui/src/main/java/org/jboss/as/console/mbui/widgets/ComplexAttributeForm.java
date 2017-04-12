package org.jboss.as.console.mbui.widgets;

import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.ballroom.client.rbac.AuthorisationDecision;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.ModelNode;

import static org.jboss.dmr.client.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.dmr.client.ModelDescriptionConstants.VALUE_TYPE;

/**
 *
 * Matches complex attribute with the following pattern: typ=Object and value-type=properties.
 * For instance:
 *
 * <pre>
 *
 *       "file" => {
 *                    <b>"type" => OBJECT</b>,
 *                    [...]
 *                    <b>"value-type"</b> => {
 *                        "relative-to" => {
 *                            "type" => STRING,
 *                            "description" => "...",
 *                            "expressions-allowed" => false,
 *                            "nillable" => true,
 *                            "min-length" => 1L,
 *                            "max-length" => 2147483647L
 *                        },
 *                        "path" => {
 *                            "type" => STRING,
 *                            "description" => "The filesystem path.",
 *                            "expressions-allowed" => true,
 *                            "nillable" => false,
 *                            "min-length" => 1L,
 *                            "max-length" => 2147483647L,
 *                            "filesystem-path" => true
 *                        }
 *                    },
 *                   [...]
 *                }
 * </pre>
 * @author Heiko Braun
 * @since 07/09/15
 */
public class ComplexAttributeForm {

    /**
     * The name of the complex attribute in the parent scope
     */
    private String attributeName;

    /**
     * The parent scope security context
     */
    private SecurityContext securityContextDelegate;

    /**
     * * The parent scope resource description (includes the complex attribute definition)
     */
    private ResourceDescription resourceDescriptionDelegate;
    private String[] excludes;

    public ComplexAttributeForm(String attributeName, SecurityContext securityContextDelegate, ResourceDescription resourceDescriptionDelegate) {

        assert resourceDescriptionDelegate.hasDefined("attributes") : "missing 'attributes' section in description";
        assert resourceDescriptionDelegate.get("attributes").get(attributeName).hasDefined("value-type") : "missing 'value-type' section in description";

        this.attributeName = attributeName;
        this.securityContextDelegate = securityContextDelegate;
        this.resourceDescriptionDelegate = resourceDescriptionDelegate;
    }

    private ResourceDescription getAttributeDescription() {
        ModelNode desc = new ModelNode();
        desc.get(ATTRIBUTES).set(resourceDescriptionDelegate.get(ATTRIBUTES).get(attributeName).get(VALUE_TYPE));
        return new ResourceDescription(desc);
    }

    /**
     * Simply delegates all auth decision to the parent context attribute scope represented by {@link #attributeName}
     * @return
     */
    private SecurityContext getSecurityContext() {
        return new SecurityContext() {
            @Override
            public AuthorisationDecision getReadPriviledge() {
                return securityContextDelegate.getReadPriviledge();
            }

            @Override
            public AuthorisationDecision getWritePriviledge() {
                return securityContextDelegate.getWritePriviledge();
            }

            @Override
            public AuthorisationDecision getAttributeWritePriviledge(String s) {
                return securityContextDelegate.getAttributeWritePriviledge(attributeName);
            }

            @Override
            public AuthorisationDecision getAttributeReadPriviledge(String s) {
                return securityContextDelegate.getAttributeReadPriviledge(attributeName);
            }

            @Override
            public AuthorisationDecision getAttributeWritePriviledge(String resourceAddress, String attributeName) {
                return securityContextDelegate.getAttributeWritePriviledge(attributeName);
            }

            @Override
            public AuthorisationDecision getAttributeReadPriviledge(String resourceAddress, String attributeName) {
                return securityContextDelegate.getAttributeReadPriviledge(attributeName);
            }

            @Override
            public AuthorisationDecision getReadPrivilege(String resourceAddress) {
                return securityContextDelegate.getAttributeReadPriviledge(attributeName);
            }

            @Override
            public AuthorisationDecision getWritePrivilege(String resourceAddress) {
                return securityContextDelegate.getAttributeWritePriviledge(attributeName);
            }

            @Override
            public AuthorisationDecision getOperationPriviledge(String resourceAddress, String operationName) {
                return securityContextDelegate.getOperationPriviledge(resourceAddress, operationName);
            }

            @Override
            public boolean hasChildContext(Object s, String resolvedKey) {
                return false;
            }

            @Override
            public void activateChildContext(Object resourceAddress, String resolvedKey) {

            }

            @Override
            public void seal() {
                securityContextDelegate.seal();
            }
        };
    }

    public ModelNodeFormBuilder.FormAssets build() {
        return builder().build();
    }

    public ModelNodeFormBuilder builder() {
        ResourceDescription attributeDescription = getAttributeDescription();

        return new ModelNodeFormBuilder()
                .setConfigOnly()
                .setResourceDescription(attributeDescription)
                .setSecurityContext(getSecurityContext())
                .exclude(excludes);
    }

    /**
     * Exclude any attribute from the automatic form mapping.
     *
     * @param attributes The list of attribute names.
     * @return this This ComplexAttributeForm instance.
     */
    public ComplexAttributeForm exclude(String... attributes) {
        excludes = attributes;
        return this;
    }
}
