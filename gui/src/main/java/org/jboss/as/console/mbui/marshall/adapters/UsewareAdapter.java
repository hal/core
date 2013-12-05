package org.jboss.as.console.mbui.marshall.adapters;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.impl.DOMUtils;
import org.jboss.as.console.mbui.marshall.ElementAdapter;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.Input;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.Link;
import org.useware.kernel.model.structure.Output;
import org.useware.kernel.model.structure.QName;
import org.useware.kernel.model.structure.Select;
import org.useware.kernel.model.structure.TemporalOperator;
import org.useware.kernel.model.structure.Trigger;

/**
 * @author Heiko Braun
 * @date 10/14/13
 */
public class UsewareAdapter implements ElementAdapter<InteractionUnit> {

    private String elementName;

    public UsewareAdapter(String elementName) {
        this.elementName = elementName;
    }

    @Override
    public String getElementName() {
        return elementName;
    }

    @Override
    public InteractionUnit fromXML(Node node) {
        return createUnitByName(elementName, node);
    }

    @Override
    public Element toXML(Document document, InteractionUnit unit) {

        Element el = DOMUtils.createElementNS(document, unit.getId().getNamespaceURI(), elementName);
        el.setAttribute("id", unit.getId().getLocalPart());
        el.setAttribute("label", unit.getLabel());

        if(unit instanceof Container)
        {
            Container container = (Container)unit;
            if(container.getTemporalOperator()!=null) {
                el.setAttribute("operator", container.getTemporalOperator().toString());
            }
        }
        else if(unit instanceof Trigger)
        {
            Trigger trigger = (Trigger)unit;
            el.setAttribute("type", trigger.getType().toString());
        }
        else if(unit instanceof Link)
        {
            Link link = (Link)unit;
            el.setAttribute("target", link.getTarget().toString());
        }

        return el;
    }

    private InteractionUnit createUnitByName(String name, Node node)
    {
        InteractionUnit unit = null;

        //QName.valueOf(node.getAttributes().getNamedItem("id").getNodeValue());
        QName id = new QName(node.getNamespaceURI(), node.getAttributes().getNamedItem("id").getNodeValue());
        String label = ParseUtils.IDOrLabel(node);

        if("container".equals(name))
        {
            String op = ParseUtils.failSafe(node.getAttributes().getNamedItem("operator"), TemporalOperator.Concurrency.toString());
            unit = new Container(
                    id.getNamespaceURI(), id.getLocalPart(),
                    label,
                    TemporalOperator.valueOf(op)
            );

        }
        else if("input".equals(name))
        {
            unit = new Input(id.getNamespaceURI(), id.getLocalPart(),
                    label);

        }
        else if("output".equals(name))
        {

            unit = new Output(id.getNamespaceURI(), id.getLocalPart(),
                    label);
        }
        else if("select".equals(name))
        {
            unit = new Select(id.getNamespaceURI(), id.getLocalPart(),
                    label);
        }
        else if("trigger".equals(name))
        {
            unit = new Trigger(id.getNamespaceURI(), id.getLocalPart(),
                    QName.valueOf(ParseUtils.failSafe(node.getAttributes().getNamedItem("type"), "")),
                    label);
        }
        else if("link".equals(name))
        {
            unit = new Link(id.getNamespaceURI(), id.getLocalPart(),
                    QName.valueOf(ParseUtils.failSafe(node.getAttributes().getNamedItem("target"), "")),
                    label);
        }
        return unit;
    }


    @Override
    public Class<?> getType() {
        return InteractionUnit.class;
    }
}
