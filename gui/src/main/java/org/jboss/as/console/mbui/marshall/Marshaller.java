package org.jboss.as.console.mbui.marshall;

import org.jboss.as.console.mbui.model.mapping.DMRMapping;
import org.jboss.dmr.client.ModelNode;
import org.useware.kernel.model.Dialog;
import org.useware.kernel.model.mapping.MappingType;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.builder.InteractionUnitVisitor;

import java.util.Date;
import java.util.Stack;

/**
 * @author Heiko Braun
 * @date 6/13/13
 */
public class Marshaller {

    public ModelNode marshall(Dialog dialog) {

        DMRMarshallRoutine marshaller = new DMRMarshallRoutine(dialog);
        dialog.getInterfaceModel().accept(marshaller);
        return marshaller.getResult();
    }

    class DMRMarshallRoutine implements InteractionUnitVisitor
    {

        private ModelNode result;
        private ModelNode currentNode = null;

        Stack<ModelNode> stack = new Stack<ModelNode>();

        DMRMarshallRoutine(Dialog dialog) {
            result = new ModelNode();
            result.get("dialog").set(dialog.getId().toString());
            result.get("rev").set(new Date().toString());
        }

        ModelNode getResult() {
            return result;
        }

        @Override
        public void startVisit(Container container) {
            ModelNode containerNode = new ModelNode();
            containerNode.get("type").set("container");

            if(container.getStereotype()!=null)
                containerNode.get("stereo-type").set(container.getStereotype().toString());


            if(container.getTemporalOperator()!=null) {
                containerNode.get("operator").set(container.getTemporalOperator().toString());
            }

            marshallMapping(container, containerNode);

            stack.push(containerNode);
        }

        @Override
        public void visit(InteractionUnit unit) {
            ModelNode item = new ModelNode();
            item.get("type").set(resolveType(unit));

            if(unit.getStereotype()!=null)
                item.get("stereo-type").set(unit.getStereotype().toString());

            marshallMapping(unit, item);

            if(!stack.isEmpty())
            {
                stack.peek().get("child:"+unit.getId().getLocalPart()).set(item);
            }
        }

        @Override
        public void endVisit(Container container) {
            if(stack.size()>1){
                ModelNode containerNode = stack.pop();
               stack.peek().get("unit:"+container.getId().getLocalPart()).set(containerNode);
            }
            else if(stack.size()==1)
            {
                ModelNode containerNode = stack.pop();
                result.get("structure").set(containerNode);
            }
        }
    }

    private String resolveType(InteractionUnit interactionUnit) {
        return interactionUnit.getClass().getName().toLowerCase();
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
