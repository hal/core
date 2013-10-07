package org.jboss.as.console.mbui.marshall;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.XMLParser;
import org.jboss.as.console.mbui.model.mapping.DMRMapping;
import org.jboss.as.console.mbui.model.mapping.ResourceAttribute;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.mapping.MappingType;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.builder.InteractionUnitVisitor;

import java.util.List;
import java.util.Stack;

/**
 * @author Heiko Braun
 * @date 6/13/13
 */
public class Marshaller {

    public Document marshall(Dialog dialog) {

        DMRMarshallRoutine marshaller = new DMRMarshallRoutine(dialog);
        dialog.getInterfaceModel().accept(marshaller);
        return marshaller.getResult();
    }

    class DMRMarshallRoutine implements InteractionUnitVisitor
    {

        private final Dialog dialog;
        private Document document;
        private Stack<Node> stack = new Stack<Node>();

        DMRMarshallRoutine(Dialog dialog) {
            this.dialog = dialog;
            this.document = XMLParser.createDocument();
        }

        Document getResult() {
            return document;
        }

        @Override
        public void startVisit(Container container) {

            String name = container.getStereotype()!=null ?
                    container.getStereotype().toString().toLowerCase() : "container";

            Element el = document.createElement(name);

            el.setAttribute("id", container.getId().getLocalPart());
            el.setAttribute("xmlns", container.getId().getNamespaceURI());

            if(container.getTemporalOperator()!=null) {
                el.setAttribute("operator", container.getTemporalOperator().toString());
            }

            marshallMapping(container, el);

            stack.push(el);
        }

        @Override
        public void visit(InteractionUnit unit) {

            String name = unit.getStereotype()!=null ?
                    unit.getStereotype().toString().toLowerCase() : resolveType(unit);

            Element el = document.createElement(name);
            el.setAttribute("id", unit.getId().getLocalPart());

            marshallMapping(unit, el);

            if(stack.isEmpty())
                throw new RuntimeException("Parse error");

            stack.peek().appendChild(el);
        }

        @Override
        public void endVisit(Container container) {
            if(stack.size()>1){
                Node el = stack.pop();
                stack.peek().appendChild(el);
            }
            else if(stack.size()==1)
            {
                Node root = stack.pop();

                Element dialogEl = document.createElement("dialog");

                dialogEl.setAttribute("id", dialog.getId().getLocalPart());

                Element structure = document.createElement("structure");

                //Element behaviour = document.createElement("behaviour");
                structure.appendChild(root);
                dialogEl.appendChild(structure);
                document.appendChild(dialogEl);


            }
            else
            {
                throw new RuntimeException("Parse error");
            }
        }

        private void marshallMapping(InteractionUnit unit, Element target)
        {
            if(unit.hasMapping(MappingType.DMR))
            {
                DMRMapping mapping = (DMRMapping) unit.getMapping(MappingType.DMR);
                Element el = document.createElement("dmr");

                if(mapping.getAddress()!=null)
                    el.setAttribute("address", mapping.getAddress());

                List<ResourceAttribute> attributes = mapping.getAttributes();
                for(ResourceAttribute att : attributes)
                {
                    Element attEl = document.createElement("attribute");
                    attEl.setAttribute("name", att.getName());
                    if(att.getLabel()!=null) attEl.setAttribute("label", att.getLabel());
                    el.appendChild(attEl);
                }

                target.appendChild(el);
            }
        }
    }

    private static String resolveType(InteractionUnit interactionUnit) {
        String className = interactionUnit.getClass().getName().toLowerCase();
        return className.substring(className.lastIndexOf(".")+1, className.length());
    }


}
