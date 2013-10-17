package org.jboss.as.console.mbui.marshall.adapters;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.impl.DOMUtils;
import org.jboss.as.console.mbui.marshall.ElementAdapter;
import org.jboss.as.console.mbui.model.mapping.DMRMapping;
import org.jboss.as.console.mbui.model.mapping.ResourceAttribute;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 10/14/13
 */
public class DMRAdapter implements ElementAdapter<DMRMapping>{

    private static final String DMR_NS = "http://wildfly.org/protocol";

    @Override
    public String getElementName() {
        return "dmr";
    }

    @Override
    public DMRMapping fromXML(Node node) {

        DMRMapping mapping = new DMRMapping();
        Node address = node.getAttributes().getNamedItem("address");
        if(address !=null)
        {
            mapping.setAddress(address.getNodeValue());
        }

        NodeList children = node.getChildNodes();
        String[] attributes = new String[children.getLength()];
        for(int i=0; i<children.getLength(); i++)
        {
            Node child = children.item(i);
            if(!( Node.ELEMENT_NODE == child.getNodeType()))
                continue;

            attributes[i] = child.getAttributes().getNamedItem("name").getNodeValue();
        }
        mapping.addAttributes(attributes);
        return mapping;
    }

    @Override
    public Element toXML(Document document, DMRMapping mapping) {
        Element el = DOMUtils.createElementNS(
                document,
                DMR_NS,
                "dmr");

        if(mapping.getAddress()!=null)
            el.setAttribute("address", mapping.getAddress());

        List<ResourceAttribute> attributes = mapping.getAttributes();
        for(ResourceAttribute att : attributes)
        {
            Element attEl =  DOMUtils.createElementNS(document, DMR_NS,"attribute");
            attEl.setAttribute("name", att.getName());
            if(att.getLabel()!=null) attEl.setAttribute("label", att.getLabel());
            el.appendChild(attEl);
        }
        return el;
    }

    @Override
    public Class<?> getType() {
        return DMRMapping.class;
    }
}
