package org.jboss.as.console.mbui.marshall;

import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.XMLParser;
import org.jboss.as.console.mbui.model.mapping.DMRMapping;
import org.jboss.dmr.client.ModelNode;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.mapping.MappingType;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.builder.InteractionUnitVisitor;

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

        private Document document;
        private Stack<Node> stack = new Stack<Node>();

        DMRMarshallRoutine(Dialog dialog) {
            document = XMLParser.createDocument();

            /*Element root = document.createElement(dialog.getId().getLocalPart());
            root.setAttribute("xmlns", dialog.getId().getNamespaceURI());
            stack.push(root);*/
        }

        Document getResult() {
            return document;
        }

        @Override
        public void startVisit(Container container) {
            Element el = document.createElement("container");

            el.setAttribute("id", container.getId().getLocalPart());
            el.setAttribute("xmlns", container.getId().getNamespaceURI());

            if(container.getStereotype()!=null)
                el.setAttribute("stereo-type", container.getStereotype().toString());


            if(container.getTemporalOperator()!=null) {
                el.setAttribute("operator", container.getTemporalOperator().toString());
            }

            //marshallMapping(container, containerNode);   // TODO

            stack.push(el);
        }

        @Override
        public void visit(InteractionUnit unit) {

            Element el = document.createElement(resolveType(unit));
            el.setAttribute("id", unit.getId().getLocalPart());

            if(unit.getStereotype()!=null)
                el.setAttribute("stereo-type", unit.getStereotype().toString());

            //marshallMapping(unit, item); // TODO

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
                document.appendChild(stack.pop());
            }
            else
            {
                throw new RuntimeException("Parse error");
            }
        }
    }

    private String resolveType(InteractionUnit interactionUnit) {
        String className = interactionUnit.getClass().getName().toLowerCase();
        return className.substring(className.lastIndexOf(".")+1, className.length());
    }

    private void marshallMapping(InteractionUnit unit, ModelNode target)
    {
        if(unit.hasMapping(MappingType.DMR))
        {
            ModelNode mappingNode = new ModelNode();
            DMRMapping mapping = (DMRMapping) unit.getMapping(MappingType.DMR);
            mappingNode.get("type").set("dmr");

            if(mapping.getAddress()!=null)
                mappingNode.get("address").set(mapping.getAddress());

            target.get("mapping").set(mappingNode);
        }
    }
}
