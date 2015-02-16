package org.jboss.as.console.mbui.dmr;

import org.jboss.as.console.mbui.model.mapping.AddressMapping;
import org.jboss.as.console.mbui.widgets.AddressUtils;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * NOTE: Stores the actual address as DMR property 'address'.
 *
 * @author Heiko Braun
 * @since 29/08/14
 */
public class ResourceAddress extends ModelNode {

    public ResourceAddress(String addressTemplate, StatementContext context) {
        ModelNode resolved = AddressMapping.fromString(addressTemplate).asResource(context);
        set(resolved); // resolved is a model node which contains ADDRESS
    }

    public ResourceAddress(String addressTemplate, ModelNode baseAddress, StatementContext context) {
        ModelNode resolved = AddressMapping.fromString(addressTemplate).asResource(baseAddress, context);
        set(resolved); // resolved is a model node which contains ADDRESS
    }

    public List<Property> asTokens() {
        return get(ADDRESS).asPropertyList();
    }

    public ModelNode asOperation(ModelNode data, String name) {

        ModelNode op = asFqAddress(name);
        List<Property> atts = data.asPropertyList();
        for (Property att : atts) {
            op.get(att.getName()).set(att.getValue());
        }
        return op;
    }

    public ModelNode asOperation(ModelNode data) {

        if(!data.hasDefined(NAME))
            throw new IllegalArgumentException("Attribute 'name' is missing");

        return asOperation(data, data.get(NAME).asString());
    }

    public ModelNode asFqAddress(String resourceName)
    {
        ModelNode fqAddress = new ModelNode();
        List<Property> tuples = get(ADDRESS).asPropertyList();
        for(Property tuple : tuples)
        {
            String key = tuple.getName();
            String value = tuple.getValue().asString();

            fqAddress.get(ADDRESS).add(key, value.equals("*") ? resourceName : value);
        }

        return fqAddress;
    }

    @Override
    public String toString() {
        return AddressUtils.toString(this.get(ADDRESS), true);
    }

    public String getResourceType() {
        List<Property> tokens = asTokens();
        return tokens.get(tokens.size() - 1).getName();
    }
}
