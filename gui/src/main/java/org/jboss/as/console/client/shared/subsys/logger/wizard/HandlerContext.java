package org.jboss.as.console.client.shared.subsys.logger.wizard;

import org.jboss.dmr.client.ModelNode;

/**
 * @author Heiko Braun
 * @since 08/09/15
 */
public class HandlerContext {
    ModelNode attributes;
    ModelNode fileAttribute;

    public HandlerContext() {
    }

    public ModelNode getAttributes() {
        return attributes;
    }

    public void setAttributes(ModelNode attributes) {
        this.attributes = attributes;
    }

    public ModelNode getFileAttribute() {
        return fileAttribute;
    }

    public void setFileAttribute(ModelNode fileAttribute) {
        this.fileAttribute = fileAttribute;
    }
}
