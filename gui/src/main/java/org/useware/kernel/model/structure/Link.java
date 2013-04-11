package org.useware.kernel.model.structure;

import org.useware.kernel.gui.behaviour.NavigationEvent;
import org.useware.kernel.gui.behaviour.common.CommonQNames;
import org.useware.kernel.model.behaviour.Resource;
import org.useware.kernel.model.behaviour.ResourceType;

/**
 * @author Heiko Braun
 * @date 1/16/13
 */
public class Link<S extends Enum<S>> extends InteractionUnit<S> {

    private QName target;

    public Link(QName id, QName target, String label) {
        super(id, label);

        this.target = target;

        // explicit output
        setOutputs(new Resource<ResourceType>(CommonQNames.NAVIGATION_ID, ResourceType.Navigation));
    }

    public QName getTarget() {
        return target;
    }

    @Override
    public String toString()
    {
        return "Link {" + getId() + ", label="+ getLabel()+"}";
    }
}
