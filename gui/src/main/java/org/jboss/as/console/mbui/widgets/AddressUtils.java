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

    public static ModelNode fromFqAddress(ModelNode address)
    {
        ModelNode wildcardAddress = new ModelNode();
        List<Property> tuples = address.asPropertyList();
        int i=0;
        for(Property tuple : tuples)
        {
            String key = tuple.getName();
            String value = tuple.getValue().asString();

            if(i==tuples.size()-1)
                wildcardAddress.add(key, "*");
            else
                wildcardAddress.add(key, value);

            i++;
        }


        return wildcardAddress;
    }

    public static String getDenominatorType(List<Property> addressTuple) {

        int i=1;

        final ModelNode addressPrefix = new ModelNode();
        Property denominator = null;
        for(Property tuple : addressTuple)
        {
            if(i==addressTuple.size())
            {
                denominator = tuple;
                break;
            }
            else
            {
                addressPrefix.add(tuple.getName(), tuple.getValue());
            }

            i++;
        }
        return denominator!=null ? denominator.getName() : null;
    }

    public static String toString(ModelNode address, boolean fq) {

        List<Property> tuples = address.asPropertyList();
        StringBuilder sb = new StringBuilder();
        int i=0;
        for (final Property tuple : tuples) {
            if(i>0) sb.append("/");

            sb.append(tuple.getName());
            sb.append("=");

            if(i==tuples.size()-1)
                if(fq)
                    sb.append(escapeValue(tuple.getValue().asString()));
                else
                    sb.append("*");
            else
                sb.append(escapeValue(tuple.getValue().asString()));

            i++;
        }
        return sb.toString();
    }

    public static String asKey(ModelNode address, boolean fq) {

        List<Property> tuples = address.asPropertyList();
        StringBuilder sb = new StringBuilder();

        int i=0;
        for (final Property tuple : tuples) {
            if(i>0) sb.append("/");

            sb.append(tuple.getName());
            sb.append("=");

            if(i==tuples.size()-1)
                if(fq)
                    sb.append(escapeValue(tuple.getValue().asString()));
                else
                    sb.append("*");
            else
                sb.append(escapeValue(tuple.getValue().asString()));

            i++;
        }

        if(tuples.isEmpty())
            sb.append("ROOT"); // better then empty string

        return sb.toString();
    }

    private static String escapeValue(String addressSegment) {
        return addressSegment
                .replace("/", "\\/")
                .replace(":", "\\:");
    }

}
