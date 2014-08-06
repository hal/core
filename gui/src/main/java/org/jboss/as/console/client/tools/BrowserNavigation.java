package org.jboss.as.console.client.tools;

import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 24/07/14
 */
public interface BrowserNavigation {
    void onViewChild(ModelNode address, String childName);

    List<Property> getSubaddress(ModelNode address);

    void onRemoveChildResource(ModelNode address, ModelNode selection);

    void onPrepareAddChildResource(ModelNode address, boolean currentSquatting);

    void onAddChildResource(ModelNode address, ModelNode resource);
}
