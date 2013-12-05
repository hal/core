package org.jboss.as.console.mbui.marshall.adapters;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.impl.DOMUtils;
import org.jboss.as.console.mbui.marshall.ElementAdapter;
import org.jboss.as.console.mbui.model.mapping.DMRMapping;
import org.jboss.as.console.mbui.model.mapping.ResourceAttribute;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Heiko Braun
 * @date 10/14/13
 */
public class DMRAdapter implements ElementAdapter<DMRMapping>{

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
        List<String> attributes = new LinkedList<String>();
        List<String> objects = new LinkedList<String>();
        for(int i=0; i<children.getLength(); i++)
        {
            Node child = children.item(i);
            if(!( Node.ELEMENT_NODE == child.getNodeType()))
                continue;

            if(child.getNodeName().equals("attribute"))
            {
                attributes.add(child.getAttributes().getNamedItem("name").getNodeValue());
            }
            else if(child.getNodeName().equals("object"))
            {
                objects.add(child.getAttributes().getNamedItem("name").getNodeValue());
            }
        }
        mapping.addAttributes(attributes);
        mapping.addObjects(objects);
        return mapping;
    }

    @Override
    public Element toXML(Document document, DMRMapping mapping) {
        Element el = DOMUtils.createElementNS(
                document,
                mapping.getId().getNamespaceURI(),
                mapping.getId().getLocalPart());

        if(mapping.getAddress()!=null)
            el.setAttribute("address", mapping.getAddress());

        List<ResourceAttribute> attributes = mapping.getAttributes();
        for(ResourceAttribute att : attributes)
        {
            Element attEl =  DOMUtils.createElementNS(document, mapping.getId().getNamespaceURI(),"attribute");
            attEl.setAttribute("name", att.getName());
            if(att.getLabel()!=null) attEl.setAttribute("label", att.getLabel());
            el.appendChild(attEl);
        }

        List<String> objects = mapping.getObjects();
        for(String objName : objects)
        {
            Element attEl =  DOMUtils.createElementNS(document, mapping.getId().getNamespaceURI(),"object");
            attEl.setAttribute("name", objName);
            el.appendChild(attEl);
        }
        return el;
    }

    @Override
    public Class<?> getType() {
        return DMRMapping.class;
    }
}
