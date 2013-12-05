package org.useware.kernel.model.structure;

import org.useware.kernel.gui.behaviour.common.CommonQNames;
import org.useware.kernel.model.behaviour.Resource;
import org.useware.kernel.model.behaviour.ResourceType;

/**
 * @author Heiko Braun
 * @date 1/16/13
 */
public class Link<S extends Enum<S>> extends InteractionUnit<S> {

    private static final Resource<ResourceType> NAVIGATION = new Resource<ResourceType>(CommonQNames.NAVIGATION_ID, ResourceType.Navigation);
    private QName target;

    public Link(String ns, String name, QName target, String label) {
        this(new QName(ns, name), target, label);
    }
    public Link(QName id, QName target, String label) {
        super(id, label);

        this.target = target;

        // explicit output
        setOutputs(NAVIGATION);
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
