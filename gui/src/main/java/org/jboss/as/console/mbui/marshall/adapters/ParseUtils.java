package org.jboss.as.console.mbui.marshall.adapters;

import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;

/**
 * @author Heiko Braun
 * @date 10/14/13
 */
public class ParseUtils {

    public static String IDOrLabel(Node node) {
        String label = node.getAttributes().getNamedItem("label")!=null ?
                node.getAttributes().getNamedItem("label").getNodeValue() : node.getAttributes().getNamedItem("id").getNodeValue();

        return label;

    }

    public static String failSafe(Node node, String fallback)
    {
        return node !=null ? node.getNodeValue() : fallback;
    }
}
