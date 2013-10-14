package org.jboss.as.console.mbui.marshall;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;
import org.jboss.as.console.mbui.marshall.adapters.DMRAdapter;
import org.jboss.as.console.mbui.marshall.adapters.EditorPanelAdapter;
import org.jboss.as.console.mbui.marshall.adapters.FormAdapter;
import org.jboss.as.console.mbui.marshall.adapters.PagesAdapter;
import org.jboss.as.console.mbui.marshall.adapters.PulldownAdapter;
import org.jboss.as.console.mbui.marshall.adapters.ToolstripAdapter;
import org.jboss.as.console.mbui.marshall.adapters.UsewareAdapter;
import org.jboss.as.console.mbui.model.StereoTypes;
import org.jboss.as.console.mbui.model.mapping.DMRMapping;
import org.jboss.as.console.mbui.model.mapping.ResourceAttribute;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.mapping.MappingType;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.QName;
import org.useware.kernel.model.structure.builder.Builder;
import org.useware.kernel.model.structure.builder.InteractionUnitVisitor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * @author Heiko Braun
 * @date 6/13/13
 */
public class DialogXML {

    private Set<String> stereoTypes;
    private static Set<ElementAdapter> adapters = new HashSet<ElementAdapter>();

    static {

        adapters.add(new UsewareAdapter("container"));
        adapters.add(new UsewareAdapter("trigger"));
        adapters.add(new UsewareAdapter("select"));
        adapters.add(new UsewareAdapter("input"));
        adapters.add(new UsewareAdapter("output"));
        adapters.add(new UsewareAdapter("link"));

        adapters.add(new FormAdapter());
        adapters.add(new ToolstripAdapter());
        adapters.add(new PagesAdapter());
        adapters.add(new PulldownAdapter());
        adapters.add(new EditorPanelAdapter());

        adapters.add(new DMRAdapter());
    }

    public DialogXML() {

        // known stereotypes
        this.stereoTypes = new HashSet<String>();
        for(StereoTypes type : StereoTypes.values())
        {
            stereoTypes.add(type.name().toLowerCase());
        }
    }


    public Dialog unmarshall(String xml)
    {
        Document document = XMLParser.parse(xml);
        Element root = document.getDocumentElement();

        // model
        Builder builder = new Builder();
        dfsElement(builder, getFirstChildElement(root));

        // dialog
        Dialog dialog = new Dialog(QName.valueOf(root.getAttribute("id")), builder.build());

        return dialog;
    }

    private static Node getFirstChildElement(Node parent) {
        NodeList children = parent.getChildNodes();

        for(int i=0; i<children.getLength(); i++)
        {
            Node child = children.item(i);
            if(child.getNodeType() == Node.ELEMENT_NODE)
                return child;
        }

        return null;
    }

    private InteractionUnit dfsElement(Builder builder, Node root) {

        // the node itself
        InteractionUnit parentUnit = (InteractionUnit)getAdapter(root.getNodeName()).fromXML(root);
        if( !(parentUnit instanceof Container))
            throw new IllegalArgumentException("Unexpected top unit: "+parentUnit);

        builder.start((Container)parentUnit);

        // it's children
        NodeList children = root.getChildNodes();

        for(int i=0; i<children.getLength(); i++)
        {

            Node child = children.item(i);

            // skip anything except elements
            if(! (Node.ELEMENT_NODE == child.getNodeType()))
                continue;

            ElementAdapter adapter = getAdapter(child.getNodeName());

            if(InteractionUnit.class == adapter.getType())
            {
                InteractionUnit unit = (InteractionUnit)adapter.fromXML(child);
                if(unit instanceof Container)
                {
                    // parse children
                    InteractionUnit container = dfsElement(builder, child);
                }
                else
                {
                    // parse siblings
                    builder.add((InteractionUnit)adapter.fromXML(child));
                }
            }
            else if (DMRMapping.class == adapter.getType())
            {
                // TODO
                DMRMapping mapping = (DMRMapping)adapter.fromXML(child);
                parentUnit.addMapping(mapping);
            }
        }

        builder.end();

        return parentUnit;
    }

    ElementAdapter getAdapter(String name) {
        ElementAdapter match = null;
        for(ElementAdapter adapter : adapters)
        {
            if(adapter.getElementName().equals(name))
            {
                match = adapter;
                break;
            }
        }

        if(null==match)
            throw new IllegalArgumentException("Invalid element name: "+ name);

        return match;
    }

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

            Element el = getAdapter(name).toXML(document, container);
            marshallMapping(container, el);

            stack.push(el);
        }

        @Override
        public void visit(InteractionUnit unit) {

            String name = unit.getStereotype()!=null ?
                    unit.getStereotype().toString().toLowerCase() : resolveType(unit);

            Element el = getAdapter(name).toXML(document, unit);
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
                dialogEl.setAttribute("id", dialog.getId().toString());
                dialogEl.appendChild(root);
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
                Element el = getAdapter("dmr").toXML(document, mapping);
                target.appendChild(el);
            }
        }
    }

    private static String resolveType(InteractionUnit interactionUnit) {
        String className = interactionUnit.getClass().getName().toLowerCase();
        return className.substring(className.lastIndexOf(".")+1, className.length());
    }


}
