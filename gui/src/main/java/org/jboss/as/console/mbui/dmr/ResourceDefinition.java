package org.jboss.as.console.mbui.dmr;

import org.jboss.dmr.client.ModelNode;

/**
 * @deprecated Replace with {@link org.jboss.as.console.client.v3.dmr.ResourceDescription}
 * @author Heiko Braun
 * @since 29/08/14
 */
@Deprecated
public class ResourceDefinition extends ModelNode {
    public ResourceDefinition(ModelNode resourceDescription) {
        this.set(resourceDescription);
    }
}
