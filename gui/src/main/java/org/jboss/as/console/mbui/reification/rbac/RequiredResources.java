package org.jboss.as.console.mbui.reification.rbac;

import org.jboss.as.console.mbui.model.StereoTypes;
import org.jboss.as.console.mbui.model.mapping.DMRMapping;
import org.useware.kernel.model.mapping.MappingType;
import org.useware.kernel.model.structure.Container;
import org.useware.kernel.model.structure.InteractionUnit;
import org.useware.kernel.model.structure.builder.InteractionUnitVisitor;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Heiko Braun
 * @date 8/13/13
 */
public class RequiredResources implements InteractionUnitVisitor<StereoTypes>{

    public Set<String> requiredresources = new HashSet<String>();

    public Set<String> getRequiredresources() {
        return requiredresources;
    }

    @Override
    public void startVisit(Container container) {
        inspectUnit(container);
    }

    @Override
    public void visit(InteractionUnit<StereoTypes> interactionUnit) {
        inspectUnit(interactionUnit);
    }

    @Override
    public void endVisit(Container container) {

    }

    private void inspectUnit(InteractionUnit unit)
    {
        if(unit.hasMapping(MappingType.DMR))
        {
            DMRMapping mapping = (DMRMapping)unit.getMapping(MappingType.DMR);
            String address = mapping.getAddress();
            if(address !=null && !requiredresources.contains(address))
            {
                requiredresources.add(address);
            }
        }
    }
}
