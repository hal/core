package org.jboss.as.console.mbui.marshall.adapters;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import org.jboss.as.console.mbui.marshall.ElementAdapter;
import org.jboss.as.console.mbui.model.StereoTypes;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
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

        String op = ParseUtils.failSafe(node.getAttributes().getNamedItem("operator"), TemporalOperator.Concurrency.toString());

        Container form = new Container(
                USEWARE, node.getAttributes().getNamedItem("id").getNodeValue(),
                label,
                TemporalOperator.valueOf(op),
                StereoTypes.Form);
        return form;
    }

    @Override
    public Element toXML(Document document, InteractionUnit unit) {
        Element el = document.createElement(getElementName());
        el.setAttribute("id", unit.getId().toString());
        el.setAttribute("label", unit.getLabel());

        if(unit instanceof Container)
        {
            Container container = (Container)unit;
            if(container.getTemporalOperator()!=null) {
                el.setAttribute("operator", container.getTemporalOperator().toString());
            }
        }

        return el;
    }

    @Override
    public Class<?> getType() {
        return InteractionUnit.class;
    }
}
