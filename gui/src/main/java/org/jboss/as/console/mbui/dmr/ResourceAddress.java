package org.jboss.as.console.mbui.dmr;

import org.jboss.as.console.mbui.model.mapping.AddressMapping;
import org.jboss.as.console.mbui.widgets.AddressUtils;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @since 29/08/14
 */
public class ResourceAddress extends ModelNode {

    private final String addressString ;
    private StatementContext context;
    public ResourceAddress(String addressString, StatementContext context) {

        this.addressString = addressString;
        this.context = context;

        ModelNode address = AddressMapping.fromString(addressString).asResource(context);
        this.set(address);
    }

    @Override
    public String toString() {
        return AddressUtils.toString(this, true);
    }

    public ModelNode apply(ModelNode data) {

        if(!data.hasDefined(NAME))
            throw new IllegalArgumentException("Attribute 'name' is missing");

        ModelNode op = AddressMapping.fromString(addressString).asResource(context, data.get(NAME).asString());
        List<Property> atts = data.asPropertyList();
        for (Property att : atts) {
            op.get(att.getName()).set(att.getValue());
        }
        return op;
    }
}
