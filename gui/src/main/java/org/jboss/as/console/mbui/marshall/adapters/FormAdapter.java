package org.jboss.as.console.mbui.marshall.adapters;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.impl.DOMUtils;
import org.jboss.as.console.mbui.marshall.ElementAdapter;
import org.jboss.as.console.mbui.model.StereoTypes;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;
import org.useware.kernel.model.structure.TemporalOperator;

/**
 * @author Heiko Braun
 * @date 10/14/13
 */
public class FormAdapter implements ElementAdapter<InteractionUnit> {

    @Override
    public String getElementName() {
        return "form";
    }

    @Override
    public InteractionUnit fromXML(Node node) {
        String label = ParseUtils.IDOrLabel(node);

        //QName id = QName.valueOf(node.getAttributes().getNamedItem("id").getNodeValue());
        QName id = new QName(node.getNamespaceURI(), node.getAttributes().getNamedItem("id").getNodeValue());

        Container form = new Container(
                id.getNamespaceURI(), id.getLocalPart(),
                label,
                TemporalOperator.Concurrency,
                StereoTypes.Form);
        return form;
    }

    @Override
    public Element toXML(Document document, InteractionUnit unit) {
        Element el = DOMUtils.createElementNS(document, unit.getId().getNamespaceURI(), getElementName());
        el.setAttribute("id", unit.getId().getLocalPart());
        el.setAttribute("label", unit.getLabel());
        return el;
    }

    @Override
    public Class<?> getType() {
        return InteractionUnit.class;
    }
}
