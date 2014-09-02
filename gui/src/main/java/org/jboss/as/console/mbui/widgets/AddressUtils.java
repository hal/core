package org.jboss.as.console.mbui.widgets;

import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 31/07/14
 *
 * TODO (hbraun): this should be factored into an address concept
 */
public class AddressUtils {

    public static ModelNode toFqAddress(ModelNode address, String resourceName)
    {
        ModelNode fqAddress = new ModelNode();
        List<Property> tuples = address.asPropertyList();
        for(Property tuple : tuples)
        {
            String key = tuple.getName();
            String value = tuple.getValue().asString();

            fqAddress.add(key, value.equals("*") ? resourceName : value);
        }

        return fqAddress;
    }

    public static String toString(ModelNode address, boolean fq) {

        List<Property> tuples = address.asPropertyList();
        StringBuilder sb = new StringBuilder();
        int i=0;
        for (final Property tuple : tuples) {
            sb.append("/");

            sb.append(tuple.getName());
            sb.append("=");

            if(i==tuples.size()-1)
                if(fq)
                    sb.append(tuple.getValue().asString());
                else
                    sb.append("*");
            else
                sb.append(tuple.getValue().asString());

            i++;
        }
        return sb.toString();
    }

    public static String asKey(ModelNode address, boolean fq) {

        List<Property> tuples = address.asPropertyList();
        StringBuilder sb = new StringBuilder();

        int i=0;
        for (final Property tuple : tuples) {
            sb.append("/");

            sb.append(tuple.getName());
            sb.append("=");

            if(i==tuples.size()-1)
                if(fq)
                    sb.append(tuple.getValue().asString());
                else
                    sb.append("*");
            else
                sb.append(tuple.getValue().asString());

            i++;
        }

        if(tuples.isEmpty())
            sb.append("ROOT"); // better then empty string

        return sb.toString();
    }
}
